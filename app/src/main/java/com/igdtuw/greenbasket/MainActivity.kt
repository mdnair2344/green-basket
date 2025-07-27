//MainActivity
package com.igdtuw.greenbasket

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.navigation.SetupNavGraph
import com.igdtuw.greenbasket.ui.authentication.GoogleAuthUiClient
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import com.razorpay.PaymentResultListener


@AndroidEntryPoint
class MainActivity : ComponentActivity(), PaymentResultListener {

    private val profileViewModel: ProfileViewModel by viewModels()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firebase Setup
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("413625786015-q9cqiafc4ggu27uq2uenmudu14lpj3fk.apps.googleusercontent.com")
            .requestEmail()
            .build()

        val googleAuthUiClient = GoogleAuthUiClient(
            context = this,
            auth = auth,
            gso = gso,
            firestore = firestore
        )

        setContent {
            val auth = FirebaseAuth.getInstance()
            val prefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val role = prefs.getString("role", null)
            var startDestination = "splash"

            startDestination = if (auth.currentUser != null && role != null) {
                when (role.lowercase()) {
                    "consumer" -> "ConsumerHomeScreen"
                    "producer" -> "ProducerHomeScreen"
                    else -> "LoginScreen"
                }
            } else {
                "LoginScreen"
            }
            GreenBasketTheme(isProducer = false) {
                val navController = rememberNavController()

                // Store navController globally
                NavControllerHolder.navController = navController

                SetupNavGraph(
                    navController = navController,
                    googleAuthUiClient = googleAuthUiClient,
                    startDestination = startDestination
                )
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String) {
        Toast.makeText(this, "✅ Payment Successful!\nID: $razorpayPaymentID", Toast.LENGTH_LONG).show()
        Log.d("Razorpay", "Payment successful: $razorpayPaymentID")

        // Call ViewModel to verify and record transaction
        profileViewModel.handlePaymentSuccess(razorpayPaymentID, this)

        // Navigate to OrderStatusScreen
        NavControllerHolder.navController?.navigate("order_status_screen?orderId={orderId}&isSuccess={isSuccess}&deliveryOption={home}")
            ?: Log.e("MainActivity", "NavController is null. Can't navigate to OrderStatusScreen")
    }


    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "❌ Payment Failed: $response", Toast.LENGTH_LONG).show()
        Log.e("Razorpay", "Payment failed with code $code. Response: $response")

        profileViewModel.handlePaymentError(code, response ?: "Unknown Error", this)

        // Navigate back to PaymentScreen (adjust the route name if needed)
        NavControllerHolder.navController?.navigate("PaymentScreen") {
            popUpTo("payment_screen?orderId={orderId}") { inclusive = true }
        } ?: Log.e("MainActivity", "NavController is null. Can't navigate to PaymentScreen")
    }

    // 🔐 Helper to get logged-in user role
    private fun getUserRole(context: Context): String? {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return prefs.getString("role", null)
    }
}


