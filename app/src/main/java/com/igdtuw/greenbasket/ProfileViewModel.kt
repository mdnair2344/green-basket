//ProfileViewModel
package com.igdtuw.greenbasket

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import androidx.annotation.RequiresApi
import android.os.Build
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.igdtuw.greenbasket.NavControllerHolder.navController

// IMPORTANT RAZORPAY IMPORTS
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener // Needed if your Activity directly handles success/error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import kotlinx.coroutines.tasks.await
import com.igdtuw.greenbasket.ui.producer.uploadToCloudinary
import java.util.concurrent.TimeUnit
import kotlin.text.get


// --- NEW: Data classes for Payment Details (defined in com.igdtuw.greenbasket package) ---
data class UpiDetails(
    val upiId: String
)

data class RazorpayAccountInfo(
    val kycCompleted: Boolean = false,
    val razorpayKycStatus: String = "pending", // e.g., 'pending', 'verified', 'rejected'
    val razorpayAccountStatus: String = "created" // e.g., 'created', 'activated', 'under_review'
)

data class CardDetails(
    val cardNumber: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cvv: String,
    val cardHolderName: String = "", // Optional, but often useful
    val cardType: String = "", // e.g., "VISA", "MASTERCARD"
    val bankName: String = ""
)

data class PaymentBankDetails( // This is distinct from ProfileViewModel.BankDetails
    val accountNumber: String,
    val ifscCode: String,
    val bankName: String, // Bank name for this specific payment
    val accountHolderName: String
)
// --- END: New Data classes ---

sealed class PaymentStatusEvent {
    object PaymentSuccess : PaymentStatusEvent()
    object PaymentFailure : PaymentStatusEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    val db = Firebase.firestore
    private val BASE_URL_CONSTANT = "https://greenbasket-backend-1.onrender.com/"
    // Your backend IP address/URL. IMPORTANT: If testing on a physical device, this MUST be your computer's local IP (e.g., 192.168.x.x), NOT 10.0.2.2. If deployed, it's the public URL.
    private val RAZORPAY_KEY_ID_CONSTANT = "*********" // Your Razorpay Test Key ID

    // If you removed Cloudinary from BuildConfig as well, declare them here too:
    private val CLOUDINARY_CLOUD_NAME_CONSTANT = "*****"
    private val CLOUDINARY_API_KEY_CONSTANT = "***********"
    private val CLOUDINARY_API_SECRET_CONSTANT = "********" // VERY INSECURE if exposed in client-side code!

    // --- END HARDCODED VALUES ---
    private val _paymentStatusEvent = MutableSharedFlow<PaymentStatusEvent>()
    val paymentStatusEvent: SharedFlow<PaymentStatusEvent> = _paymentStatusEvent


    val acceptOnly5To10 = mutableStateOf(false)


    data class User(
        val name: String = "",
        val email: String = "",
        val phone: String = "",
        val address: String = "",
        val farmName: String = "",
        val imageUri: String = "",
        val certificateUri: String = "",
        val videoUri: String = "",
        val userType: String = "",
        val linkedAccountId: String = ""   // <-- Add this field
    )


    data class Card(
        val id: String = "",
        val cardNumber: String = "",
        val cardType: String = "",
        val bankName: String = "",
        val cvv: String = ""
    )

    // This BankDetails data class is for YOUR Firestore model (e.g., saved consumer bank accounts)
    // It is distinct from the top-level 'PaymentBankDetails' used for payment initiation.
    data class BankDetails(
        val accountNumber: String = "",
        val ifscCode: String = "",
        val upi: String = "",
        val bankName: String = "",
        val accountHolderName: String = ""
    )



    private val _userDetails = MutableStateFlow(User())
    val userDetails: StateFlow<User> = _userDetails.asStateFlow()

    private val _savedCards = MutableStateFlow<List<Card>>(emptyList())
    val savedCards: StateFlow<List<Card>> = _savedCards.asStateFlow()

    private val _savedBankDetails = MutableStateFlow<List<BankDetails>>(emptyList())
    val savedBankDetails: StateFlow<List<BankDetails>> = _savedBankDetails


    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    private val _savedProducerBankDetails = mutableStateOf<BankDetails?>(null)
    val savedProducerBankDetails: androidx.compose.runtime.State<BankDetails?> = _savedProducerBankDetails

    // State for Razorpay merchant creation API call status (from producer side)
    private val _razorpayMerchantCreationStatus = MutableStateFlow<ApiStatus>(ApiStatus.Idle)
    val razorpayMerchantCreationStatus: StateFlow<ApiStatus> = _razorpayMerchantCreationStatus.asStateFlow()

    // --- NEW: State for Consumer Payment Processing Status ---
    private val _paymentProcessStatus = MutableStateFlow<ApiStatus>(ApiStatus.Idle)
    val paymentProcessStatus: StateFlow<ApiStatus> = _paymentProcessStatus.asStateFlow()

    // NEW: Variable to hold the last initiated Razorpay Order ID for verification
    // This is crucial for matching the payment success/failure callback with the correct order
    private var currentRazorpayOrderId: String? = null

    private val _razorpayAccountInfo = MutableStateFlow(RazorpayAccountInfo())
    val razorpayAccountInfo: StateFlow<RazorpayAccountInfo> = _razorpayAccountInfo.asStateFlow()



    // Call this to reset the status after handling
    fun resetPaymentStatus() {
        _paymentProcessStatus.value = ApiStatus.Idle
    }




    sealed class ApiStatus {
        object Idle : ApiStatus()
        object Loading : ApiStatus()
        data class Success(val message: String) : ApiStatus()
        data class Error(val message: String) : ApiStatus()
    }


    init {
        fetchUser()
        fetchBankDetails()
        fetchCards()
        fetchProducerBankDetails() // Ensure this is fetched on init for producers
    }

