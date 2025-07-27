//RefundsScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant // Your custom green color
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme

// 1. Data Class for Refund Items (remains the same)
data class RefundItem(
    val id: String,
    val amount: String,
    val date: String,
    val status: String,
    val orderId: String? = null
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefundsScreen(navController: NavController) {
    // 2. Dummy Data for Refunds (remains the same)
    val refunds = listOf(
        RefundItem("R001", "₹500.00", "2023-04-20", "Completed", "ORD12345"),
        RefundItem("R002", "₹120.50", "2023-04-15", "Pending", "ORD12340"),
        RefundItem("R003", "₹85.00", "2023-03-10", "Completed", "ORD12300"),
        RefundItem("R004", "₹300.00", "2023-02-28", "Cancelled", "ORD12250"),
        RefundItem("R005", "₹75.25", "2023-02-01", "Completed", "ORD12100")
    )
    val sharedViewModel: SharedViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "My Refunds", // Screen specific title
                            color = Color.White, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                },
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
                    CartWishlistActions(navController, sharedViewModel) // Pass sharedViewModel
                }
            )
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (refunds.isEmpty()) {
                Text(
                    text = "No past refunds to display.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(refunds) { refund ->
                        RefundItemCard(refund)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefundItemCard(refund: RefundItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Refund ID: ${refund.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ConsumerPrimaryVariant
                )
                Text(
                    text = refund.amount,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    // CHANGED: Use ConsumerPrimaryVariant for the amount text
                    color = ConsumerPrimaryVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Date: ${refund.date}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )
            refund.orderId?.let {
                Text(
                    text = "Order ID: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status: ${refund.status}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = when (refund.status) {
                    // CHANGED: Use greenish tones for "Completed" and "Pending" statuses
                    "Completed" -> ConsumerPrimaryVariant // Green for completed
                    "Pending" -> Color.Gray // Or a slightly lighter green/yellowish green
                    "Cancelled" -> Color.Red // Keep error as red
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun RefundsScreenPreview() {
    GreenBasketTheme(isProducer = false) {
        RefundsScreen(navController = rememberNavController())
    }
}