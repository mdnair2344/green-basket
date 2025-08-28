//AuthenticationViewModel
package com.igdtuw.greenbasket.ui.authentication

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class AuthenticationViewModel (private val googleAuthUiClient: GoogleAuthUiClient): ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()


    fun signUpWithEmail(
        name: String,
        address: String,
        phone: String,
        email: String,
        password: String,
        userType: String,
        farmName: String,
        context: Context,
        onResult: (Boolean) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    val userMap = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "address" to address,
                        "phone" to if (phone.startsWith("+91")) phone else "+91$phone",
                        "userType" to userType,
                        "farmName" to if (userType == "Producer") farmName else "",
                        "email" to email
                    )

                    uid?.let {
                        firestore.collection("users").document(it).set(userMap)
                            .addOnSuccessListener {
                                saveUserRole(context, userType)
                                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                onResult(true)
                            }.addOnFailureListener { e ->
                                Toast.makeText(context, "Firestore error: ${e.message}", Toast.LENGTH_SHORT).show()
                                onResult(false)
                            }
                    } ?: run {
                        Toast.makeText(context, "UID not found", Toast.LENGTH_SHORT).show()
                        onResult(false)
                    }
                } else {
                    Toast.makeText(context, "Sign-up error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            }
    }

    /*fun signInWithEmail(
        email: String,
        password: String,
        context: Context,
        onResult: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid
                    uid?.let {
                        firestore.collection("users").document(it).get()
                            .addOnSuccessListener { document ->
                                val userType = document.getString("userType")
                                if (userType != null) {
                                    saveUserRole(context, userType)
                                    onResult(userType)
                                } else {
                                    Toast.makeText(context, "User type not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } ?: run {
                        Toast.makeText(context, "UID not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }*/


    fun signInWithEmail(email: String, password: String, context: Context, onResult: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user

                // 🔄 Try to link with Google if previously signed in
                val googleAccount =     GoogleSignIn.getLastSignedInAccount(context)
                if (googleAccount != null && googleAccount.idToken != null) {
                    val googleCredential = GoogleAuthProvider.getCredential(googleAccount.idToken, null)
                    user?.linkWithCredential(googleCredential)
                        ?.addOnSuccessListener {
                            // Google account linked successfully
                        }
                        ?.addOnFailureListener {
                            // Optional: Log this or show a toast if needed
                        }
                }

                // Get user type from Firestore
                firestore.collection("users").document(user!!.uid).get()
                    .addOnSuccessListener { doc ->
                        val userType = doc.getString("userType")
                        onResult(userType)
                    }
                    .addOnFailureListener {
                        onResult(null)
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
    }


    fun signInWithGoogle(
        account: GoogleSignInAccount,
        googleAuthUiClient: GoogleAuthUiClient,
        context: Context,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit,
        onNavigateToSignUp: () -> Unit
    ) {
        val googleCredential = GoogleAuthProvider.getCredential(account.idToken, null)
        val currentUser = auth.currentUser
        FirebaseAuth.getInstance().currentUser?.linkWithCredential(googleCredential)
            ?.addOnSuccessListener {
                Log.d("Auth", "Google account linked to email/pwd account")
            }
            ?.addOnFailureListener {
                Log.e("Auth", "Failed to link Google: ${it.message}")
            }

        if (currentUser != null) {
            currentUser.linkWithCredential(googleCredential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        firestore.collection("users").document(currentUser.uid).get()
                            .addOnSuccessListener { document ->
                                val userType = document.getString("userType")
                                if (userType != null) {
                                    saveUserRole(context, userType)
                                    onSuccess(userType)
                                } else {
                                    onFailure("User type not found.")
                                }
                            }
                    } else {
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            auth.signInWithCredential(googleCredential).addOnCompleteListener { loginTask ->
                                if (loginTask.isSuccessful) {
                                    val user = loginTask.result?.user
                                    firestore.collection("users").document(user?.uid ?: "").get()
                                        .addOnSuccessListener { document ->
                                            val userType = document.getString("userType")
                                            if (userType != null) {
                                                saveUserRole(context, userType)
                                                onSuccess(userType)
                                            } else {
                                                onFailure("User type not found.")
                                            }
                                        }
                                } else {
                                    onFailure("Login failed: ${loginTask.exception?.message}")
                                }
                            }
                        } else {
                            onFailure("Google link failed: ${task.exception?.message}")
                        }
                    }
                }
        } else {
            googleAuthUiClient.signInWithGoogleCredential(googleCredential) { result ->
                when (result) {
                    is AuthResultStatus.Success -> {
                        val uid = result.user.uid
                        firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                val userType = document.getString("userType")
                                if (userType != null) {
                                    saveUserRole(context, userType)
                                    onSuccess(userType)
                                } else {
                                    onFailure("User type not found.")
                                }
                            }
                    }

                    is AuthResultStatus.Failure -> {
                        onFailure(result.message)
                    }

                    is AuthResultStatus.NavigateToSignUp -> {
                        onNavigateToSignUp()
                    }
                }
            }
        }
    }

    fun sendOtp(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

                override fun onVerificationFailed(e: FirebaseException) {
                    onFailure(e.message ?: "OTP verification failed.")
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    onCodeSent(verificationId)
                }
            }).build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(
        verificationId: String,
        otp: String,
        context: Context,
        onSuccess: (String) -> Unit
    ) {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        val currentUser = auth.currentUser

        if (currentUser != null) {
            currentUser.linkWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val phoneNumber = task.result?.user?.phoneNumber
                        if (phoneNumber != null) {
                            firestore.collection("users").document(currentUser.uid)
                                .update("phone", phoneNumber)
                                .addOnSuccessListener {
                                    firestore.collection("users").document(currentUser.uid).get()
                                        .addOnSuccessListener { document ->
                                            val userType = document.getString("userType")
                                            if (userType != null) {
                                                saveUserRole(context, userType)
                                                onSuccess(userType)
                                            } else {
                                                Toast.makeText(context, "User type not found", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                        } else {
                            Toast.makeText(context, "Phone number not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (task.exception is FirebaseAuthUserCollisionException) {
                            auth.signInWithCredential(credential)
                                .addOnCompleteListener { signInTask ->
                                    if (signInTask.isSuccessful) {
                                        val user = signInTask.result?.user
                                        val phoneNumber = user?.phoneNumber
                                        if (phoneNumber != null) {
                                            firestore.collection("users")
                                                .whereEqualTo("phone", phoneNumber)
                                                .get()
                                                .addOnSuccessListener { documents ->
                                                    if (!documents.isEmpty) {
                                                        val userType = documents.documents[0].getString("userType")
                                                        if (userType != null) {
                                                            saveUserRole(context, userType)
                                                            onSuccess(userType)
                                                        }
                                                    }
                                                }
                                        }
                                    } else {
                                        Toast.makeText(context, "OTP Sign-in failed: ${signInTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(context, "Phone linking failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        } else {
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val phoneNumber = user?.phoneNumber
                        if (phoneNumber != null) {
                            firestore.collection("users")
                                .whereEqualTo("phone", phoneNumber)
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (!documents.isEmpty) {
                                        val userType = documents.documents[0].getString("userType")
                                        if (userType != null) {
                                            saveUserRole(context, userType)
                                            onSuccess(userType)
                                        }
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(context, "Invalid OTP: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    fun signOut(context: Context) {
        auth.signOut()
        googleAuthUiClient.signOut { /* Optional: handle completion of Google sign out */ } // ✅ Add this line!
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    private fun saveUserRole(context: Context, role: String) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("role", role.lowercase()).apply()
    }

    fun getUserType(
        uid: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val userType = document.getString("userType")
                if (userType != null) {
                    onResult(userType)
                } else {
                    onError("User type not found")
                }
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Failed to fetch user type")
            }
    }
}


class AuthenticationViewModelFactory(
    private val googleAuthUiClient: GoogleAuthUiClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthenticationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthenticationViewModel(googleAuthUiClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}