    fun authenticateUser(password: String, context: Context) {
        val user = auth.currentUser ?: run {
            Toast.makeText(context, "No active user found.", Toast.LENGTH_SHORT).show()
            return
        }
        val email = user.email ?: run {
            Toast.makeText(context, "User email not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                _isAuthenticated.value = true
                Toast.makeText(context, "Authenticated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Authentication failed: ${e.message}", Toast.LENGTH_LONG).show()
                _isAuthenticated.value = false
            }
    }

    fun resetAuthenticationState() {
        _isAuthenticated.value = false
    }

    private fun fetchUser() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                _userDetails.value = User(
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    phone = doc.getString("phone") ?: "",
                    address = doc.getString("address") ?: "",
                    farmName = doc.getString("farmName") ?: "",
                    imageUri = doc.getString("imageUri") ?: "",
                    certificateUri = doc.getString("certificateUri") ?: "",
                    videoUri = doc.getString("videoUri") ?: "",
                    userType = doc.getString("userType") ?: "",
                    linkedAccountId = doc.getString("linkedAccountId") ?: ""
                )
                _razorpayAccountInfo.value = RazorpayAccountInfo(
                    kycCompleted = doc.getBoolean("kycCompleted") ?: false,
                    razorpayKycStatus = doc.getString("razorpayKycStatus") ?: "pending",
                    razorpayAccountStatus = doc.getString("razorpayAccountStatus") ?: "created"
                )
            }
            .addOnFailureListener {
                Log.e("ProfileViewModel", "Failed to fetch user", it)
            }
    }

    fun fetchCards() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("bank")
            .document("cards")
            .collection("all")
            .get()
            .addOnSuccessListener { result ->
                val cardList = result.documents.mapNotNull { doc ->
                    Card(
                        id = doc.id,
                        cardNumber = decrypt(doc.getString("cardNumber") ?: ""),
                        cardType = doc.getString("cardType") ?: "",
                        bankName = doc.getString("bankName") ?: "",
                        cvv = decrypt(doc.getString("cvv") ?: "")
                    )
                }
                _savedCards.value = cardList
            }
            .addOnFailureListener {
                Log.e("ProfileViewModel", "Failed to fetch cards", it)
            }
    }

    fun fetchBankDetails() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid)
            .collection("bankDetails")
            .get()
            .addOnSuccessListener { snapshot ->
                val bankList = snapshot.documents.mapNotNull { doc ->
                    try {
                        BankDetails(
                            accountNumber = decrypt(doc.getString("accountNumber") ?: return@mapNotNull null),
                            ifscCode = decrypt(doc.getString("ifsc") ?: return@mapNotNull null),
                            upi = decrypt(doc.getString("upi") ?: return@mapNotNull null),
                            bankName = decrypt(doc.getString("bankName") ?: ""), // newly added
                            accountHolderName = decrypt(doc.getString("accountHolderName") ?: "")
                        )
                    } catch (e: Exception) {
                        Log.e("ProfileViewModel", "Decryption failed for a bank entry", e)
                        null
                    }
                }
                _savedBankDetails.value = bankList
            }
            .addOnFailureListener {
                Log.e("ProfileViewModel", "Failed to fetch bank details", it)
            }
    }

    fun saveBankDetails(account: String, ifsc: String, upi: String, context: Context) {
        val uid = auth.currentUser?.uid ?: return

        val bankMap = mapOf(
            "accountNumber" to encrypt(account),
            "ifsc" to encrypt(ifsc),
            "upi" to encrypt(upi),
            "timestamp" to System.currentTimeMillis()  // optional: useful for sorting
        )

        firestore.collection("users")
            .document(uid)
            .collection("bankDetails")
            .add(bankMap) // <-- auto-generates a unique bank ID
            .addOnSuccessListener {
                Toast.makeText(context, "Bank Info Saved!", Toast.LENGTH_SHORT).show()
                fetchBankDetails()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save bank info: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ProfileViewModel", "Error saving bank details", e)
            }
    }


    fun saveCardDetails(
        id: String? = null,
        cardNumber: String,
        cardType: String,
        bankName: String,
        cvv: String,
        context: Context
    ) {
        val uid = auth.currentUser?.uid ?: return
        val cardId = id ?: firestore.collection("users")
            .document(uid)
            .collection("bank")
            .document("cards")
            .collection("all")
            .document().id

        val cardData = mapOf(
            "cardNumber" to encrypt(cardNumber),
            "cardType" to cardType,
            "bankName" to bankName,
            "cvv" to encrypt(cvv)
        )

        firestore.collection("users")
            .document(uid)
            .collection("bank")
            .document("cards")
            .collection("all")
            .document(cardId)
            .set(cardData)
            .addOnSuccessListener {
                Toast.makeText(context, "Card saved", Toast.LENGTH_SHORT).show()
                fetchCards()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to save card: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ProfileViewModel", "Error saving card", e)
            }
    }

    fun uploadProfileImage(uri: Uri, context: Context) {
        val uid = auth.currentUser?.uid ?: return

        val folderPath = "Medias/$uid/Profile"

        uploadToCloudinary(
            context = context,
            fileUri = uri,
            folder = folderPath,
            onSuccess = { imageUrl, publicId ->
                firestore.collection("users").document(uid)
                    .update("imageUri", imageUrl)
                    .addOnSuccessListener {
                        viewModelScope.launch {
                            Toast.makeText(context, "Profile image updated", Toast.LENGTH_SHORT).show()
                            fetchUser()
                        }
                    }
                    .addOnFailureListener { e ->
                        viewModelScope.launch {
                            Toast.makeText(context, "Failed to save image URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            },
            onFailure = { error ->
                viewModelScope.launch {
                    Toast.makeText(context, "Upload failed: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }


    fun removeProfileImage(context: Context) {
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }
        val userDoc = firestore.collection("users").document(userId)

        viewModelScope.launch {
            try {
                val currentImageUri = _userDetails.value.imageUri
                if (currentImageUri.isNullOrBlank()) {
                    Toast.makeText(context, "No profile picture to remove", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Prefer reading a stored publicId field if you saved it during upload
                val publicId = extractPublicIdFromUrl(currentImageUri)

                // 1) Delete from Cloudinary (IO thread)
                if (publicId != null) {
                    val deleted = deleteImageFromCloudinary(publicId) // suspend
                    if (!deleted) {
                        // You can still continue to clear Firestore if you want even if CDN delete failed
                        Log.w("Cloudinary", "Deletion API returned failure for publicId=$publicId")
                    }
                } else {
                    Log.w("Cloudinary", "publicId could not be derived from URL — skipping Cloudinary delete")
                }

                // 2) Clear Firestore (await)
                userDoc.update("imageUri", "").await()

                // 3) Update local state so UI refreshes
                _userDetails.update { it.copy(imageUri = "") }

                Toast.makeText(context, "Profile picture removed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Exception while removing image", e)
                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun extractPublicIdFromUrl(url: String): String? {
        return try {
            val start = url.indexOf("/upload/") + 8
            val end = url.lastIndexOf('.')
            if (start != -1 && end != -1 && end > start) {
                url.substring(start, end)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun deleteImageFromCloudinary(publicId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cloudName = CLOUDINARY_CLOUD_NAME_CONSTANT
            val apiKey = CLOUDINARY_API_KEY_CONSTANT
            val apiSecret = CLOUDINARY_API_SECRET_CONSTANT

            if (cloudName.isBlank() || apiKey.isBlank() || apiSecret.isBlank() ||
                cloudName == "your_cloudinary_cloud_name_HERE") {
                Log.e("Cloudinary", "Cloudinary credentials are not set.")
                return@withContext false
            }

            val timestamp = (System.currentTimeMillis() / 1000).toString()

            // signature = sha1("public_id=<id>&timestamp=<ts><api_secret>")
            val sigBase = "public_id=$publicId&timestamp=$timestamp$apiSecret"
            val signature = MessageDigest.getInstance("SHA-1")
                .digest(sigBase.toByteArray())
                .joinToString("") { "%02x".format(it) }

            val requestBody = FormBody.Builder()
                .add("public_id", publicId)
                .add("timestamp", timestamp)
                .add("api_key", apiKey)
                .add("signature", signature)
                .add("invalidate", "true") // optional but useful
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/destroy")
                .post(requestBody)
                .build()

            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e("Cloudinary", "Failed: code=${resp.code}, message=${resp.message}, body=${resp.body?.string()}")
                    return@withContext false
                }
            }

            Log.d("Cloudinary", "Deleted image: $publicId")
            true
        } catch (e: Exception) {
            Log.e("Cloudinary", "Exception in image deletion", e)
            false
        }
    }


    fun updateBasicInfo(name: String, phone: String, address: String, context: Context) {
        val uid = auth.currentUser?.uid ?: return
        val updatedMap = mutableMapOf<String, Any>()
        if (name.isNotBlank()) updatedMap["name"] = name
        if (phone.isNotBlank()) updatedMap["phone"] = phone
        if (address.isNotBlank()) updatedMap["address"] = address

        if (updatedMap.isEmpty()) {
            Toast.makeText(context, "No changes to save.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(uid)
            .update(updatedMap)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                fetchUser()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("ProfileViewModel", "Error updating profile", e)
            }
    }

    // THIS IS THE MODIFIED FUNCTION TO ALSO CALL RAZORPAY MERCHANT CREATION
//    fun saveProducerBankDetails(account: String, ifsc: String, /*upi: String*/ context: Context) {
//        val uid = auth.currentUser?.uid ?: return
//
//        val encryptedMap = mapOf(
//            "accountNumber" to encrypt(account),
//            "ifsc" to encrypt(ifsc)
//            //"upi" to encrypt(upi)
//        )
//
//        firestore.collection("users").document(uid)
//            .collection("bankDetails").document("primary")
//            .set(encryptedMap)
//            .addOnSuccessListener {
//                Toast.makeText(context, "Producer bank info saved!", Toast.LENGTH_SHORT).show()
//                fetchProducerBankDetails() // Update local state for saved bank details
//
//                // --- NEW: Trigger Razorpay merchant account creation here ---
//                // You need the producer's name, email, and contact for Razorpay.
//                // Assuming userDetails.value is up-to-date and holds this info.
//                val producerName = userDetails.value.name
//                val producerEmail = userDetails.value.email
//                val producerPhone = userDetails.value.phone // This is 'contact' for Razorpay
//
//                if (producerName.isNotBlank() && producerEmail.isNotBlank() && producerPhone.isNotBlank()) {
//                    createRazorpayMerchantAccount(
//                        name = producerName,
//                        email = producerEmail,
//                        contact = producerPhone,
//                        accountNumber = account, // Pass unencrypted data to backend for Razorpay
//                        ifsc = ifsc,            // Pass unencrypted data to backend for Razorpay
//                        context = context
//                    )
//                } else {
//                    Toast.makeText(context, "Producer name, email, or phone missing. Cannot create Razorpay account.", Toast.LENGTH_LONG).show()
//                    Log.e("ProfileViewModel", "Producer details missing for Razorpay account creation.")
//                }
//                // --- END NEW ---
//            }
//            .addOnFailureListener { e ->
//                Log.e("ProfileViewModel", "Error saving producer bank details to Firestore", e)
//                Toast.makeText(context, "Failed to save bank info: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }

    fun fetchProducerBankDetails() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid)
            .collection("bankDetails").document("primary")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    _savedProducerBankDetails.value = BankDetails(
                        accountNumber = decrypt(doc.getString("accountNumber") ?: ""),
                        ifscCode = decrypt(doc.getString("ifsc") ?: ""),
                        upi = decrypt(doc.getString("upi") ?: ""),
                        bankName = decrypt(doc.getString("bankName") ?: ""), // newly added
                        accountHolderName = decrypt(doc.getString("accountHolderName") ?: "") // newly added
                    )
                }
                else {
                    _savedProducerBankDetails.value = null
                }
            }
            .addOnFailureListener {
                Log.e("ProfileViewModel", "Error fetching producer bank details", it)
            }
    }

    // --- Function for Razorpay Merchant Account Creation (Producer Side) ---


    /*fun markRazorpayOnboardingComplete(
        producerId: String,
        merchantId: String?,
        context: Context
    ) {
        val data = mutableMapOf<String, Any>(
            "razorpayOnboarded" to true,
            "timestamp" to FieldValue.serverTimestamp()
        )
        if (!merchantId.isNullOrBlank()) {
            data["razorpayMerchantId"] = merchantId
        }

        Firebase.firestore
            .collection("producers")
            .document(producerId)
            .collection("razorpay")
            .document("onboarding")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "✅ Razorpay onboarding saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "❌ Failed to save onboarding", Toast.LENGTH_SHORT).show()
            }
    }



    fun createRazorpayMerchantAccount(context: Context) {
        viewModelScope.launch {
            _razorpayMerchantCreationStatus.value = ApiStatus.Loading

            val producerId = auth.currentUser?.uid
            if (producerId == null) {
                _razorpayMerchantCreationStatus.value = ApiStatus.Error("User not authenticated.")
                Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                // Fetch current user details from _userDetails to ensure latest name, email, contact
                val currentUserDetails = _userDetails.value // Use the existing StateFlow value

                val name = currentUserDetails.name
                val email = currentUserDetails.email
                val contact = currentUserDetails.phone

                if (name.isBlank() || email.isBlank() || contact.isBlank()) {
                    _razorpayMerchantCreationStatus.value = ApiStatus.Error("Name, Email, or Mobile number is missing in your profile.")
                    Toast.makeText(context, "Please complete your profile with name, email, and mobile number.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val backendUrl = "${BASE_URL_CONSTANT}create-linked-account"

                val jsonBody = JSONObject().apply {
                    put("name", name)
                    put("email", email)
                    put("contact", contact)
                    put("producerId", producerId)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = RequestBody.create(mediaType, jsonBody.toString())

                val request = Request.Builder()
                    .url(backendUrl)
                    .post(requestBody)
                    .build()

                val client = OkHttpClient()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val accountId = jsonResponse?.optString("accountId")

                    if (accountId != null) {
                        _razorpayMerchantCreationStatus.value = ApiStatus.Success("Merchant account created! ID: $accountId")
                        Toast.makeText(context, "Merchant Account Created!", Toast.LENGTH_LONG).show()
                        // Important: Refresh user details to get the newly stored linkedAccountId
                        fetchUser() // This will also update _razorpayAccountInfo

                    } else {
                        val errorMsg = jsonResponse?.optString("error") ?: "Failed to get account ID from response."
                        _razorpayMerchantCreationStatus.value = ApiStatus.Error(errorMsg)
                        Toast.makeText(context, "Failed to create merchant: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBody = response.body?.string()
                    val errorMessage = JSONObject(errorBody).optString("error", "Unknown error")
                    _razorpayMerchantCreationStatus.value = ApiStatus.Error("Failed to create merchant: $errorMessage")
                    Toast.makeText(context, "Failed to create merchant: $errorMessage", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                _razorpayMerchantCreationStatus.value = ApiStatus.Error("Error: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- NEW FUNCTION: Generate and Open Razorpay Onboarding Link (Producer Side) ---
    fun generateAndOpenRazorpayOnboardingLink(linkedAccountId: String, context: Context) {
        viewModelScope.launch {
            if (linkedAccountId.isBlank()) {
                Toast.makeText(context, "Razorpay Merchant Account not yet created.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Optional: Set a loading status if you want a specific indicator for this operation
            // _razorpayMerchantCreationStatus.value = ApiStatus.Loading

            try {
                val backendUrl = "${BASE_URL_CONSTANT}generate-razorpay-onboarding-link" // Use the NEW ENDPOINT

                val jsonBody = JSONObject().apply {
                    put("linkedAccountId", linkedAccountId)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = RequestBody.create(mediaType, jsonBody.toString())

                val request = Request.Builder()
                    .url(backendUrl)
                    .post(requestBody)
                    .build()

                val client = OkHttpClient()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val onboardingLink = jsonResponse?.optString("onboardingLink")

                    if (!onboardingLink.isNullOrBlank()) {
                        // Open the link in a browser (or Chrome Custom Tabs for better UX)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(onboardingLink))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required if called from non-Activity context
                        context.startActivity(intent)
                        Toast.makeText(context, "Opening Razorpay KYC portal in browser...", Toast.LENGTH_LONG).show()
                        // Optional: You might want to update a UI state here to indicate redirection
                        // _razorpayMerchantCreationStatus.value = ApiStatus.Idle // Reset status
                    } else {
                        val errorMsg = jsonResponse?.optString("error") ?: "Failed to get onboarding link from response."
                        Toast.makeText(context, "Error getting KYC link: $errorMsg", Toast.LENGTH_LONG).show()
                        Log.e("ProfileViewModel", "Failed to get onboarding link: $errorMsg")
                        // _razorpayMerchantCreationStatus.value = ApiStatus.Error(errorMsg) // Set error status
                    }
                } else {
                    val errorBody = response.body?.string()
                    val errorMessage = JSONObject(errorBody).optString("error", "Unknown error")
                    Toast.makeText(context, "Failed to generate KYC link: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("ProfileViewModel", "Backend error generating link: $errorBody")
                    // _razorpayMerchantCreationStatus.value = ApiStatus.Error(errorMessage) // Set error status
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Network Error or JSON parsing issue: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ProfileViewModel", "Exception generating link: ${e.message}", e)
                // _razorpayMerchantCreationStatus.value = ApiStatus.Error(e.message ?: "Unknown error") // Set error status
            } finally {
                // Optional: Reset loading state or other status
            }
        }
    }

    /**
     * Initiates a payment process by creating a Razorpay Order on the backend,
     * which includes transfers to the producer's linked account.
     *
     * @param producerMerchantAccountId The Razorpay linked account ID of the producer.
     * @param orderTotal The total amount of the order (e.g., 100.0 for 100 INR).
     * @param paymentMethod A string indicating the chosen payment method ("UPI", "Card", "Bank Transfer").
     * @param consumerUpiDetails (Optional) The UPI ID entered by the consumer, if paying via UPI.
     * @param consumerCardDetails (Optional) The card details entered by the consumer, if paying via Card.
     * @param consumerBankDetails (Optional) The bank details entered by the consumer, if paying via Bank Transfer.
     * @param context Android context for Toast messages and launching Razorpay Checkout.
     */

    fun initiateOrderAndPayment(
        producerMerchantAccountId: String,
        orderTotal: Double,
        paymentMethod: String,
        consumerUpiDetails: UpiDetails?,
        consumerCardDetails: CardDetails?,
        consumerBankDetails: PaymentBankDetails?,
        consumerId: String,           // <-- Add this
        localOrderId: String,         // <-- Add this
        context: Context
    ){
        viewModelScope.launch {
            _paymentProcessStatus.value = ApiStatus.Loading

            val consumerId = auth.currentUser?.uid
            if (consumerId.isNullOrBlank()) {
                _paymentProcessStatus.value = ApiStatus.Error("User not authenticated for payment.")
                Toast.makeText(context, "Please log in to make a payment.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (orderTotal <= 0 || orderTotal.isNaN()) {
                _paymentProcessStatus.value = ApiStatus.Error("Invalid order amount.")
                Toast.makeText(context, "Invalid payment amount.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val backendUrl = "${BASE_URL_CONSTANT}initiate-payment" // Your backend endpoint for creating Razorpay Orders

            if (backendUrl.isBlank() || !backendUrl.startsWith("http")) {
                _paymentProcessStatus.value = ApiStatus.Error("Invalid backend URL.")
                Log.e("PaymentViewModel", "Invalid backend URL: $backendUrl")
                return@launch
            }

            if (paymentMethod == "UPI" && (consumerUpiDetails?.upiId.isNullOrBlank() || !consumerUpiDetails!!.upiId.contains("@"))) {
                _paymentProcessStatus.value = ApiStatus.Error("Invalid UPI ID.")
                Toast.makeText(context, "Please enter a valid UPI ID.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (paymentMethod == "Card" && (consumerCardDetails?.cardNumber?.length !in 12..19)) {
                _paymentProcessStatus.value = ApiStatus.Error("Invalid card number.")
                Toast.makeText(context, "Card number must be between 12 and 19 digits.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val localOrderId = "${System.currentTimeMillis()}_${consumerId.take(5)}"
            //val backendUrl = "${BASE_URL_CONSTANT}initiate-payment"

            val jsonBody = try {
                JSONObject().apply {
                    put("amount", orderTotal)
                    put("currency", "INR")
                    put("producerAccountId", producerMerchantAccountId)
                    put("consumerId", consumerId)
                    put("paymentMethod", paymentMethod)
                    put("localOrderId", localOrderId)

                    when (paymentMethod) {
                        "UPI" -> {
                            consumerUpiDetails?.let {
                                put("upiId", it.upiId)
                            }
                        }

                        "Card" -> {
                            consumerCardDetails?.let {
                                put("cardNumber", it.cardNumber)
                                put("cardType", it.cardType)
                                put("cardBankName", it.bankName)
                            }
                        }

                        "Bank Transfer" -> {
                            consumerBankDetails?.let {
                                put("consumerAccountNumber", it.accountNumber)
                                put("consumerIfscCode", it.ifscCode)
                                put("consumerBankName", it.bankName)
                                put("consumerAccountHolderName", it.accountHolderName)
                            }
                        }
                    }
                }
            } catch (e: JSONException) {
                _paymentProcessStatus.value = ApiStatus.Error("Error creating payment request JSON.")
                Log.e("PaymentViewModel", "JSONException while building body", e)
                return@launch
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = RequestBody.create(mediaType, jsonBody.toString())
            val request = Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient()

            try {
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val razorpayOrderId = jsonResponse.optString("orderId", null)
                        val razorpayAmount = jsonResponse.optDouble("amount", -1.0)

                        if (!razorpayOrderId.isNullOrBlank() && razorpayAmount > 0) {
                            _paymentProcessStatus.value = ApiStatus.Success("Order created. Launching payment gateway.")
                            currentRazorpayOrderId = razorpayOrderId
                            startRazorpayCheckout(razorpayOrderId, razorpayAmount, context)
                        } else {
                            _paymentProcessStatus.value = ApiStatus.Error("Invalid response from server.")
                            Log.e("PaymentViewModel", "Missing orderId/amount in response: $responseBody")
                            Toast.makeText(context, "Payment initialization failed: Invalid server response", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: JSONException) {
                        _paymentProcessStatus.value = ApiStatus.Error("Malformed JSON in server response.")
                        Log.e("PaymentViewModel", "Error parsing server JSON: $responseBody", e)
                    }
                } else {
                    val errorMessage = try {
                        JSONObject(responseBody ?: "").optString("error", "Unknown backend error")
                    } catch (e: JSONException) {
                        "Unknown error from backend."
                    }
                    _paymentProcessStatus.value = ApiStatus.Error("Backend order creation failed: $errorMessage")
                    Toast.makeText(context, "Payment initiation failed: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("PaymentViewModel", "Backend error response: ${response.code} - $responseBody")
                }
            } catch (e: Exception) {
                _paymentProcessStatus.value = ApiStatus.Error("Network/API error: ${e.message}")
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("PaymentViewModel", "Exception during payment initiation", e)
            }
        }
    }


    private fun encrypt(data: String): String {
        return Base64.encodeToString(data.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
    }

    private fun decrypt(data: String): String {
        return try {
            String(Base64.decode(data, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            ""
        }
    }

    // THIS FUNCTION IS CALLED INTERNALLY BY initiateOrderAndPayment


    private fun startRazorpayCheckout(razorpayOrderId: String, amount: Double, context: Context) {
        try {
            val activity = context as? Activity ?: run {
                Toast.makeText(context, "Error: Context is not an Activity. Cannot launch Razorpay.", Toast.LENGTH_SHORT).show()
                _paymentProcessStatus.value = ApiStatus.Error("Internal error: Activity context not found for Razorpay.")
                return
            }

            val co = Checkout()
            // IMPORTANT: Use your actual PUBLIC Key ID (rzp_test_xxxxxxx or rzp_live_xxxxxxx)
            // This key is now hardcoded
            co.setKeyID(RAZORPAY_KEY_ID_CONSTANT) // <--- Now uses the hardcoded constant

            val options = JSONObject()
            options.put("name", "GreenBasket") // Your app name
            options.put("description", "Order Payment for Producer")
            options.put("currency", "INR")
            options.put("amount", (amount * 100).toLong()) // Amount in paisa
            options.put("order_id", razorpayOrderId) // The order ID from your backend

            val prefill = JSONObject()
            prefill.put("email", userDetails.value.email.ifBlank { "" })
            prefill.put("contact", userDetails.value.phone.ifBlank { "" })
            options.put("prefill", prefill)

            // You can add theme colors, image, etc. if needed
            // options.put("theme.color", "#3399CC");

            co.open(activity, options)

        } catch (e: Exception) {
            _paymentProcessStatus.value = ApiStatus.Error("Error launching Razorpay: ${e.message}")
            Toast.makeText(context, "Payment gateway failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("PaymentViewModel", "Error in startRazorpayCheckout", e)
        }
    }
    private fun recordTransaction(
        paymentId: String,
        orderId: String,
        consumerId: String,
        amount: Double,
        paymentMethod: String,
        context: Context
    ) {
        val transactionData = mapOf(
            "paymentId" to paymentId,
            "orderId" to orderId,
            "consumerId" to consumerId,
            "amount" to amount,
            "paymentMethod" to paymentMethod,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("transactions")
            .add(transactionData)
            .addOnSuccessListener {
                Toast.makeText(context, "Transaction recorded successfully.", Toast.LENGTH_SHORT).show()
                Log.d("ProfileViewModel", "Transaction recorded in Firestore.")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to record transaction: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ProfileViewModel", "Error recording transaction", e)
            }
    }


    /**
     * Handles the successful payment callback from Razorpay.
     * This method should be called from your hosting Activity's onPaymentSuccess.
     * It then calls your backend to verify the payment.
     * @param paymentId The Razorpay payment ID.
     */
    // THIS FUNCTION IS CALLED BY YOUR ACTIVITY (FROM onPaymentSuccess)
    fun handlePaymentSuccess(paymentId: String, context: Context) {
        viewModelScope.launch {
            _paymentProcessStatus.value = ApiStatus.Loading
            Toast.makeText(context, "Payment successful! Verifying...", Toast.LENGTH_SHORT).show()

            val consumerId = auth.currentUser?.uid
            if (consumerId == null) {
                _paymentProcessStatus.value = ApiStatus.Error("User not authenticated for verification.")
                return@launch
            }

            if (currentRazorpayOrderId == null) {
                _paymentProcessStatus.value = ApiStatus.Error("Internal error: No active order ID to verify.")
                Toast.makeText(context, "Payment success, but order ID missing for verification.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val backendUrl = "${BASE_URL_CONSTANT}verify-payment"
            val jsonBody = JSONObject().apply {
                put("orderId", currentRazorpayOrderId)
                put("paymentId", paymentId)
                put("consumerId", consumerId)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = RequestBody.create(mediaType, jsonBody.toString())

            val request = Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient()

            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val verificationStatus = jsonResponse?.optString("status")
                    val message = jsonResponse?.optString("message") ?: "Payment verified successfully!"

                    if (verificationStatus == "success") {
                        _paymentProcessStatus.value = ApiStatus.Success(message)
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                        val verifiedAmount = jsonResponse?.optDouble("amount") ?: 0.0
                        val producerId = jsonResponse?.optString("producerId") ?: ""
                        val localOrderId = jsonResponse?.optString("localOrderId") ?: ""

                        currentRazorpayOrderId = null

                        recordTransaction(
                            paymentId = paymentId,
                            orderId = localOrderId,
                            consumerId = consumerId,
                            amount = verifiedAmount,
                            paymentMethod = "Razorpay",
                            context = context
                        )

                        // ✅ Transfer to Producer after verification
                        if (producerId.isNotEmpty() && verifiedAmount > 0) {
                            transferToProducer(
                                producerId = producerId,
                                amount = verifiedAmount,
                                transactionId = paymentId,
                                context = context,
                                onSuccess = {
                                    Log.d("PaymentViewModel", "Transfer to producer successful")
                                    navController?.navigate("OrderStatusScreen")
                                },
                                onFailure = { errorMsg ->
                                    Log.e("PaymentViewModel", "Transfer to producer failed: $errorMsg")
                                    navController?.navigate("OrderStatusScreen")
                                }
                            )
                        } else {
                            Log.e("PaymentViewModel", "Missing producerId or amount for transfer")
                        }

                        viewModelScope.launch {
                            _paymentStatusEvent.emit(PaymentStatusEvent.PaymentSuccess)
                        }
                    } else {
                        _paymentProcessStatus.value = ApiStatus.Error("Payment verification failed: $message")
                        Toast.makeText(context, "Verification failed: $message", Toast.LENGTH_LONG).show()
                        Log.e("PaymentViewModel", "Payment verification failed: $responseBody")
                    }
                } else {
                    val errorBody = response.body?.string()
                    val errorJson = errorBody?.let { JSONObject(it) }
                    val errorMessage = errorJson?.optString("error") ?: "Unknown error during verification."
                    _paymentProcessStatus.value = ApiStatus.Error("Backend verification failed: $errorMessage")
                    Toast.makeText(context, "Verification failed: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("PaymentViewModel", "Backend verification failed with status ${response.code}: $errorBody")
                }
            } catch (e: Exception) {
                _paymentProcessStatus.value = ApiStatus.Error("Network/API error during verification: ${e.message}")
                Toast.makeText(context, "Network error during verification: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("PaymentViewModel", "Exception during payment verification", e)
            }
        }
    }

    /**
     * Handles the failed payment callback from Razorpay.
     * This method should be called from your hosting Activity's onPaymentError.
     * @param code The error code.
     * @param description The error description.
     */
    // THIS FUNCTION IS CALLED BY YOUR ACTIVITY (FROM onPaymentError)
    fun handlePaymentError(code: Int, description: String, context: Context) {
        val errorMessage = "Payment failed. Code: $code, Desc: $description"
        _paymentProcessStatus.value = ApiStatus.Error(errorMessage)
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        Log.e("PaymentViewModel", "Razorpay Payment Error: $errorMessage")
        // Optionally, you can also notify your backend about the failed payment here
        // (e.g., if you need to update an order status to 'failed')
        currentRazorpayOrderId = null // Clear the stored order ID
        viewModelScope.launch {
            _paymentStatusEvent.emit(PaymentStatusEvent.PaymentFailure)
        }
        navController?.navigate("PaymentScreen")
    }
    fun transferToProducer(
        producerId: String,
        amount: Double,
        transactionId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _paymentProcessStatus.value = ApiStatus.Loading

            val backendUrl = "${BASE_URL_CONSTANT}transfer"

            val jsonBody = JSONObject().apply {
                put("producerId", producerId)
                put("amount", amount)
                put("transactionId", transactionId)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = RequestBody.create(mediaType, jsonBody.toString())

            val request = Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient()

            try {
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.optBoolean("success", false)) {
                        _paymentProcessStatus.value = ApiStatus.Success("Transfer successful!")
                        onSuccess()
                    } else {
                        val errorMsg = "Transfer failed on server."
                        _paymentProcessStatus.value = ApiStatus.Error(errorMsg)
                        onFailure(errorMsg)
                    }
                } else {
                    val errorMsg = "Transfer failed: $responseBody"
                    _paymentProcessStatus.value = ApiStatus.Error(errorMsg)
                    onFailure(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Transfer network error: ${e.message}"
                _paymentProcessStatus.value = ApiStatus.Error(errorMsg)
                onFailure(errorMsg)
            }
        }
    }*/


    // --- Function for Razorpay Merchant Account Creation (Producer Side) ---



    // --- Function for Razorpay Merchant Account Creation (Producer Side) ---


    fun markRazorpayOnboardingComplete(
        producerId: String,
        merchantId: String?,
        context: Context
    ) {
        val data = mutableMapOf<String, Any>(
            "razorpayOnboarded" to true,
            "timestamp" to FieldValue.serverTimestamp()
        )
        if (!merchantId.isNullOrBlank()) {
            data["razorpayMerchantId"] = merchantId
        }

        Firebase.firestore
            .collection("producers")
            .document(producerId)
            .collection("razorpay")
            .document("onboarding")
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "✅ Razorpay onboarding saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "❌ Failed to save onboarding", Toast.LENGTH_SHORT).show()
            }
    }



    fun createRazorpayMerchantAccount(context: Context) {
        viewModelScope.launch {
            _razorpayMerchantCreationStatus.value = ApiStatus.Loading

            val producerId = auth.currentUser?.uid
            if (producerId == null) {
                _razorpayMerchantCreationStatus.value = ApiStatus.Error("User not authenticated.")
                Toast.makeText(context, "User not authenticated.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                // Fetch current user details from _userDetails to ensure latest name, email, contact
                val currentUserDetails = _userDetails.value // Use the existing StateFlow value

                val name = currentUserDetails.name
                val email = currentUserDetails.email
                val contact = currentUserDetails.phone

                if (name.isBlank() || email.isBlank() || contact.isBlank()) {
                    _razorpayMerchantCreationStatus.value = ApiStatus.Error("Name, Email, or Mobile number is missing in your profile.")
                    Toast.makeText(context, "Please complete your profile with name, email, and mobile number.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val backendUrl = "${BASE_URL_CONSTANT}create-linked-account"

                val jsonBody = JSONObject().apply {
                    put("name", name)
                    put("email", email)
                    put("contact", contact)
                    put("producerId", producerId)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = RequestBody.create(mediaType, jsonBody.toString())

                val request = Request.Builder()
                    .url(backendUrl)
                    .post(requestBody)
                    .build()

                val client = OkHttpClient()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val accountId = jsonResponse?.optString("accountId")

                    if (accountId != null) {
                        _razorpayMerchantCreationStatus.value = ApiStatus.Success("Merchant account created! ID: $accountId")
                        Toast.makeText(context, "Merchant Account Created!", Toast.LENGTH_LONG).show()
                        // Important: Refresh user details to get the newly stored linkedAccountId
                        fetchUser() // This will also update _razorpayAccountInfo

                    } else {
                        val errorMsg = jsonResponse?.optString("error") ?: "Failed to get account ID from response."
                        _razorpayMerchantCreationStatus.value = ApiStatus.Error(errorMsg)
                        Toast.makeText(context, "Failed to create merchant: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBody = response.body?.string()
                    val errorMessage = JSONObject(errorBody).optString("error", "Unknown error")
                    _razorpayMerchantCreationStatus.value = ApiStatus.Error("Failed to create merchant: $errorMessage")
                    Toast.makeText(context, "Failed to create merchant: $errorMessage", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                _razorpayMerchantCreationStatus.value = ApiStatus.Error("Error: ${e.message}")
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- NEW FUNCTION: Generate and Open Razorpay Onboarding Link (Producer Side) ---
    fun generateAndOpenRazorpayOnboardingLink(linkedAccountId: String, context: Context) {
        viewModelScope.launch {
            if (linkedAccountId.isBlank()) {
                Toast.makeText(context, "Razorpay Merchant Account not yet created.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Optional: Set a loading status if you want a specific indicator for this operation
            // _razorpayMerchantCreationStatus.value = ApiStatus.Loading

            try {
                val backendUrl = "${BASE_URL_CONSTANT}generate-razorpay-onboarding-link" // Use the NEW ENDPOINT

                val jsonBody = JSONObject().apply {
                    put("linkedAccountId", linkedAccountId)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val requestBody = RequestBody.create(mediaType, jsonBody.toString())

                val request = Request.Builder()
                    .url(backendUrl)
                    .post(requestBody)
                    .build()

                val client = OkHttpClient()

                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val onboardingLink = jsonResponse?.optString("onboardingLink")

                    if (!onboardingLink.isNullOrBlank()) {
                        // Open the link in a browser (or Chrome Custom Tabs for better UX)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(onboardingLink))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required if called from non-Activity context
                        context.startActivity(intent)
                        Toast.makeText(context, "Opening Razorpay KYC portal in browser...", Toast.LENGTH_LONG).show()
                        // Optional: You might want to update a UI state here to indicate redirection
                        // _razorpayMerchantCreationStatus.value = ApiStatus.Idle // Reset status
                    } else {
                        val errorMsg = jsonResponse?.optString("error") ?: "Failed to get onboarding link from response."
                        Toast.makeText(context, "Error getting KYC link: $errorMsg", Toast.LENGTH_LONG).show()
                        Log.e("ProfileViewModel", "Failed to get onboarding link: $errorMsg")
                        // _razorpayMerchantCreationStatus.value = ApiStatus.Error(errorMsg) // Set error status
                    }
                } else {
                    val errorBody = response.body?.string()
                    val errorMessage = JSONObject(errorBody).optString("error", "Unknown error")
                    Toast.makeText(context, "Failed to generate KYC link: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("ProfileViewModel", "Backend error generating link: $errorBody")
                    // _razorpayMerchantCreationStatus.value = ApiStatus.Error(errorMessage) // Set error status
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Network Error or JSON parsing issue: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ProfileViewModel", "Exception generating link: ${e.message}", e)
                // _razorpayMerchantCreationStatus.value = ApiStatus.Error(e.message ?: "Unknown error") // Set error status
            } finally {
                // Optional: Reset loading state or other status
            }
        }
    }

    /**
     * Initiates a payment process by creating a Razorpay Order on the backend,
     * which includes transfers to the producer's linked account.
     *
     * @param producerMerchantAccountId The Razorpay linked account ID of the producer.
     * @param orderTotal The total amount of the order (e.g., 100.0 for 100 INR).
     * @param paymentMethod A string indicating the chosen payment method ("UPI", "Card", "Bank Transfer").
     * @param consumerUpiDetails (Optional) The UPI ID entered by the consumer, if paying via UPI.
     * @param consumerCardDetails (Optional) The card details entered by the consumer, if paying via Card.
     * @param consumerBankDetails (Optional) The bank details entered by the consumer, if paying via Bank Transfer.
     * @param context Android context for Toast messages and launching Razorpay Checkout.
     */

    fun initiateOrderAndPayment(
        producerMerchantAccountId: String,
        orderTotal: Double,
        paymentMethod: String,
        consumerUpiDetails: UpiDetails?,
        consumerCardDetails: CardDetails?,
        consumerBankDetails: PaymentBankDetails?,
        consumerId: String,           // <-- Add this
        localOrderId: String,         // <-- Add this
        context: Context
    ){
        viewModelScope.launch {
            _paymentProcessStatus.value = ApiStatus.Loading

            val consumerId = auth.currentUser?.uid
            if (consumerId.isNullOrBlank()) {
                _paymentProcessStatus.value = ApiStatus.Error("User not authenticated for payment.")
                Toast.makeText(context, "Please log in to make a payment.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (orderTotal <= 0 || orderTotal.isNaN()) {
                _paymentProcessStatus.value = ApiStatus.Error("Invalid order amount.")
                Toast.makeText(context, "Invalid payment amount.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val backendUrl = "${BASE_URL_CONSTANT}initiate-payment" // Your backend endpoint for creating Razorpay Orders

            if (backendUrl.isBlank() || !backendUrl.startsWith("http")) {
                _paymentProcessStatus.value = ApiStatus.Error("Invalid backend URL.")
                Log.e("PaymentViewModel", "Invalid backend URL: $backendUrl")
                return@launch
            }

            if (paymentMethod == "UPI" && (consumerUpiDetails?.upiId.isNullOrBlank() || !consumerUpiDetails!!.upiId.contains("@"))) {
                _paymentProcessStatus.value = ApiStatus.Error("Invalid UPI ID.")
                Toast.makeText(context, "Please enter a valid UPI ID.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (paymentMethod == "Card" && (consumerCardDetails?.cardNumber?.length !in 12..19)) {
                _paymentProcessStatus.value = ApiStatus.Error("Invalid card number.")
                Toast.makeText(context, "Card number must be between 12 and 19 digits.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val localOrderId = "${System.currentTimeMillis()}_${consumerId.take(5)}"
            //val backendUrl = "${BASE_URL_CONSTANT}initiate-payment"

            val jsonBody = try {
                JSONObject().apply {
                    put("amount", orderTotal)
                    put("currency", "INR")
                    put("producerAccountId", producerMerchantAccountId)
                    put("consumerId", consumerId)
                    put("paymentMethod", paymentMethod)
                    put("localOrderId", localOrderId)

                    when (paymentMethod) {
                        "UPI" -> {
                            consumerUpiDetails?.let {
                                put("upiId", it.upiId)
                            }
                        }

                        "Card" -> {
                            consumerCardDetails?.let {
                                put("cardNumber", it.cardNumber)
                                put("cardType", it.cardType)
                                put("cardBankName", it.bankName)
                            }
                        }

                        "Bank Transfer" -> {
                            consumerBankDetails?.let {
                                put("consumerAccountNumber", it.accountNumber)
                                put("consumerIfscCode", it.ifscCode)
                                put("consumerBankName", it.bankName)
                                put("consumerAccountHolderName", it.accountHolderName)
                            }
                        }
                    }
                }
            } catch (e: JSONException) {
                _paymentProcessStatus.value = ApiStatus.Error("Error creating payment request JSON.")
                Log.e("PaymentViewModel", "JSONException while building body", e)
                return@launch
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = RequestBody.create(mediaType, jsonBody.toString())
            val request = Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient()

            try {
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    try {
                        val jsonResponse = JSONObject(responseBody)
                        val razorpayOrderId = jsonResponse.optString("orderId", null)
                        val razorpayAmount = jsonResponse.optDouble("amount", -1.0)

                        if (!razorpayOrderId.isNullOrBlank() && razorpayAmount > 0) {
                            _paymentProcessStatus.value = ApiStatus.Success("Order created. Launching payment gateway.")
                            currentRazorpayOrderId = razorpayOrderId
                            startRazorpayCheckout(razorpayOrderId, razorpayAmount, context)
                        } else {
                            _paymentProcessStatus.value = ApiStatus.Error("Invalid response from server.")
                            Log.e("PaymentViewModel", "Missing orderId/amount in response: $responseBody")
                            Toast.makeText(context, "Payment initialization failed: Invalid server response", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: JSONException) {
                        _paymentProcessStatus.value = ApiStatus.Error("Malformed JSON in server response.")
                        Log.e("PaymentViewModel", "Error parsing server JSON: $responseBody", e)
                    }
                } else {
                    val errorMessage = try {
                        JSONObject(responseBody ?: "").optString("error", "Unknown backend error")
                    } catch (e: JSONException) {
                        "Unknown error from backend."
                    }
                    _paymentProcessStatus.value = ApiStatus.Error("Backend order creation failed: $errorMessage")
                    Toast.makeText(context, "Payment initiation failed: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("PaymentViewModel", "Backend error response: ${response.code} - $responseBody")
                }
            } catch (e: Exception) {
                _paymentProcessStatus.value = ApiStatus.Error("Network/API error: ${e.message}")
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("PaymentViewModel", "Exception during payment initiation", e)
            }
        }
    }


    private fun encrypt(data: String): String {
        return Base64.encodeToString(data.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
    }

    private fun decrypt(data: String): String {
        return try {
            String(Base64.decode(data, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            ""
        }
    }

    // THIS FUNCTION IS CALLED INTERNALLY BY initiateOrderAndPayment


    private fun startRazorpayCheckout(razorpayOrderId: String, amount: Double, context: Context) {
        try {
            val activity = context as? Activity ?: run {
                Toast.makeText(context, "Error: Context is not an Activity. Cannot launch Razorpay.", Toast.LENGTH_SHORT).show()
                _paymentProcessStatus.value = ApiStatus.Error("Internal error: Activity context not found for Razorpay.")
                return
            }

            val co = Checkout()
            // IMPORTANT: Use your actual PUBLIC Key ID (rzp_test_xxxxxxx or rzp_live_xxxxxxx)
            // This key is now hardcoded
            co.setKeyID(RAZORPAY_KEY_ID_CONSTANT) // <--- Now uses the hardcoded constant

            val options = JSONObject()
            options.put("name", "GreenBasket") // Your app name
            options.put("description", "Order Payment for Producer")
            options.put("currency", "INR")
            options.put("amount", (amount * 100).toLong()) // Amount in paisa
            options.put("order_id", razorpayOrderId) // The order ID from your backend

            val prefill = JSONObject()
            prefill.put("email", userDetails.value.email.ifBlank { "" })
            prefill.put("contact", userDetails.value.phone.ifBlank { "" })
            options.put("prefill", prefill)

            // You can add theme colors, image, etc. if needed
            // options.put("theme.color", "#3399CC");

            co.open(activity, options)

        } catch (e: Exception) {
            _paymentProcessStatus.value = ApiStatus.Error("Error launching Razorpay: ${e.message}")
            Toast.makeText(context, "Payment gateway failed: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("PaymentViewModel", "Error in startRazorpayCheckout", e)
        }
    }
    private fun recordTransaction(
        paymentId: String,
        orderId: String,
        consumerId: String,
        amount: Double,
        paymentMethod: String,
        context: Context
    ) {
        val transactionData = mapOf(
            "paymentId" to paymentId,
            "orderId" to orderId,
            "consumerId" to consumerId,
            "amount" to amount,
            "paymentMethod" to paymentMethod,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("transactions")
            .add(transactionData)
            .addOnSuccessListener {
                Toast.makeText(context, "Transaction recorded successfully.", Toast.LENGTH_SHORT).show()
                Log.d("ProfileViewModel", "Transaction recorded in Firestore.")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to record transaction: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ProfileViewModel", "Error recording transaction", e)
            }
    }


    /**
     * Handles the successful payment callback from Razorpay.
     * This method should be called from your hosting Activity's onPaymentSuccess.
     * It then calls your backend to verify the payment.
     * @param paymentId The Razorpay payment ID.
     */
    // THIS FUNCTION IS CALLED BY YOUR ACTIVITY (FROM onPaymentSuccess)
    fun handlePaymentSuccess(paymentId: String, context: Context) {
        viewModelScope.launch {
            _paymentProcessStatus.value = ApiStatus.Loading
            Toast.makeText(context, "Payment successful! Verifying...", Toast.LENGTH_SHORT).show()

            val consumerId = auth.currentUser?.uid
            if (consumerId == null) {
                _paymentProcessStatus.value = ApiStatus.Error("User not authenticated for verification.")
                return@launch
            }

            if (currentRazorpayOrderId == null) {
                _paymentProcessStatus.value = ApiStatus.Error("Internal error: No active order ID to verify.")
                Toast.makeText(context, "Payment success, but order ID missing for verification.", Toast.LENGTH_LONG).show()
                return@launch
            }

            val backendUrl = "${BASE_URL_CONSTANT}verify-payment"
            val jsonBody = JSONObject().apply {
                put("orderId", currentRazorpayOrderId)
                put("paymentId", paymentId)
                put("consumerId", consumerId)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = RequestBody.create(mediaType, jsonBody.toString())

            val request = Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient()

            try {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonResponse = responseBody?.let { JSONObject(it) }
                    val verificationStatus = jsonResponse?.optString("status")
                    val message = jsonResponse?.optString("message") ?: "Payment verified successfully!"

                    if (verificationStatus == "success") {
                        _paymentProcessStatus.value = ApiStatus.Success(message)
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                        val verifiedAmount = jsonResponse?.optDouble("amount") ?: 0.0
                        val producerId = jsonResponse?.optString("producerId") ?: ""
                        val localOrderId = jsonResponse?.optString("localOrderId") ?: ""

                        currentRazorpayOrderId = null

                        recordTransaction(
                            paymentId = paymentId,
                            orderId = localOrderId,
                            consumerId = consumerId,
                            amount = verifiedAmount,
                            paymentMethod = "Razorpay",
                            context = context
                        )

                        // ✅ Transfer to Producer after verification
                        if (producerId.isNotEmpty() && verifiedAmount > 0) {
                            transferToProducer(
                                producerId = producerId,
                                amount = verifiedAmount,
                                transactionId = paymentId,
                                context = context,
                                onSuccess = {
                                    Log.d("PaymentViewModel", "Transfer to producer successful")
                                    navController?.navigate("OrderStatusScreen")
                                },
                                onFailure = { errorMsg ->
                                    Log.e("PaymentViewModel", "Transfer to producer failed: $errorMsg")
                                    navController?.navigate("OrderStatusScreen")
                                }
                            )
                        } else {
                            Log.e("PaymentViewModel", "Missing producerId or amount for transfer")
                        }

                        viewModelScope.launch {
                            _paymentStatusEvent.emit(PaymentStatusEvent.PaymentSuccess)
                        }
                    } else {
                        _paymentProcessStatus.value = ApiStatus.Error("Payment verification failed: $message")
                        Toast.makeText(context, "Verification failed: $message", Toast.LENGTH_LONG).show()
                        Log.e("PaymentViewModel", "Payment verification failed: $responseBody")
                    }
                } else {
                    val errorBody = response.body?.string()
                    val errorJson = errorBody?.let { JSONObject(it) }
                    val errorMessage = errorJson?.optString("error") ?: "Unknown error during verification."
                    _paymentProcessStatus.value = ApiStatus.Error("Backend verification failed: $errorMessage")
                    Toast.makeText(context, "Verification failed: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("PaymentViewModel", "Backend verification failed with status ${response.code}: $errorBody")
                }
            } catch (e: Exception) {
                _paymentProcessStatus.value = ApiStatus.Error("Network/API error during verification: ${e.message}")
                Toast.makeText(context, "Network error during verification: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("PaymentViewModel", "Exception during payment verification", e)
            }
        }
    }

    /**
     * Handles the failed payment callback from Razorpay.
     * This method should be called from your hosting Activity's onPaymentError.
     * @param code The error code.
     * @param description The error description.
     */
    // THIS FUNCTION IS CALLED BY YOUR ACTIVITY (FROM onPaymentError)
    fun handlePaymentError(code: Int, description: String, context: Context) {
        val errorMessage = "Payment failed. Code: $code, Desc: $description"
        _paymentProcessStatus.value = ApiStatus.Error(errorMessage)
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        Log.e("PaymentViewModel", "Razorpay Payment Error: $errorMessage")
        // Optionally, you can also notify your backend about the failed payment here
        // (e.g., if you need to update an order status to 'failed')
        currentRazorpayOrderId = null // Clear the stored order ID
        viewModelScope.launch {
            _paymentStatusEvent.emit(PaymentStatusEvent.PaymentFailure)
        }
        navController?.navigate("PaymentScreen")
    }
    fun transferToProducer(
        producerId: String,
        amount: Double,
        transactionId: String,
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            _paymentProcessStatus.value = ApiStatus.Loading

            val backendUrl = "${BASE_URL_CONSTANT}transfer"

            val jsonBody = JSONObject().apply {
                put("producerId", producerId)
                put("amount", amount)
                put("transactionId", transactionId)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = RequestBody.create(mediaType, jsonBody.toString())

            val request = Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient()

            try {
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.optBoolean("success", false)) {
                        _paymentProcessStatus.value = ApiStatus.Success("Transfer successful!")
                        onSuccess()
                    } else {
                        val errorMsg = "Transfer failed on server."
                        _paymentProcessStatus.value = ApiStatus.Error(errorMsg)
                        onFailure(errorMsg)
                    }
                } else {
                    val errorMsg = "Transfer failed: $responseBody"
                    _paymentProcessStatus.value = ApiStatus.Error(errorMsg)
                    onFailure(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "Transfer network error: ${e.message}"
                _paymentProcessStatus.value = ApiStatus.Error(errorMsg)
                onFailure(errorMsg)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun loadAccept5to10Status() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        Firebase.firestore.collection("accept5to10").document(uid).get()
            .addOnSuccessListener { doc ->
                acceptOnly5To10.value = doc.exists()
            }
    }

    fun updateAccept5to10Status(accept: Boolean) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val docRef = Firebase.firestore.collection("accept5to10").document(uid)

        if (accept) {
            docRef.set(mapOf("active" to true))
        } else {
            docRef.delete()
        }

        acceptOnly5To10.value = accept
    }


}
