//HelpScreen
package com.igdtuw.greenbasket.ui.consumer

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme

// Make sure to import CartWishlistActions if it's in a different file
// import com.igdtuw.greenbasket.ui.consumer.CartWishlistActions // Uncomment if needed

// Data class to represent a help video/topic
data class HelpVideo(
    val title: String,
    val description: String,
    val videoUrl: String? = null // Optional: URL to the video
)

val helpTopics = listOf(
    HelpVideo("How to Place an Order", "A step-by-step guide on placing your first order.", "https://example.com/video1"),
    HelpVideo("Managing Your Cart", "Learn how to add, remove, and update items in your cart.", "https://example.com/video2"),
    HelpVideo("Payment Options", "Understand the various payment methods available.", "https://example.com/video3"),
    HelpVideo("Tracking Your Delivery", "How to track the live status of your order.", "https://example.com/video4"),
    HelpVideo("Contacting Support", "Ways to get in touch with our customer support team.", "https://example.com/video5"),
    HelpVideo("Returns and Refunds", "Information on our return and refund policy.", "https://example.com/video6")
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    navController: NavController? = null, // MAKE NAVCONTROLLER NULLABLE HERE
    onBackClick: () -> Unit = { navController?.popBackStack() } // Use safe call for popBackStack
) {
    val context = LocalContext.current
    val sharedViewModel: SharedViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel) // Pass sharedViewModel
                }
            )
        }, containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(helpTopics) { topic ->
                HelpTopicItem(topic = topic, context = context) { clickedTopic ->
                    Toast.makeText(context, "Playing video for: ${clickedTopic.title}", Toast.LENGTH_SHORT).show()
                    // If you navigate from here, use safe call too:
                    // navController?.navigate("video_player_screen/${clickedTopic.videoUrl}")
                    println("Video URL: ${clickedTopic.videoUrl}")
                }
            }
        }
    }
}

@Composable
fun HelpTopicItem(topic: HelpVideo, context: Context, onClick: (HelpVideo) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(topic) },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = topic.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showSystemUi = true, showBackground = true)
@Composable
fun HelpScreenPreview() {
    // Make sure to import GreenBasketTheme if it's in a different file
    // import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme // Uncomment if needed
    GreenBasketTheme(isProducer = false) {
        HelpScreen(navController = rememberNavController()) // Pass a valid navController for preview
    }
}