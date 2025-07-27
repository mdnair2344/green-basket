//LiveCCTVScreen

package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Videocam
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
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme // Assuming your theme is here
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant // Your custom green color

data class Producer(
    val producerId: String,
    val name: String,
    val description: String,
    val cctvFeedUrl: String // URL for the live CCTV feed
)

// Dummy data for producers with CCTV feeds
val dummyProducersWithCCTV = listOf(
    Producer("p1", "Farm Fresh Organics", "Organic fruits and vegetables", "https://example.com/cctv/farm1"),
    Producer("p2", "Dairy Delights Co.", "Fresh milk and dairy products", "https://example.com/cctv/dairy2"),
    Producer("p3", "Green Growers Farm", "Hydroponic leafy greens", "https://example.com/cctv/greenfarm3"),
    Producer("p4", "Healthy Harvest", "Seasonal produce direct from farm", "https://example.com/cctv/harvest4"),
    Producer("p5", "Sunrise Poultry", "Free-range eggs and poultry", "https://example.com/cctv/poultry5")
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCCTVScreen(navController: NavController) {
    val sharedViewModel: SharedViewModel = hiltViewModel()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live CCTV Feeds", color = Color.White, fontWeight = FontWeight.Bold) },
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
        }, containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_cctv_producer") }, // Navigate to add new producer screen
                containerColor = ConsumerPrimaryVariant, // Use your green variant
                shape = CircleShape,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Producer", tint = Color.White)
            }
        },
        floatingActionButtonPosition = FabPosition.End // Position FAB at bottom end
    ) { paddingValues ->
        if (dummyProducersWithCCTV.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No CCTV feeds added yet. Click '+' to add one!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dummyProducersWithCCTV) { producer ->
                    ProducerCCTVCard(producer = producer) { clickedProducer ->
                        // Navigate to the CCTV feed screen with producer ID
                        navController.navigate("cctv_feed/${clickedProducer.producerId}")
                    }
                }
            }
        }
    }
}

@Composable
fun ProducerCCTVCard(producer: Producer, onClick: (Producer) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(producer) },
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20) // Greenish tone
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = producer.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                Icons.Default.Videocam,
                contentDescription = "View Live Feed",
                tint = ConsumerPrimaryVariant, // Green icon
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LiveCCTVScreenPreview() {
    GreenBasketTheme(isProducer = false) {
        LiveCCTVScreen(navController = rememberNavController())
    }
}