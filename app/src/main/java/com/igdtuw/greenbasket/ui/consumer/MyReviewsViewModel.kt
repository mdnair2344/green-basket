//MyReviewsViewModel
package com.igdtuw.greenbasket.ui.consumer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.update

@HiltViewModel
class MyReviewsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _myReviews = MutableStateFlow<List<ReviewDisplay>>(emptyList())
    val myReviews: StateFlow<List<ReviewDisplay>> = _myReviews






    fun loadMyReviews(consumerId: String) {
        viewModelScope.launch {
            try {
                val reviewSnapshot = firestore.collection("reviews")
                    .whereEqualTo("consumerId", consumerId)
                    .get()
                    .await()

                val enrichedReviews = mutableListOf<ReviewDisplay>()

                for (doc in reviewSnapshot.documents) {
                    val data = doc.data ?: continue
                    val cropId = data["cropId"] as? String ?: continue
                    val producerId = data["producerId"] as? String ?: continue

                    val review = Review(
                        consumerId = consumerId,
                        producerId = producerId,
                        cropId = cropId,
                        comment = data["comment"] as? String ?: "",
                        rating = (data["rating"] as? Long)?.toInt() ?: 0,
                        timestamp = data["timestamp"] as? Timestamp
                    )

                    // Fetch crop details
                    val cropDoc = firestore.collection("producers").document(producerId)
                        .collection("crops").document(cropId)
                        .get().await()
                    val cropName = cropDoc.getString("name") ?: "Unknown Crop"
                    val imageUrl = cropDoc.getString("imageUri") ?: ""

                    // Fetch producer name
                    val producerDoc = firestore.collection("producers").document(producerId)
                        .get().await()
                    val producerName = producerDoc.getString("name") ?: "Unknown Producer"

                    val formattedDate = review.timestamp?.toDate()?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                    } ?: "N/A"

                    val reviewDisplay = ReviewDisplay(
                        id = doc.id,
                        productId = cropId,
                        productName = cropName,
                        productImageUrl = imageUrl,
                        producerName = producerName,
                        rating = review.rating,
                        comment = review.comment,
                        date = formattedDate
                    )

                    enrichedReviews.add(reviewDisplay)
                }

                _myReviews.value = enrichedReviews
            } catch (e: Exception) {
                _myReviews.value = emptyList()
            }
        }
    }

}






