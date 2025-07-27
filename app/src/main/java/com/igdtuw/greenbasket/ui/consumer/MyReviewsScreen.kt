package com.igdtuw.greenbasket.ui.consumer

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.igdtuw.greenbasket.R
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReviewsScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
    consumerId: String
) {
    Log.d("MyReviewsScreen", "Received consumerId = $consumerId")
    var reviews by remember { mutableStateOf<List<Review>>(emptyList()) }
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(consumerId) {
        try {
            val snapshot = firestore
                .collection("reviews")
                .whereEqualTo("consumerId", consumerId)
                .get()
                .await()

            Log.d("MyReviewsScreen", "Fetched ${snapshot.documents.size} documents")

            val reviewList = mutableListOf<Review>()

            for (doc in snapshot.documents) {
                Log.d("MyReviewsScreen", "Doc ID = ${doc.id}, Data = ${doc.data}")
                val review = doc.toObject(Review::class.java)?.copy(id = doc.id) ?: continue

                try {
                    // Correct Firestore path: producers/{producerId}/crops/{cropId}
                    val cropDoc = firestore
                        .collection("producers")
                        .document(review.producerId)
                        .collection("crops")
                        .document(review.cropId)
                        .get()
                        .await()

                    review.cropName = cropDoc.getString("name") ?: "Crop not found"
                } catch (e: Exception) {
                    Log.e("MyReviewsScreen", "Error fetching crop: ${review.cropId}", e)
                }

                try {
                    val producerDoc = firestore
                        .collection("users")
                        .document(review.producerId)
                        .get()
                        .await()

                    review.producerName = producerDoc.getString("name")
                    review.farmName = producerDoc.getString("farmName")
                } catch (e: Exception) {
                    Log.e("MyReviewsScreen", "Error fetching producer: ${review.producerId}", e)
                }

                reviewList.add(review)
            }

            reviews = reviewList
        } catch (e: Exception) {
            Log.e("MyReviewsScreen", "Error fetching reviews", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Reviews",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel)
                }
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (reviews.isEmpty()) {
            EmptyReviewsState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(reviews) { review ->
                    ReviewCard(review)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .background(color = ConsumerCardBackground1, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = review.date,
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = review.cropName ?: "Crop not found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "From ${review.farmName ?: "Unknown Farm"} by ${review.producerName ?: "Unknown Producer"}",
            fontSize = 14.sp,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "\"${review.comment}\"",
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(review.rating) {
                Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFFFC107))
            }
            repeat(5 - review.rating) {
                Icon(Icons.Default.StarBorder, contentDescription = "Empty Star", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun EmptyReviewsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_no_reviews_placeholder),
            contentDescription = "No Reviews",
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No reviews yet!",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.DarkGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "It looks like you haven't reviewed any products. Share your feedback!",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
