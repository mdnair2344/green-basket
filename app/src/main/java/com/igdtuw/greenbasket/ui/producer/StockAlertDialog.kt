package com.igdtuw.greenbasket.ui.producer

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme

@Composable
fun StockAlertDialog(
    onReject: () -> Unit,
    onRestock: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Insufficient Stock", fontWeight = FontWeight.Bold) },
        text = { Text("You don't have enough stock to fulfill this order.") },
        confirmButton = {
            Button(onClick = onRestock, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) {
                Text("Restock", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReject, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)) {
                Text("Reject")
            }
        }
    )
}
@Preview(showBackground = true)
@Composable
fun PreviewStockAlertDialog() {
    GreenBasketTheme(content = {
        StockAlertDialog(
            onReject = {},
            onRestock = {}
        )
    }, isProducer = false)
}
