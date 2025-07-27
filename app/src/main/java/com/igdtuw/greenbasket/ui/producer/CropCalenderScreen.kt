//CropCalenderScreen
package com.igdtuw.greenbasket.ui.producer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant


data class CalendarCrop(
    val id: String = "",
    val name: String = "",
    val growthStage: String = "Sown",
    val lastUpdateTimestamp: Long = 0L // New field to store timestamp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropCalendarScreen(onBack: () -> Unit = {}) {
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "mock_producer_id" // Use mock ID for preview
    var crops by remember { mutableStateOf(listOf<Pair<String, CalendarCrop>>()) }

    // Simulate fetching data for preview or fetch real data
    if (uid == "mock_producer_id") {
        LaunchedEffect(Unit) {
            // Simulate a delay for a more realistic loading feel
            kotlinx.coroutines.delay(500)
            crops = listOf(
                "crop1_id" to CalendarCrop("crop1_id", "Tomato A", "Vegetative", System.currentTimeMillis() - 86400000 * 5), // 5 days ago
                "crop2_id" to CalendarCrop("crop2_id", "Cucumber B", "Flowering", System.currentTimeMillis() - 86400000 * 2), // 2 days ago
                "crop3_id" to CalendarCrop("crop3_id", "Lettuce C", "Sown", System.currentTimeMillis()), // Just now
                "crop4_id" to CalendarCrop("crop4_id", "Bell Pepper D", "Harvesting", System.currentTimeMillis() - 86400000 * 10) // 10 days ago
            )
        }
    } else {
        LaunchedEffect(uid) {
            db.collection("producers").document(uid)
                .collection("crops").get()
                .addOnSuccessListener { result ->
                    crops = result.documents.mapNotNull {
                        val id = it.id
                        val name = it.getString("name") ?: "Unknown"
                        val growthStage = it.getString("growthStage") ?: "Sown"
                        val lastUpdateTimestamp = it.getLong("lastUpdateTimestamp") ?: 0L // Fetch timestamp
                        id to CalendarCrop(id, name, growthStage, lastUpdateTimestamp)
                    }
                }
                .addOnFailureListener { e ->
                    println("Error fetching crops: $e")
                    // Handle error, e.g., show a Toast or error message
                }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Crop Stages",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ConsumerPrimaryVariant)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White) // Ensures white background across screen
                .padding(padding)
        ) {
            if (crops.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No crops added yet. Start by adding a new crop!",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(crops.size) { index ->
                        val (cropId, crop) = crops[index]
                        CropCard(crop, uid, cropId)
                    }
                }
            }
        }
    }

}