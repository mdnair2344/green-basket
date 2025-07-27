//ReviewsAndRatingsScreen
package com.igdtuw.greenbasket.ui.producer

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class Review(
    var id: String = "",
    val consumerId: String = "",
    val producerId: String = "",
    val cropId: String = "",
    val comment: String = "",
    val rating: Int = 0,
    val timestamp: Timestamp? = null,
    var cropName: String? = null,
    var consumerName: String? = null,
    var date: String = ""
)

class ReviewsViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews: StateFlow<List<Review>> = _reviews

    fun loadReviewsForProducer(producerId: String) {
        viewModelScope.launch {
            try {
                val reviewSnap = firestore.collection("reviews")
                    .whereEqualTo("producerId", producerId)
                    .get()
                    .await()

                val tempList = mutableListOf<Review>()

                for (doc in reviewSnap.documents) {
                    val review = doc.toObject(Review::class.java)?.copy(id = doc.id) ?: continue

                    // Format date
                    review.date = review.timestamp?.toDate()?.let {
                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
                    } ?: "N/A"

                    // Fetch crop name
                    val cropDoc = firestore.collection("producers")
                        .document(producerId)
                        .collection("crops")
                        .document(review.cropId)
                        .get()
                        .await()
                    review.cropName = cropDoc.getString("name") ?: "Unknown Crop"

                    // Fetch consumer name
                    val consumerDoc = firestore.collection("users").document(review.consumerId).get().await()
                    review.consumerName = consumerDoc.getString("name") ?: "Anonymous"

                    tempList.add(review)
                }

                _reviews.value = tempList
            } catch (e: Exception) {
                Log.e("ReviewsViewModel", "Error fetching reviews: ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsAndRatingsScreen(
    producerId: String,
    viewModel: ReviewsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit = {}
) {
    LaunchedEffect(producerId) {
        viewModel.loadReviewsForProducer(producerId)
    }

    val reviews by viewModel.reviews.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Reviews And Ratings",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGreen)
            )
        }, containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            items(reviews) { review ->
                ReviewCard(review)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = review.date,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Crop: ${review.cropName}",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "By: ${review.consumerName}",
                style = MaterialTheme.typography.labelMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            RatingBar(rating = review.rating)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"${review.comment}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
fun RatingBar(rating: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(rating) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star",
                tint = Color(0xFFFFD700)
            )
        }
        repeat(5 - rating) {
            Icon(
                imageVector = Icons.Outlined.StarBorder,
                contentDescription = "Empty Star",
                tint = Color.Gray
            )
        }
    }
}
