//WaitingForApproval
package com.igdtuw.greenbasket.ui.consumer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight

@Composable
fun WaitingForApprovalScreen(
    navController: NavController,
    orderId: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val userId = Firebase.auth.currentUser?.uid ?: return
    var orderStatus by remember { mutableStateOf("waiting_for_approval") }

    // Real-time listener to order status in consumers/{uid}/orders/{orderId}
    DisposableEffect(orderId) {
        val orderRef = firestore
            .collection("orders")
            .document(orderId)

        val listener = orderRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ApprovalScreen", "Listen failed", error)
                return@addSnapshotListener
            }
            val status = snapshot?.getString("status")
            if (status != null) {
                orderStatus = status
                Log.d("ApprovalScreen", "Order status: $status")
            }
        }

        onDispose { listener.remove() }
    }

    // Navigate based on status
    LaunchedEffect(orderStatus) {
        when (orderStatus) {
            "approved" -> {
                delay(1000)
                navController.navigate("select_address")//$orderId
            }
            "rejected" -> {
                delay(2000)
                navController.popBackStack("cart", inclusive = false)
            }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (orderStatus) {
                "waiting_for_approval" -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Waiting for Producer's Approval...",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                "rejected" -> {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "Rejected",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Your order was rejected by the producer.",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                }

                else -> {
                    // Optional: display other status
                }
            }
        }
    }
}
