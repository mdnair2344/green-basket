//SustainabilityViewModel
package com.igdtuw.greenbasket.ui.producer

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

data class CropForSustainability(
    val name: String
)

@HiltViewModel
class SustainabilityViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val db = Firebase.firestore

    var crops = mutableStateListOf<CropForSustainability>()
        private set

    var completedOrders = mutableStateOf(0)
        private set

    val validCertificateCount = mutableStateOf(0)


    var totalRevenue = mutableStateOf(0.0)
        private set

    var rating = mutableStateOf(0f)
        private set

    var reviewCount = mutableStateOf(0)
        private set

    var sustainableScore = mutableStateOf(0f)
        private set

    init {
        fetchData()
    }

    private fun fetchData() {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            // 1. Fetch crops (no need to check isOrganic)
            db.collection("producers").document(uid).collection("crops")
                .get()
                .addOnSuccessListener { result ->
                    crops.clear()
                    for (doc in result) {
                        val name = doc.getString("name") ?: continue
                        crops.add(CropForSustainability(name)) // Cleaned: no isOrganic check
                    }
                    computeScore()
                }

            // 2. Fetch completed orders
            db.collection("orders")
                .whereEqualTo("producerId", uid)
                .whereIn("status", listOf("payment_successful", "approved", "delivered"))
                .get()
                .addOnSuccessListener { result ->
                    completedOrders.value = result.size()
                    totalRevenue.value = result.sumOf { it.getDouble("totalAmount") ?: 0.0 }
                    computeScore()
                }

            // 3. Fetch reviews (matched by producerId)
            db.collection("reviews")
                .whereEqualTo("producerId", uid)
                .get()
                .addOnSuccessListener { result ->
                    reviewCount.value = result.size()
                    if (!result.isEmpty) {
                        val totalRating = result.documents.sumOf { doc ->
                            doc.getDouble("rating") ?: 0.0
                        }
                        rating.value = (totalRating / result.size()).toFloat()
                    }
                    computeScore()
                }

            db.collection("producers").document(uid).collection("certificates")
                .get()
                .addOnSuccessListener { result ->
                    val today = LocalDate.now()
                    var validCount = 0

                    for (doc in result) {
                        val expiryDateString = doc.getString("expiryDate") ?: continue

                        try {
                            val expiryDate = LocalDate.parse(expiryDateString) // assuming format is "yyyy-MM-dd"
                            if (!expiryDate.isBefore(today)) {
                                validCount++
                            }
                        } catch (e: Exception) {
                            Log.e("CertificateCheck", "Invalid date format: $expiryDateString", e)
                        }
                    }

                    validCertificateCount.value = validCount
                    computeScore()
                }

        }
    }


    private fun computeScore() {
        val cropCount = crops.size.coerceAtLeast(1)
        val orderPerCrop = completedOrders.value.toFloat() / cropCount
        val revenuePerCrop = totalRevenue.value.toFloat() / cropCount

        val efficiency = (0.5f * (orderPerCrop / 10f).coerceAtMost(1f)) +
                (0.5f * (revenuePerCrop / 500f).coerceAtMost(1f))

        val rep = (0.7f * (rating.value / 5f)) +
                (0.3f * (reviewCount.value / 50f).coerceAtMost(1f))

        val cropBonus = (cropCount / 10f).coerceAtMost(1f)

        val certificateBonus = (validCertificateCount.value / 5f).coerceAtMost(1f) // Max 5 certificates = full bonus

        sustainableScore.value = (efficiency * 45f) +
                (rep * 35f) +
                (cropBonus * 10f) +
                (certificateBonus * 10f)
    }

}
