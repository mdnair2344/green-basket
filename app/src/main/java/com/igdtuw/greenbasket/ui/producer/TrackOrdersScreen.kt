//TrackOrdersScreen
package com.igdtuw.greenbasket.ui.producer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import kotlinx.coroutines.delay
import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

val LightGreen = Color(0xFFA5D6A7)
val DarkGreen = Color(0xFF1B5E20)
val White = Color.White

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOrdersScreen(
    viewModel: TrackOrdersViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val pendingOrders by viewModel.pendingOrders.collectAsState()
    val completedOrders by viewModel.completedOrders.collectAsState()
    val context = LocalContext.current



    // Trigger fetching orders when screen is first shown
    LaunchedEffect(Unit) {
        viewModel.fetchOrdersForProducer()
        // Optional: refresh every 10 seconds
        while (true) {
            delay(10_000)
            viewModel.fetchOrdersForProducer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Track Orders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.generateCompletedOrdersPDF(context, completedOrders, pendingOrders)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Orders PDF",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGreen)
            )
        },
        containerColor = White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Tab buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightGreen.copy(alpha = 0.3f)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TabButton(
                    text = "Pending Orders",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "Completed",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            // Order list view
            when (selectedTab) {
                0 -> OrderList(
                    orders = pendingOrders,
                    onMarkDelivered = { orderId ->
                        viewModel.markOrderAsDelivered(orderId)
                    }
                )
                1 -> OrderList(orders = completedOrders)
            }
        }
    }
}

@Composable
fun TabButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor = if (selected) DarkGreen else Color.Transparent
    val contentColor = if (selected) White else DarkGreen

    TextButton(
        onClick = onClick,
        modifier = modifier
            .padding(4.dp)
            .background(backgroundColor, shape = MaterialTheme.shapes.small)
    ) {
        Text(text = text, color = contentColor, fontWeight = FontWeight.SemiBold)
    }
}


@Composable
fun OrderList(
    orders: List<DisplayOrder>,
    onMarkDelivered: ((String) -> Unit)? = null
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No orders available", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(orders) { order ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Order ID: ${order.orderId}", fontWeight = FontWeight.Bold, color = DarkGreen)
                        Text("Name: ${order.consumerName}", color = Color.Black)
                        Text("Phone: ${order.consumerPhone}", color = Color.Black)
                        Text("Address: ${order.consumerAddress}", color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Items Ordered:", fontWeight = FontWeight.SemiBold, color = DarkGreen)
                        order.items?.forEach { (item, qty) ->
                            Text("- $item x$qty", color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Delivery: ${order.deliveryDateTime}", color = Color.Black)
                        Text("Status: ${order.status}", color = Color.Black, fontWeight = FontWeight.Medium)

                        // Add Delivery Completed button if applicable
                        if (order.status == "payment_successful" && onMarkDelivered != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onMarkDelivered(order.orderId) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen)
                            ) {
                                Text("Delivery Completed", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}


