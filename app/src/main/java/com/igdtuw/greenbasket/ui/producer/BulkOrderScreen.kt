//BulkOrderScreen
package com.igdtuw.greenbasket.ui.producer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme


// ─── BulkOrder Data Model ───────────────────────────────────────────────────────
data class BulkOrder(
    val occasion: String,
    val quantity: String,
    val customerName: String,
    val address: String,
    val phone: String,
    val deliveryDateTime: String,
    val isCompleted: Boolean
)

// ─── BulkOrdersScreen ────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkOrdersScreen(onBack: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(0) }

    val pending = listOf(
        BulkOrder("Wedding", "50 kg", "Aman", "Sector 22", "9876543210", "10 June 2025, 10:00 AM", false),
        BulkOrder("Birthday", "30 kg", "Neha", "Sector 45", "9876509876", "12 June 2025, 09:00 AM", false)
    )

    val completed = listOf(
        BulkOrder("Anniversary", "40 kg", "Ravi", "Sector 18", "9876543200", "02 June 2025, 05:00 PM", true)
    )

    val ordersToShow = if (selectedTab == 0) pending else completed

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bulk Orders",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkGreen
                )
            )
        },
        containerColor = White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
        ) {
            // ─── Tab Row ─────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightGreen.copy(alpha = 0.3f))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BulkTabButton("Pending", selected = selectedTab == 0) { selectedTab = 0 }
                BulkTabButton("Completed", selected = selectedTab == 1) { selectedTab = 1 }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (ordersToShow.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No ${if (selectedTab == 0) "pending" else "completed"} bulk orders.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                }
            } else {
                BulkOrderList(orders = ordersToShow)
            }
        }
    }
}


// ─── BulkTabButton (Pending / Completed) ────────────────────────────────────────
@Composable
private fun BulkTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) DarkGreen else Color.Transparent
    val contentColor = if (selected) White else DarkGreen

    TextButton(
        onClick = onClick,
        modifier = Modifier
            //.weight(1f)
            .padding(horizontal = 4.dp)
            .background(bgColor, shape = RoundedCornerShape(4.dp))
    ) {
        Text(
            text = text,
            color = contentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── BulkOrderList (LazyColumn of Cards) ────────────────────────────────────────
@Composable
private fun BulkOrderList(orders: List<BulkOrder>) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        items(orders) { order ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Occasion: ${order.occasion}",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(text = "Quantity: ${order.quantity}", color = Color.Black)
                    Text(text = "Name: ${order.customerName}", color = Color.Black)
                    Text(text = "Address: ${order.address}", color = Color.Black)
                    Text(text = "Phone: ${order.phone}", color = Color.Black)
                    Text(text = "Delivery: ${order.deliveryDateTime}", color = Color.Black)

                    Spacer(modifier = Modifier.height(12.dp))

                    // ─── “Call” Icon ───────────────────────────────────────
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Customer",
                        tint = DarkGreen,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                // Launch phone dialer
                                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${order.phone}")
                                }
                                context.startActivity(dialIntent)
                            }
                    )
                }
            }
        }
    }
}

// ─── Preview ────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewBulkOrdersScreen() {
    GreenBasketTheme(content = {
        BulkOrdersScreen()
    }, isProducer = false)
}
