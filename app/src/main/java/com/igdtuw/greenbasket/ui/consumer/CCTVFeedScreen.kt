//CCTVFeedScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme // Assuming your theme is here

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CCTVFeedScreen(navController: NavController, producerId: String?) {
    // Find the producer details using the passed producerId
    val producer = dummyProducersWithCCTV.find { it.producerId == producerId }
    val sharedViewModel: SharedViewModel = hiltViewModel()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = producer?.name ?: "Live Feed",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel) // Pass sharedViewModel
                }
            )
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (producer != null) {
                Text(
                    text = "Live Feed from ${producer.name}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Placeholder for the live video player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f) // Common video aspect ratio
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // In a real app, you'd integrate a video player library here (e.g., ExoPlayer)
                    // using AndroidView or similar.
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Video Placeholder",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "CCTV Feed Placeholder\nURL: ${producer.cctvFeedUrl}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 80.dp) // Offset text to not overlap play icon
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Description: ${producer.description}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            } else {
                Text(
                    text = "Producer not found.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showSystemUi = true, showBackground = true)
@Composable
fun CCTVFeedScreenPreview() {
    GreenBasketTheme(isProducer = false) {
        // Preview with a dummy producer ID
        CCTVFeedScreen(navController = rememberNavController(), producerId = "p1")
    }
}