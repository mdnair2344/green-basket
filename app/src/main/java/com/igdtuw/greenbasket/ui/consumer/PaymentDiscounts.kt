//PaymentDiscounts.kt
package com.igdtuw.greenbasket.ui.consumer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// Add this data class to your project, possibly in a `data` package or similar.

@Composable
fun PaymentDiscounts(
    producerId: String,
    orderCropNames: List<String>,
    onOffersApplied: (List<DiscountOffer>) -> Unit
) {
    println("PaymentDiscounts Composable is being composed/recomposed.")

    var manualCode by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val selectedOffer = remember { mutableStateOf<DiscountOffer?>(null) }


    var allOffers by remember { mutableStateOf<List<DiscountOffer>>(emptyList()) }
    var applicableOfferIds by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(producerId, orderCropNames) {
        allOffers = fetchDiscountOffers(producerId)
        applicableOfferIds = filterApplicableOffers(allOffers, orderCropNames).map { it.id }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1)

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Apply Discount Code", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it.uppercase() },
                    placeholder = { Text("Enter code") },
                    modifier = Modifier.weight(1f),
                    isError = errorText != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val enteredCode = manualCode.trim().uppercase()
                        val offer = allOffers.find { it.promoCode?.uppercase() == enteredCode }

                        when {
                            enteredCode.isBlank() -> errorText = "Please enter a code."
                            offer == null -> errorText = "Code not found."
                            !applicableOfferIds.contains(offer.id) -> errorText = "Code not applicable."
                            selectedOffer.value?.id == offer.id -> errorText = "Code already applied."
                            else -> {
                                selectedOffer.value = offer
                                onOffersApplied(listOf(offer)) // Only one offer sent
                                manualCode = ""
                                errorText = null
                            }
                        }

                    },
                    enabled = manualCode.isNotBlank()
                ) {
                    Text("Apply")
                }
            }

            errorText?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Text("Available Offers", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (allOffers.isEmpty()) {
                Text("No offers available at the moment.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    allOffers.forEach { offer ->
                        val isChecked = selectedOffer.value?.id == offer.id
                        val isApplicable = applicableOfferIds.contains(offer.id)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isApplicable) {
                                    selectedOffer.value = if (isChecked) null else offer
                                    onOffersApplied(listOfNotNull(selectedOffer.value))
                                }

                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { isSelected ->
                                    if (isApplicable) {
                                        selectedOffer.value = if (isSelected) offer else null
                                        onOffersApplied(listOfNotNull(selectedOffer.value))
                                    }
                                },
                                enabled = isApplicable
                            )

                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(offer.title, style = MaterialTheme.typography.bodyLarge)
                                if (!isApplicable) {
                                    Text(
                                        "Not applicable",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// 🧠 Fetch offers from Firestore
// This function remains the same as your provided code, it's correct.
suspend fun fetchDiscountOffers(producerId: String): List<DiscountOffer> {
    return try {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("producers")
            .document(producerId)
            .collection("discount_offers")
            .get()
            .await()

        val offers = snapshot.documents.mapNotNull { doc ->
            val offer = doc.toObject<DiscountOffer>()?.copy(id = doc.id) // Ensure ID is set from doc.id
            println("Fetched from Firestore: id=${offer?.id}, code=${offer?.promoCode}, title=${offer?.title}")
            offer
        }

        println("Total offers fetched: ${offers.size}")
        offers
    } catch (e: Exception) {
        println("Error while fetching offers: ${e.message}")
        emptyList()
    }
}

// ✅ Check validity and crop applicability
// This function remains the same as your provided code, it's correct.
fun filterApplicableOffers(
    offers: List<DiscountOffer>,
    cropsInOrder: List<String>
): List<DiscountOffer> {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    return offers.filter { offer ->
        val notExpired = offer.validTill >= today
        // Ensure promoCode is not null if checking for a specific code usage in UI
        val cropMatch = offer.appliesToAllCrops || offer.cropNames.any { it in cropsInOrder }
        notExpired && cropMatch
    }
}