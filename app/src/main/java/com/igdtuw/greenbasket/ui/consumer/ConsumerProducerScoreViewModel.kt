//ConsumerProducerScoreViewModel
package com.igdtuw.greenbasket.ui.consumer

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import android.util.Log


@HiltViewModel
class ConsumerProducerScoreViewModel @Inject constructor() : ViewModel() {

    private val db = Firebase.firestore

    val score = mutableStateOf<Float?>(null)

    fun fetchSustainabilityScore(producerId: String) {
        val crops = mutableListOf<String>()
        var completedOrders = 0
        var totalRevenue = 0.0
        var rating = 0f
        var reviewCount = 0
        var validCertificates = 0

        val today = LocalDate.now()

        viewModelScope.launch {
            try {
                // 1. Crops
                val cropSnap = db.collection("producers").document(producerId)
                    .collection("crops").get().await()
                crops.addAll(cropSnap.documents.mapNotNull { it.getString("name") })

                // 2. Orders
                val ordersSnap = db.collection("orders")
                    .whereEqualTo("producerId", producerId)
                    .whereIn("status", listOf("approved", "payment_successful", "delivered"))
                    .get().await()
                completedOrders = ordersSnap.size()
                totalRevenue = ordersSnap.sumOf { it.getDouble("totalAmount") ?: 0.0 }

                // 3. Reviews
                val reviewSnap = db.collection("reviews")
                    .whereEqualTo("producerId", producerId).get().await()
                reviewCount = reviewSnap.size()
                val totalRating = reviewSnap.sumOf { it.getDouble("rating") ?: 0.0 }
                rating = if (reviewCount > 0) (totalRating / reviewCount).toFloat() else 0f

                // 4. Certificates
                val certSnap = db.collection("producers").document(producerId)
                    .collection("certificates").get().await()
                validCertificates = certSnap.count {
                    val expiryStr = it.getString("expiryDate") ?: return@count false
                    try {
                        val expiry = LocalDate.parse(expiryStr, DateTimeFormatter.ISO_DATE)
                        expiry.isAfter(today)
                    } catch (_: Exception) {
                        false
                    }
                }

                // ✅ Compute score (same logic)
                val cropCount = crops.size.coerceAtLeast(1)
                val orderPerCrop = completedOrders.toFloat() / cropCount
                val revenuePerCrop = totalRevenue.toFloat() / cropCount

                val efficiency = (0.5f * (orderPerCrop / 10f).coerceAtMost(1f)) +
                        (0.5f * (revenuePerCrop / 500f).coerceAtMost(1f))

                val rep = (0.7f * (rating / 5f)) +
                        (0.3f * (reviewCount / 50f).coerceAtMost(1f))

                val cropBonus = (cropCount / 10f).coerceAtMost(1f)
                val certBonus = (validCertificates / 5f).coerceAtMost(1f)

                val finalScore = (efficiency * 45f) +
                        (rep * 35f) +
                        (cropBonus * 10f) +
                        (certBonus * 10f)

                score.value = finalScore

            } catch (e: Exception) {
                score.value = null
                Log.e("ScoreFetch", "Error: ${e.localizedMessage}")
            }
        }
    }
}
