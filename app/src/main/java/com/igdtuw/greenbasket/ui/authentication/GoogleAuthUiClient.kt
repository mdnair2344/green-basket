package com.igdtuw.greenbasket.ui.authentication

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore

sealed class AuthResultStatus {
    data class Success(val user: FirebaseUser) : AuthResultStatus()
    data class Failure(val message: String) : AuthResultStatus()
    object NavigateToSignUp : AuthResultStatus()
}

class GoogleAuthUiClient(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val gso: GoogleSignInOptions
) {
    private val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    // Get intent for sign-in (should only be called after signOut)
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // Force Google to show account chooser by signing out first
    fun signOut(onSignOut: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            auth.signOut()
            onSignOut()
        }
    }

    // Handles actual sign-in with Firebase credential
    fun signInWithGoogleCredential(
        credential: AuthCredential,
        onResult: (AuthResultStatus) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        firestore.collection("users").document(user.uid).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // ✅ User exists in Firestore, allow login
                                    onResult(AuthResultStatus.Success(user))
                                } else {
                                    // ❌ Firestore record doesn't exist; delete auth user
                                    user.delete()
                                        .addOnSuccessListener {
                                            onResult(AuthResultStatus.NavigateToSignUp)
                                        }
                                        .addOnFailureListener {
                                            onResult(AuthResultStatus.Failure("Unable to delete Firebase user. Try again."))
                                        }

                                }
                            }
                            .addOnFailureListener {
                                onResult(AuthResultStatus.Failure("Firestore check failed"))
                            }
                    } ?: run {
                        onResult(AuthResultStatus.Failure("No user found"))
                    }
                } else {
                    onResult(AuthResultStatus.Failure(task.exception?.message ?: "Sign-in failed"))
                }
            }
    }
}
