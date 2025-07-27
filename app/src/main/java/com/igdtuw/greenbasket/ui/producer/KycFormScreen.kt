package com.igdtuw.greenbasket.ui.producer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant

@Composable
fun KycFormScreen(
    onSubmit: (accountNumber: String, confirmAccountNumber: String, ifscCode: String, beneficiaryName: String) -> Unit
) {
    var accountNumber by remember { mutableStateOf("") }
    var confirmAccountNumber by remember { mutableStateOf("") }
    var ifscCode by remember { mutableStateOf("") }
    var beneficiaryName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submissionSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("Complete KYC", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = accountNumber,
            onValueChange = { accountNumber = it },
            label = { Text("Account Number") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmAccountNumber,
            onValueChange = { confirmAccountNumber = it },
            label = { Text("Re-enter Account Number") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = ifscCode,
            onValueChange = { ifscCode = it },
            label = { Text("IFSC Code") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = beneficiaryName,
            onValueChange = { beneficiaryName = it },
            label = { Text("Beneficiary Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let {
            Text(text = it, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (submissionSuccess) {
            Text("✅ KYC Submitted Successfully!", color = Color.Green)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (accountNumber != confirmAccountNumber) {
                    errorMessage = "Account numbers do not match."
                    submissionSuccess = false
                } else if (accountNumber.isBlank() || ifscCode.isBlank() || beneficiaryName.isBlank()) {
                    errorMessage = "Please fill all fields."
                    submissionSuccess = false
                } else {
                    errorMessage = null
                    onSubmit(accountNumber, confirmAccountNumber, ifscCode, beneficiaryName)
                    submissionSuccess = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConsumerPrimaryVariant, // consumer primary
                contentColor = Color.White          // white text
            )
        ) {
            Text("Submit KYC")
        }
    }
}