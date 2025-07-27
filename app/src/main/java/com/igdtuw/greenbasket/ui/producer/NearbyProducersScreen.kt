//NearbyProducersScreen
package com.igdtuw.greenbasket.ui.producer


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme

data class Producer(
    val name: String,
    val farmName: String,
    val cropsGrown: List<String>,
    val location: String,
    val mobile: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyProducersScreen(
    producers: List<Producer>,
    onCallClick: (Producer) -> Unit = {},
    onVideoClick: (Producer) -> Unit = {},
    onChatClick: (Producer) -> Unit = {},
    onBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nearby Producers",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant
                )
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (producers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No Nearby Producers Found", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(producers) { producer ->
                    ProducerCard(
                        producer = producer,
                        onCallClick = { onCallClick(producer) },
                        onVideoClick = { onVideoClick(producer) },
                        onChatClick = { onChatClick(producer) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProducerCard(
    producer: Producer,
    onCallClick: () -> Unit,
    onVideoClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Reviewer Avatar",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = producer.name,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.Black)
                    )
                    Text("Farm: ${producer.farmName}", fontSize = 14.sp, color = Color(0xFF1B5E20))
                    Text("Crops: ${producer.cropsGrown.joinToString()}", fontSize = 14.sp, color = Color(0xFF1B5E20))
                    Text("Location: ${producer.location}", fontSize = 13.sp, color = Color.Black)
                    Text("Mobile: ${producer.mobile}", fontSize = 13.sp, color = Color.Black)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCallClick) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.Black)
                }
                IconButton(onClick = onVideoClick) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video", tint = Color.Black)
                }
                IconButton(onClick = onChatClick) {
                    Icon(Icons.Default.Message, contentDescription = "Chat", tint = Color.Black)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NearbyProducersScreenPreview() {
    GreenBasketTheme(content = {
        NearbyProducersScreen(
            producers = listOf(
                Producer(
                    name = "Karan",
                    farmName = "GreenField Farms",
                    cropsGrown = listOf("Wheat", "Potato", "Onion"),
                    location = "Rohtak",
                    mobile = "9876543210"
                ),
                Producer(
                    name = "Simran",
                    farmName = "Nature's Bloom",
                    cropsGrown = listOf("Tomatoes", "Spinach"),
                    location = "Sonipat",
                    mobile = "9812345678"
                )
            )
        )
    }, isProducer = false)
}
