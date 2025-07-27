//MyOrdersScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MyOrdersScreen(
    navController: NavHostController,
    sharedViewModel: SharedViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val orders by orderViewModel.orders.collectAsState()
    val currentUserUid = Firebase.auth.currentUser?.uid

    LaunchedEffect(currentUserUid) {
        if (!currentUserUid.isNullOrEmpty()) {
            orderViewModel.fetchOrdersForUser(currentUserUid)
        } else {
            Toast.makeText(context, "Please log in to view your orders.", Toast.LENGTH_SHORT).show()
            orderViewModel.clearOrders()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Orders", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel)
                }
            )
        }, containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            when {
                currentUserUid == null -> {
                    Text(
                        "You need to be logged in to view orders.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                orders.isEmpty() -> {
                    Text(
                        "No orders found.",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders) { order ->
                            OrderCard(order)
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun OrderCard(order: Order) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    var buyerAddress by remember { mutableStateOf("") }

    var sellerName by remember { mutableStateOf("") }
    var sellerPhone by remember { mutableStateOf("") }
    var farmName by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

    LaunchedEffect(order.userId, order.producerId) {
        if (order.userId.isNotBlank()) {
            db.collection("users").document(order.userId).get()
                .addOnSuccessListener { doc ->
                    buyerName = doc.getString("name") ?: ""
                    buyerPhone = doc.getString("phone") ?: ""
                    buyerAddress = doc.getString("address") ?: ""
                }
                .addOnFailureListener {
                    Log.e("OrderCard", "Failed to fetch buyer info: ${it.message}")
                }
        } else {
            Log.w("OrderCard", "Empty userId for order ${order.orderId}")
        }

        if (order.producerId.isNotBlank()) {
            db.collection("users").document(order.producerId).get()
                .addOnSuccessListener { doc ->
                    sellerName = doc.getString("name") ?: ""
                    sellerPhone = doc.getString("phone") ?: ""
                    farmName = doc.getString("farmName") ?: ""
                }
                .addOnFailureListener {
                    Log.e("OrderCard", "Failed to fetch producer info: ${it.message}")
                }
        } else {
            Log.w("OrderCard", "Empty producerId for order ${order.orderId}")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Order ID: ${order.orderId}", style = MaterialTheme.typography.titleSmall)
            Text("Status: ${order.status}", style = MaterialTheme.typography.bodySmall)
            Text("Total: ₹${"%.2f".format(order.totalAmount)}", style = MaterialTheme.typography.bodySmall)
            Text("Payment Method: ${order.paymentMethod}", style = MaterialTheme.typography.bodySmall)
            Text("Ordered On: ${dateFormat.format(Date(order.orderDate))}", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(10.dp))

            Text("Buyer: $buyerName", style = MaterialTheme.typography.labelMedium)
            Text("Phone: $buyerPhone", style = MaterialTheme.typography.bodySmall)
            Text("Address: $buyerAddress", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(10.dp))

            Text("Seller: $sellerName", style = MaterialTheme.typography.labelMedium)
            Text("Phone: $sellerPhone", style = MaterialTheme.typography.bodySmall)
            Text("Farm: $farmName", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(10.dp))

            Text("Items:", style = MaterialTheme.typography.labelMedium)
            order.items.forEach {
                Text("• ${it.productName} x ${it.quantity} @ ₹${it.unitPrice}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { generateBillPdf(context, order) },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20), // Dark green
                    contentColor = Color.White
                )
            ) {
                Text("Download Bill")
            }
        }
    }
}
