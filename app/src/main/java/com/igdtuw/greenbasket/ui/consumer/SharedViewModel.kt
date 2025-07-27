//SharedViewModel
package com.igdtuw.greenbasket.ui.consumer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import android.annotation.SuppressLint
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.Badge
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter


// import com.igdtuw.greenbasket.UserData // REMOVED - no longer using global UserData object


// --- CENTRALIZED DATA CLASS DEFINITIONS START ---
// These should ONLY be defined here. Remove from all other files (MyOrdersScreen, WishlistScreen, etc.)
// Make sure Cert and MediaItem are also defined if used in Crop
data class Prod(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val unit: String = "",
    val category: String = ""
)


/*
data class Crop(
    val id: String = UUID.randomUUID().toString(), // Ensure ID is generated for new crops if not from Firestore
    val name: String = "",
    val category: String = "",
    val variety: String = "",
    val type: String = "",
    var quantity: String = "", // e.g., "1.0 kg", "500 gm"
    var pricePerKg: Double = 0.00,
    var description: String = "",
    val producer: String = "",
    //val mediaItems: List<MediaItem> = emptyList(),
    //val harvestingInfo: String = "",
    var imageUri: String? = null,
    //val certifications: List<Cert> = emptyList(),
)*/

// This data class holds information about a product that can be added to the cart
data class CartableProductInfo(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val producerId: String = "" // ✅ Correctly typed and defaulted
)

data class CartItem(
    val product: CartableProductInfo = CartableProductInfo(),
    var quantity: Int = 0
)


@Parcelize
data class UserAddress(
    val fullName: String = "",
    val phone: String = "",
    val address: String = ""
)
: Parcelable {
    val fullAddress: String
        get() = "$address"
}

@Parcelize
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val imageUrl: String? = null,
    val quantity: Int = 0,
    val unitPrice: Double = 0.0
): Parcelable

@Parcelize

data class Order(
    val orderId: String = "",
    val userId: String = "", // Now strictly using UID
    val userEmail: String = "", // Can still keep for reference
    val orderDate: Long = 0L,
    val deliveryAddress: String = "",
    val deliveryFullName: String = "",
    val deliveryPhone: String = "",
    val items: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = "Pending",
    val paymentMethod: String = "",
    val paymentId: String = "" ,     //From razorpay
    val producerId: String = ""
): Parcelable

data class Certification(
    val id: String,
    val producerId: String,      // ← ties back to Producer.producerId
    val name: String,
    val description: String,
    val issueDate: String,
    val expiryDate: String,
    val imageUrl: String,
    val authority: String,
    val status: String           // "Active", "Expired", "Pending"
)


@Parcelize
data class UserUpiDetails(
    val id: String = "",
    val upiId: String = ""
) : Parcelable

@Parcelize
data class UserCardDetails(
    val id: String = "",
    val cardNumber: String = "",
    val cardType: String = "",
    val bankName: String = "",
    val cvv: String = ""
) : Parcelable

@Parcelize
data class UserBankDetails(
    val id: String = "",
    val accountNumber: String = "",
    val bankName: String = "",
    val ifscCode: String = "",
    val accountHolderName: String = ""
) : Parcelable


// Represents a crop that has been wishlisted
data class WishlistedCrop(
    val crop: Crop = Crop(), // The Crop object that is wishlisted
    val addedAt: String = "" // Timestamp or date when it was added to wishlist
)

// At top of ViewModel
val showUndoWishlistRemovalPopup = mutableStateOf(false)
var recentlyRemovedWishlistItem: WishlistedCrop? = null


// --- CENTRALIZED DATA CLASS DEFINITIONS END ---


