//OrderStatusScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderStatusScreen(
    navController: NavController,
    orderId: String,
    isSuccess: Boolean,
    deliveryOption: String,
    orderViewModel: OrderViewModel = viewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val appBarColor = ConsumerPrimaryVariant

    // Fetch order from Firestore via ViewModel
    LaunchedEffect(orderId) {
        orderViewModel.fetchOrderById(orderId)
    }

    val fetchedOrder by orderViewModel.fetchedOrder.collectAsState()

    Log.d("OrderStatusDebug", "OrderStatusScreen Composable loaded.")
    Log.d("OrderStatusDebug", "isSuccess: $isSuccess, deliveryOption: $deliveryOption")
    Log.d("OrderStatusDebug", "Fetched Order: $fetchedOrder")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Status", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
        }, containerColor = Color.White,
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isSuccess) appBarColor else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isSuccess) "Order Placed Successfully!" else "Order Could Not Be Placed",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isSuccess) {
                    // --- Delivery Card with Map Info ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(180.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (deliveryOption == "home")
                                    "[Map] Track your delivery here"
                                else "[Map] Route to producer farm",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- Download Bill Button ---
                    Button(
                        onClick = {
                            if (fetchedOrder != null) {
                                generateBillPdf(context, fetchedOrder!!)
                                Toast.makeText(
                                    context,
                                    "Generating bill for Order: ${fetchedOrder!!.orderId}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Order details not available for bill.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("OrderStatusDebug", "Fetched order is null for bill generation.")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = appBarColor)
                    ) {
                        Text("Download Bill", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- Continue Shopping ---
                Button(
                    onClick = {
                        navController.navigate("consumer_dashboard") {
                            popUpTo("consumer_dashboard") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = appBarColor),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Continue Shopping", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    )
}
