//ProductViewModel
package com.igdtuw.greenbasket.ui.consumer

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class ProductDetailViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _crop = MutableStateFlow<Crop?>(null)
    val crop: StateFlow<Crop?> = _crop

    private val _certificates = MutableStateFlow<List<Certificate>>(emptyList())
    val certificates: StateFlow<List<Certificate>> = _certificates

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews


    private val _producer = MutableStateFlow<ProducerDetails?>(null)
    val producer: StateFlow<ProducerDetails?> = _producer

    fun loadProducer(producerId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .document(producerId)
                    .get()
                    .await()

                val producer = snapshot.toObject(ProducerDetails::class.java)
                _producer.value = producer
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getConsumerName(consumerId: String, onResult: (String?) -> Unit) {
        firestore.collection("users") // or "consumers" if you use a different path
            .document(consumerId)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("name")
                onResult(name)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }



    fun loadCrop(producerId: String, cropId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore
                    .collection("producers")
                    .document(producerId)
                    .collection("crops")
                    .document(cropId)
                    .get()
                    .await()

                val crop = snapshot.toObject(Crop::class.java)
                _crop.value = crop
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadCertificates(producerId: String, cropId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore
                    .collection("producers")
                    .document(producerId)
                    .collection("certificates")
                    .get()
                    .await()

                val certificates = snapshot.documents.mapNotNull {
                    it.toObject(Certificate::class.java)
                }
                _certificates.value = certificates
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadReviews(cropId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore
                    .collection("reviews")
                    .whereEqualTo("cropId", cropId)
                    //.orderBy("timestamp")
                    .get()
                    .await()

                val reviews = snapshot.documents.mapNotNull {
                    it.toObject(Review::class.java)
                }

                _reviews.value = reviews
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun submitReview(
        cropId: String,
        producerId: String,
        consumerId: String,
        text: String,
        rating: Int
    ) {
        viewModelScope.launch {
            try {
                val timestamp = Timestamp.now()
                val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd").format(timestamp.toDate())

                val review = Review(
                    consumerId = consumerId,
                    producerId = producerId,
                    cropId = cropId,
                    comment = text,
                    rating = rating,
                    timestamp = timestamp,
                    date = formattedDate
                )

                // Save review to top-level `reviews` collection and get document reference
                val docRef = firestore.collection("reviews").add(review).await()

                // Update the review with its generated ID
                firestore.collection("reviews").document(docRef.id)
                    .update("id", docRef.id)

                // Optionally update local object
                review.id = docRef.id

                loadReviews(cropId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private val _similarCrops = MutableStateFlow<List<Crop>>(emptyList())
    val similarCrops: StateFlow<List<Crop>> = _similarCrops





    fun loadSimilarCropsAcrossProducers(currentCropId: String, category: String) {
        viewModelScope.launch {
            try {
                Log.d("SimilarDebug", "Starting collectionGroup query for crops in category: $category excluding ID: $currentCropId")

                val cropsSnapshot = firestore.collectionGroup("crops")
                    .whereEqualTo("category", category)
                    .get()
                    .await()

                val allMatchingCrops = cropsSnapshot.documents.mapNotNull { doc ->
                    val crop = doc.toObject(Crop::class.java)
                    crop?.apply {
                        id = doc.id
                    }
                }.filter { it.id != currentCropId }

                Log.d("SimilarDebug", "Found ${allMatchingCrops.size} similar crops across all producers.")

                _similarCrops.value = allMatchingCrops
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SimilarDebug", "Error loading similar crops: ${e.message}", e)
            }
        }
    }

}