@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class SharedViewModel @Inject constructor(
    private val auth: FirebaseAuth, // Injected FirebaseAuth
    private val db: FirebaseFirestore // Injected FirebaseFirestore
) : ViewModel() {


    val userId = MutableStateFlow<String?>(null)

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser
        userId.value = currentUser?.uid
    }


    fun isLoggedIn(): Boolean = auth.currentUser != null
    val firestore = FirebaseFirestore.getInstance()

    fun getUserId(): String? = auth.currentUser?.uid

    fun getUserData(callback: (Map<String, Any>?) -> Unit) {
        val userId = getUserId() ?: return callback(null)
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                callback(document.data)
            }
            .addOnFailureListener {
                callback(null)
            }
    }


    private fun getUnitFromQuantityString(quantityString: String): String {
        return quantityString.split(" ").getOrElse(1) { "unit" }
    }

    val producerMerchantAccountId = MutableStateFlow<String?>(null)
    val orderTotal = MutableStateFlow<Double?>(null)


    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartCount: StateFlow<Int> = _cartItems
        .map { it.sumOf { item -> item.quantity } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cartTotal: StateFlow<Float> = cartItems
        .map { items ->
            items.sumOf { it.quantity * it.product.price }
        }
        .map { it.toFloat() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    private val _selectedProduct = MutableStateFlow<Crop?>(null)
    val selectedProduct: StateFlow<Crop?> = _selectedProduct

    fun selectProduct(product: Crop) {
        _selectedProduct.value = product
    }

    private val _consumerId = MutableStateFlow<String?>(null)
    val consumerId: StateFlow<String?> get() = _consumerId

    fun setConsumerId(id: String) {
        _consumerId.value = id
    }
    private val _wishlistItems = MutableStateFlow<List<WishlistedCrop>>(emptyList())
    val wishlistItems: StateFlow<List<WishlistedCrop>> = _wishlistItems.asStateFlow()

    val wishlistCount: StateFlow<Int> = _wishlistItems
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    var showCartAddedPopup = mutableStateOf(false)
    var showWishlistAddedPopup = mutableStateOf(false)

    private val _userAddresses = MutableStateFlow<List<UserAddress>>(emptyList())
    val userAddresses: StateFlow<List<UserAddress>> = _userAddresses.asStateFlow()

    private val _selectedAddress = MutableStateFlow<UserAddress?>(null)
    val selectedAddress: StateFlow<UserAddress?> = _selectedAddress.asStateFlow()


    // StateFlow for consumer's (user's) address
    private val _consumerAddress = MutableStateFlow<UserAddress?>(null)
    val selectedConsumerAddress: StateFlow<UserAddress?> = _consumerAddress

    // StateFlow for producer's address
    private val _producerAddress = MutableStateFlow<UserAddress?>(null)
    val selectedProducerAddress: StateFlow<UserAddress?> = _producerAddress




    private val _addressToConfirm = MutableStateFlow<UserAddress?>(null)
    val addressToConfirm: StateFlow<UserAddress?> = _addressToConfirm

    fun setAddressToConfirm(address: UserAddress) {
        _addressToConfirm.value = address
    }

    fun clearAddressToConfirm() {
        _addressToConfirm.value = null
    }

    private val _latestOrder = MutableStateFlow<Order?>(null)
    val latestOrder: StateFlow<Order?> = _latestOrder // ✅ expose as StateFlow

    fun setLatestOrder(order: Order) {
        _latestOrder.value = order
    }


    private val _userAddress = MutableStateFlow<UserAddress?>(null)
    val userAddress: StateFlow<UserAddress?> = _userAddress

    fun setUserAddress(address: UserAddress) {
        _userAddress.value = address
    }


    private val _appliedDiscounts = MutableStateFlow<List<DiscountOffer>>(emptyList())
    val appliedDiscounts: StateFlow<List<DiscountOffer>> = _appliedDiscounts

    fun setAppliedDiscounts(discounts: List<DiscountOffer>) {
        _appliedDiscounts.value = discounts
    }


    fun clearUserAddress() {
        _userAddress.value = null
    }




    private val _currentOrderDetails = MutableStateFlow<Order?>(null)
    val currentOrderDetails: StateFlow<Order?> = _currentOrderDetails.asStateFlow()

    private val _selectedProductDetails = MutableStateFlow<Crop?>(null)
    val selectedProductDetails: StateFlow<Crop?> = _selectedProductDetails.asStateFlow()
    //val latestOrder = MutableStateFlow<Order?>(null)
    sealed class OrderStatus {
        object Idle : OrderStatus()
        object Loading : OrderStatus()
        object Success : OrderStatus()
        data class Error(val message: String) : OrderStatus()
    }

    private val _orderCreationStatus = MutableStateFlow<OrderStatus>(OrderStatus.Idle)
    val orderCreationStatus: StateFlow<OrderStatus> = _orderCreationStatus.asStateFlow()




    private val _savedUpiDetails = MutableStateFlow<List<UserUpiDetails>>(emptyList())
    val savedUpiDetails: StateFlow<List<UserUpiDetails>> = _savedUpiDetails

    private val _savedCardDetails = MutableStateFlow<List<UserCardDetails>>(emptyList())
    val savedCardDetails: StateFlow<List<UserCardDetails>> = _savedCardDetails

    private val _savedBankDetails = MutableStateFlow<List<UserBankDetails>>(emptyList())
    val savedBankDetails: StateFlow<List<UserBankDetails>> = _savedBankDetails


    private val _deliveryOption = MutableStateFlow("home")
    val deliveryOption: StateFlow<String> = _deliveryOption

    fun setDeliveryOption(option: String) {
        _deliveryOption.value = option
    }



    fun loadProducerAddressFromCart() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Step 1: Get one cart item to retrieve producerId
                val cartSnapshot = db.collection("consumers")
                    .document(userId)
                    .collection("cart")
                    .limit(1)
                    .get()
                    .await()

                if (!cartSnapshot.isEmpty) {
                    val cartItem = cartSnapshot.documents[0]
                    val producerId = cartItem.getString("producerId")

                    // Step 2: Fetch producer info from `users/{producerId}`
                    if (producerId != null) {
                        val producerDoc = db.collection("users")
                            .document(producerId)
                            .get()
                            .await()

                        val name = producerDoc.getString("name") ?: "Producer"
                        val address = producerDoc.getString("address") ?: "Address not found"
                        val phone = producerDoc.getString("phone") ?: "N/A" // Optional

                        _producerAddress.value = UserAddress( // Update producer address
                            fullName = name,
                            phone = phone,
                            address = address
                        )
                    } else {
                        Log.w("SharedViewModel", "Producer ID not found in cart item.")
                        _producerAddress.value = null
                    }
                } else {
                    Log.d("SharedViewModel", "Cart is empty, cannot load producer address.")
                    _producerAddress.value = null
                }
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error loading producer address: ${e.message}")
                _producerAddress.value = null
            }
        }
    }



    fun resetOrderCreationStatus() {
        _orderCreationStatus.value = OrderStatus.Idle
    }

    init {
        // Observe changes in auth.currentUser to react to login/logout
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                // Ensure these are fetched only when a user is logged in
                fetchWishlistRealtime(uid)
                fetchCartRealtime(uid) // Changed to real-time cart fetch
                loadUserAddress() // Load address based on UID
            } else {
                // Clear data if user logs out
                _wishlistItems.value = emptyList()
                _cartItems.value = emptyList()
                _userAddresses.value = emptyList()
                _selectedAddress.value = null
            }
        }
    }

    // --- REVISED addToCart: Stores each item as a document in consumers/{uid}/cart/{productId} ---
    @RequiresApi(Build.VERSION_CODES.O)
    fun addToCart(product: Crop) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("SharedViewModel", "User UID is null, cannot add to cart")
            return
        }

        // Reference to the specific item in the user's cart subcollection
        val cartItemDocRef = db.collection("consumers").document(userId)
            .collection("cart").document(product.id) // Document ID is product.id

        // Declare a mutable variable to hold the new quantity outside the transaction
        // This will be updated inside the transaction and then used in the success listener
        var newCalculatedQuantity: Int = 0 // Initialize to 0

        db.runTransaction { transaction ->
            val snapshot = transaction.get(cartItemDocRef)
            val currentQuantity = snapshot.getLong("quantity")?.toInt() ?: 0
            val newQuantity = currentQuantity + 1
            newCalculatedQuantity = newQuantity // Assign the calculated quantity to the outer variable

            /*// Prepare the CartableProductInfo to be stored
            val cartableProductInfo = CartableProductInfo(
                id = product.id,
                name = product.name,
                price = product.pricePerKg
                //unit = getUnitFromQuantityString(product.quantity),
                //imageUrl = product.imageUri
            )

            // The CartItem data to be written to the document
            val cartItemData = CartItem(
                product = cartableProductInfo,
                quantity = newQuantity
            )*/



            val cartableProductInfo = CartableProductInfo(
                id = product.id,
                name = product.name,
                price = product.pricePerKg,
                producerId = product.producer
            )

            val cartItemData = mapOf(
                "product" to mapOf(
                    "id" to cartableProductInfo.id,
                    "name" to cartableProductInfo.name,
                    "price" to cartableProductInfo.price, // Stored explicitly as Double
                    "producerId" to cartableProductInfo.producerId
                ),
                "quantity" to newQuantity // Int (safe)
            )



            transaction.set(cartItemDocRef, cartItemData, SetOptions.merge())
            null // Return null for success
        }.addOnSuccessListener {
            // Now newCalculatedQuantity is accessible here
            Log.d("SharedViewModel", "Cart updated successfully for ${product.name}, new quantity: $newCalculatedQuantity for UID: $userId")
            // The real-time listener (fetchCartRealtime) will update _cartItems.value
            showCartAddedPopup.value = true
        }.addOnFailureListener { e ->
            Log.e("SharedViewModel", "Failed to update cart: ${e.message}", e)
        }
    }

    // --- REVISED removeFromCart: Deletes the specific product document ---
    fun removeFromCart(cartItem: CartItem) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w("SharedViewModel", "User UID is null, cannot remove from cart.")
            return
        }
        viewModelScope.launch {
            db.collection("consumers").document(userId)
                .collection("cart")
                .document(cartItem.product.id) // Target the document by its product ID
                .delete()
                .addOnSuccessListener {
                    Log.d("SharedViewModel", "Item removed from cart in Firestore: ${cartItem.product.name}")
                    // The real-time listener will update _cartItems.value
                }
                .addOnFailureListener { e ->
                    Log.e("SharedViewModel", "Failed to remove item from cart in Firestore", e)
                }
        }
    }

    // --- REVISED fetchCartRealtime: Iterates through documents in cart subcollection ---
    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchCartRealtime(userId: String) {
        Log.d("SharedViewModel", "Starting fetchCartRealtime for UID: $userId")

        db.collection("consumers").document(userId).collection("cart")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SharedViewModel", "Real-time cart error: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.e("SharedViewModel", "Snapshot is NULL")
                    _cartItems.value = emptyList()
                    return@addSnapshotListener
                }

                Log.d("SharedViewModel", "Snapshot received with ${snapshot.size()} documents")

                val items = mutableListOf<CartItem>()
                for (doc in snapshot.documents) {
                    Log.d("SharedViewModel", "Doc ID: ${doc.id}, data: ${doc.data}")
                    try {
                        val cartItem = doc.toObject(CartItem::class.java)
                        if (cartItem != null && doc.id != "init") {
                            items.add(cartItem)
                        } else {
                            Log.w("SharedViewModel", "CartItem is null or 'init'")
                        }
                    } catch (e: Exception) {
                        Log.e("SharedViewModel", "Parsing error for doc ${doc.id}: ${e.message}", e)
                    }
                }

                _cartItems.value = items
                Log.d("SharedViewModel", "CartItems updated with ${items.size} items")
            }
    }



    // --- REVISED increaseQuantity: Targets consumers/{uid}/cart/{productId} ---
    fun increaseQuantity(cartItem: CartItem) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w("SharedViewModel", "User UID is null, cannot increase quantity.")
            return
        }

        db.collection("consumers").document(userId)
            .collection("cart")
            .document(cartItem.product.id) // Target the document by its product ID
            .update("quantity", FieldValue.increment(1)) // Use FieldValue.increment
            .addOnSuccessListener {
                Log.d("SharedViewModel", "Quantity increased for ${cartItem.product.name}.")
                // The real-time listener will update _cartItems.value
            }
            .addOnFailureListener { e ->
                Log.e("SharedViewModel", "Error increasing quantity for ${cartItem.product.name}", e)
            }
    }

    // --- REVISED decreaseQuantity: Targets consumers/{uid}/cart/{productId} ---
    fun decreaseQuantity(cartItem: CartItem) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w("SharedViewModel", "User UID is null, cannot decrease quantity.")
            return
        }

        val itemDoc = db.collection("consumers").document(userId)
            .collection("cart")
            .document(cartItem.product.id) // Target the document by its product ID

        // Fetch current quantity to decide whether to update or delete
        itemDoc.get().addOnSuccessListener { documentSnapshot ->
            val currentQuantity = documentSnapshot.getLong("quantity")?.toInt() ?: 0
            if (currentQuantity > 1) {
                itemDoc.update("quantity", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Log.d("SharedViewModel", "Quantity decreased for ${cartItem.product.name}.")
                        // The real-time listener will update _cartItems.value
                    }
                    .addOnFailureListener { e ->
                        Log.e("SharedViewModel", "Error decreasing quantity for ${cartItem.product.name}", e)
                    }
            } else {
                // If quantity is 1, remove the item
                itemDoc.delete()
                    .addOnSuccessListener {
                        Log.d("SharedViewModel", "Item removed from cart: ${cartItem.product.name}.")
                        // The real-time listener will update _cartItems.value
                    }
                    .addOnFailureListener { e ->
                        Log.e("SharedViewModel", "Error removing item from cart: ${cartItem.product.name}", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("SharedViewModel", "Error fetching quantity to decrease: ${e.message}", e)
        }
    }


    // --- REVISED addToWishlist: Stores each item as a document in consumers/{uid}/wishlist/{cropId} ---
    fun addToWishlist(crop: Crop) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("SharedViewModel", "User UID is null, cannot add to wishlist.")
            return
        }

        val wishlistRef = db.collection("consumers").document(userId).collection("wishlist")

        val wishlistedCrop = WishlistedCrop(
            crop = crop,
            addedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )

        // Use crop.id as the document ID for the wishlisted item for easy lookup and removal
        wishlistRef.document(crop.id).set(wishlistedCrop) // Document ID is crop.id
            .addOnSuccessListener {
                // The real-time listener (fetchWishlistRealtime) will update _wishlistItems.value
                showWishlistAddedPopup.value = true
                Log.d("SharedViewModel", "Added ${crop.name} to wishlist in Firestore for UID: $userId.")
            }
            .addOnFailureListener { e ->
                Log.e("SharedViewModel", "Error adding to wishlist in Firestore: ${e.message}", e)
            }
    }

    // --- Modified removeFromWishlistWithUndo: Targets consumers/{uid}/wishlist/{cropId} ---
    fun removeFromWishlistWithUndo(cropId: String) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w("SharedViewModel", "User UID is null, cannot remove from wishlist.")
            return
        }

        val wishlistRef = db.collection("consumers").document(userId).collection("wishlist")

        // Save the item to restore later if undo
        recentlyRemovedWishlistItem = _wishlistItems.value.find { it.crop.id == cropId }

        wishlistRef.document(cropId).delete() // Target the document by its crop ID
            .addOnSuccessListener {
                // The real-time listener will update _wishlistItems.value
                showUndoWishlistRemovalPopup.value = true

                viewModelScope.launch {
                    delay(5000)
                    if (showUndoWishlistRemovalPopup.value) {
                        // User didn’t undo
                        recentlyRemovedWishlistItem = null
                        showUndoWishlistRemovalPopup.value = false
                    }
                }
                Log.d("SharedViewModel", "Removed $cropId from wishlist in Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e("SharedViewModel", "Error removing from wishlist in Firestore: ${e.message}", e)
            }
    }

    // --- Call this on UNDO button click ---
    fun undoRemoveFromWishlist() {
        val userId = auth.currentUser?.uid ?: return
        val crop = recentlyRemovedWishlistItem ?: return

        val wishlistRef = db.collection("consumers").document(userId).collection("wishlist")

        wishlistRef.document(crop.crop.id).set(crop) // Target the document by its crop ID
            .addOnSuccessListener {
                // The real-time listener will update _wishlistItems.value
                Log.d("SharedViewModel", "Undo successful for ${crop.crop.name} for UID: $userId")
            }
            .addOnFailureListener { e ->
                Log.e("SharedViewModel", "Error undoing wishlist removal: ${e.message}", e)
            }

        recentlyRemovedWishlistItem = null
        showUndoWishlistRemovalPopup.value = false
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    @Composable
    fun WishlistBadge(viewModel: SharedViewModel) {
        val items = wishlistItems.value

        if (items.isNotEmpty()) {
            Badge(
                containerColor = Color.Red,
                contentColor = Color.White
            ) {
                Text("${items.size}")
            }
        }
    }

    // --- Real-time fetchWishlist using SnapshotListener (updated for document-based subcollection) ---
    fun fetchWishlistRealtime(userId: String) {
        db.collection("consumers").document(userId).collection("wishlist")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SharedViewModel", "Real-time wishlist error for UID $userId: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) { // snapshot might be empty, but not null
                    val items = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(WishlistedCrop::class.java)
                        } catch (e: Exception) {
                            Log.e("SharedViewModel", "Error parsing WishlistedCrop from document ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
                    _wishlistItems.value = items
                    Log.d("SharedViewModel", "Wishlist loaded: ${items.size} items for UID: $userId")
                } else {
                    _wishlistItems.value = emptyList()
                    Log.d("SharedViewModel", "Wishlist is empty or no data for UID: $userId")
                }
            }
    }

    fun hidePopups() {
        showCartAddedPopup.value = false
        showWishlistAddedPopup.value = false
    }

    // --- loadUserAddress and updateUserAddress remain largely the same, targeting consumers/{uid} ---
    fun loadUserAddress() {
        val userId = auth.currentUser?.uid ?: run {
            Log.w("SharedViewModel", "User not logged in")
            _consumerAddress.value = null // Update consumer address
            return
        }

        Log.d("Address Debug", "Fetching address for UID: $userId")

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                Log.d("Address Debug", "Doc data: ${doc.data}")
                if (doc.exists()) {
                    val fullName = doc.getString("name") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val address = doc.getString("address") ?: ""
                    // Consider how fullAddress is populated if needed for AddressSelectionCard
                    _consumerAddress.value = UserAddress(fullName, phone, address) // Assuming address can be fullAddress or you derive it
                } else {
                    Log.w("Address Debug", "No document found")
                    _consumerAddress.value = null
                }
            }
            .addOnFailureListener {
                Log.e("Address Debug", "Failed to fetch address: ${it.message}")
                _consumerAddress.value = null
            }
    }


    fun selectAddress(address: UserAddress) {
        _selectedAddress.value = address
    }

    fun setSelectedProductDetails(crop: Crop) {
        _selectedProductDetails.value = crop
    }

    fun clearSelectedProductDetails() {
        _selectedProductDetails.value = null
    }




    fun updateUserAddress(address: UserAddress) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w("SharedViewModel", "User not logged in, cannot update address.")
            return
        }
        val userDoc = db.collection("users").document(userId) // ✅ CORRECTED
        userDoc.update(
            mapOf(
                "name" to address.fullName,
                "phone" to address.phone,
                "address" to address.address
            )
        ).addOnSuccessListener {
            _selectedAddress.value = address
            Log.d("SharedViewModel", "Address updated successfully in Firestore for UID: $userId.")
        }.addOnFailureListener { e ->
            Log.e("SharedViewModel", "Failed to update address for UID $userId: ${e.message}", e)
        }
    }




    fun prepareOrderDetails(
        deliveryFullName: String,
        deliveryPhone: String,
        deliveryAddress: String,
        items: List<OrderItem>,
        totalAmount: Double,
        paymentMethod: String
    ) {
        val userId = auth.currentUser?.uid ?: run {
            Log.w("SharedViewModel", "User not logged in. Cannot prepare order.")
            return
        }
        val userEmail = auth.currentUser?.email ?: "" // User email might be null for phone auth

        val order = Order(
            userId = userId,
            userEmail = userEmail,
            deliveryFullName = deliveryFullName,
            deliveryPhone = deliveryPhone,
            deliveryAddress = deliveryAddress,
            items = items,
            totalAmount = totalAmount,
            status = "Pending",
            paymentMethod = paymentMethod
        )
        _currentOrderDetails.value = order
    }

    // --- REVISED createNewOrder: Now stores each order as a document in consumers/{uid}/orders/{orderId} ---
    fun createNewOrder(
        deliveryFullName: String,
        deliveryPhone: String,
        deliveryAddress: String,
        items: List<OrderItem>,
        totalAmount: Double,
        paymentMethod: String,
        onSuccess: (Order) -> Unit,
        onFailure: () -> Unit
    ) {
        _orderCreationStatus.value = OrderStatus.Loading

        val userId = auth.currentUser?.uid ?: run {
            Log.e("SharedViewModel", "User not logged in. Cannot create order.")
            _orderCreationStatus.value = OrderStatus.Error("User not logged in.")
            onFailure()
            return
        }

        val userEmail = auth.currentUser?.email ?: ""

        val consumerOrdersRef = db.collection("orders")
        val newOrderDocRef = consumerOrdersRef.document()

        val orderId = newOrderDocRef.id

        val order = Order(
            orderId = orderId,
            userId = userId,
            userEmail = userEmail,
            deliveryFullName = deliveryFullName,
            deliveryPhone = deliveryPhone,
            deliveryAddress = deliveryAddress,
            items = items,
            totalAmount = totalAmount,
            status = "Pending",
            paymentMethod = paymentMethod
        )

        newOrderDocRef.set(order)
            .addOnSuccessListener {
                Log.d("SharedViewModel", "Order created successfully with ID: $orderId")
                _orderCreationStatus.value = OrderStatus.Success
                _latestOrder.value = order // ✅ <- This is critical

                onSuccess(order) // ✅ <- Also this
                clearCart(userId)
            }
            .addOnFailureListener { e ->
                Log.e("SharedViewModel", "Failed to create order: ${e.message}", e)
                _orderCreationStatus.value = OrderStatus.Error("Failed to create order: ${e.message}")
                onFailure()
            }
    }


    // New function to clear the cart after an order
    private fun clearCart(userId: String) {
        db.collection("consumers").document(userId).collection("cart")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (document in snapshot.documents) {
                    // Exclude the "init" document if it was created
                    if (document.id != "init") {
                        batch.delete(document.reference)
                    }
                }
                batch.commit()
                    .addOnSuccessListener {
                        _cartItems.value = emptyList() // Clear local state
                        Log.d("SharedViewModel", "Cart cleared successfully for UID: $userId.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("SharedViewModel", "Failed to clear cart for UID $userId: ${e.message}", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("SharedViewModel", "Failed to fetch cart documents to clear: ${e.message}", e)
            }
    }

    fun loadOrderById(orderId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("orders").document(orderId).get().await()
                val order = snapshot.toObject(Order::class.java)
                _latestOrder.value = order
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Failed to load order: ${e.message}")
            }
        }
    }


}