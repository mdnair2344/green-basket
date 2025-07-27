//PendingOrdersScreenForProducer
package com.igdtuw.greenbasket.ui.producer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimary // Replace with your actual dark green color if different
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingOrdersScreenForProducer(
    navController: NavController,
    viewModel: ProducerOrdersViewModel = hiltViewModel()
) {
    val orders by viewModel.ordersToApprove.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.listenToPendingOrdersRealtime()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Approve Orders", color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        if (orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No pending orders.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(8.dp)
            ) {
                items(orders) { order ->
                    Card(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Consumer ID: ${order.consumerId}", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))

                            order.items.forEach { item ->
                                Text("${item.name}: ${item.quantity} × ₹${item.pricePerUnit} = ₹${item.quantity * item.pricePerUnit}")
                            }

                            Spacer(Modifier.height(8.dp))
                            Text("Total: ₹${order.totalAmount}", fontWeight = FontWeight.Bold)

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.updateOrderStatus(
                                            orderId = order.orderId,
                                            newStatus = "approved"
                                        ) {
                                            // snapshot listener handles UI update
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1B5E20), // Dark green
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Accept")
                                }

                                Button(
                                    onClick = {
                                        viewModel.updateOrderStatus(
                                            orderId = order.orderId,
                                            newStatus = "rejected"
                                        ) {
                                            // snapshot listener handles UI update
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFC62828), // Dark red
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("Reject")
                                }
                            }

                        }
                    }
                }
            }
        }
    }
}
