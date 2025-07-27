package com.igdtuw.greenbasket.ui.producer

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun DeliveryChoiceDialog(
    isPartnerAvailable: Boolean,
    onSelfDeliver: () -> Unit,
    onPartnerAssigned: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Delivery Options", fontWeight = FontWeight.Bold) },
        text = {
            if (isPartnerAvailable)
                Text("A delivery partner is available. Choose your delivery method.")
            else
                Text("No delivery partner is available. Do you want to deliver it yourself?")
        },
        confirmButton = {
            Button(
                onClick = if (isPartnerAvailable) onPartnerAssigned else onSelfDeliver,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text(
                    if (isPartnerAvailable) "Use Partner" else "Self Deliver",
                    color = Color.White
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Reject")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun DeliveryChoiceDialogWithPartnerPreview() {
    DeliveryChoiceDialog(
        isPartnerAvailable = true,
        onSelfDeliver = {},
        onPartnerAssigned = {},
        onReject = {}
    )
}

@Preview(showBackground = true)
@Composable
fun DeliveryChoiceDialogWithoutPartnerPreview() {
    DeliveryChoiceDialog(
        isPartnerAvailable = false,
        onSelfDeliver = {},
        onPartnerAssigned = {},
        onReject = {}
    )
}
