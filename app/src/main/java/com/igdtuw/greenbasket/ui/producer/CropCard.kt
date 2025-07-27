//CropCard
package com.igdtuw.greenbasket.ui.producer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.Dp

val growthStages = listOf("Sown", "Sprouted", "Vegetative", "Flowering", "Harvesting")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CropCard(crop: CalendarCrop, producerId: String, cropId: String) {
    var currentStage by remember { mutableStateOf(crop.growthStage) }
    var showCelebrate by remember { mutableStateOf(false) }
    var lastUpdateTimestamp by remember { mutableStateOf(crop.lastUpdateTimestamp) }
    var showTips by remember { mutableStateOf(false) }

    // Check if "Harvesting" and trigger celebration on first render if already harvested
    LaunchedEffect(Unit) {
        if (currentStage == "Harvesting" && !showCelebrate) {
            showCelebrate = true
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Crop Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Crop: ${crop.name}", style = MaterialTheme.typography.headlineSmall.copy(color = Color(0xFF1B5E20)), fontWeight = FontWeight.Bold)
                IconButton(onClick = { showTips = !showTips }) {
                    Icon(Icons.Default.Info, contentDescription = "Show Tips", tint = Color(0xFF4CAF50))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Animated Tips Section
            AnimatedVisibility(
                visible = showTips,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCEDC8)),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Tips for ${currentStage} Stage:", style = MaterialTheme.typography.titleSmall.copy(color = Color(0xFF388E3C)))
                        Text(getStageTips(currentStage), fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
            }

            Text("Current Stage: $currentStage", style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(12.dp))

            // Calling the timeline with an adjustment value
            GrowthStageTimeline(currentStage, verticalLineAdjust = 2.dp) // Adjust this value (e.g., 2.dp, 4.dp)
            Spacer(modifier = Modifier.height(12.dp))

            EstimatedTimeline(currentStage)
            Spacer(modifier = Modifier.height(8.dp))

            // Last Updated Timestamp
            Text(
                text = "Last Updated: ${formatTimestamp(lastUpdateTimestamp)}",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Only "Change Stage" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StageDropdownWithConfirmation(currentStage) { selectedStage ->
                    currentStage = selectedStage
                    val newTimestamp = System.currentTimeMillis()
                    lastUpdateTimestamp = newTimestamp
                    updateCropStageInFirestore(producerId, cropId, selectedStage, newTimestamp)

                    if (selectedStage == "Harvesting") {
                        showCelebrate = true
                    } else {
                        showCelebrate = false
                    }
                }
            }

            // Celebration Message
            AnimatedVisibility(
                visible = showCelebrate,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🎉 Congratulations! Crop harvested! 🎉",
                        color = Color(0xFF1B5E20),
                        fontSize = 15.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text("Time to celebrate your hard work!", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun GrowthStageTimeline(currentStage: String, verticalLineAdjust: Dp = 0.dp) { // Added verticalLineAdjust
    val currentIndex = growthStages.indexOf(currentStage)
    val dotSize = 24.dp // Diameter of the circle
    val lineHeight = 2.dp // Thickness of the line

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Use a single Canvas to draw both the grey and green lines, and then layer the circles
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(dotSize) // Canvas height to align with dot centers
                // Adjusting the y-offset to shift the line upwards
                .offset(y = dotSize/8) // Modified offset
        ) {
            val progressLineHeightPx = lineHeight.toPx()
            val yPosOnLine = size.height / 2f // Vertical center of the drawing area for the line

            val totalSegments = growthStages.size - 1
            val segmentWidthPx = size.width / totalSegments.toFloat()

            // Calculate exact x positions for each dot's center
            val dotCentersX = List(growthStages.size) { i ->
                (i * segmentWidthPx) + (dotSize.toPx() / 2) // Start from left edge, move by segment, then half dot size
            }

            // Draw the full grey line first, connecting centers of all dots
            if (totalSegments > 0) { // Ensure there are segments to draw
                drawLine(
                    color = Color.LightGray,
                    start = Offset(dotCentersX.first(), yPosOnLine),
                    end = Offset(dotCentersX.last(), yPosOnLine),
                    strokeWidth = progressLineHeightPx
                )
            }

            // Draw the green progress line over the grey line for completed segments
            if (currentIndex > 0) {
                drawLine(
                    color = Color(0xFF1B5E20), // Active line color
                    start = Offset(dotCentersX.first(), yPosOnLine),
                    end = Offset(dotCentersX[currentIndex], yPosOnLine), // Extends to current dot's center
                    strokeWidth = progressLineHeightPx
                )
            }
        }

        // Now layer the circles and text on top of the Canvas
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            growthStages.forEachIndexed { index, stage ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val dotColor by animateColorAsState(
                        targetValue = if (index <= currentIndex) Color(0xFF1B5E20) else Color.LightGray,
                        label = "DotColorAnimation"
                    )

                    // The Box for the circle
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .background(color = dotColor, shape = CircleShape)
                            .border(1.dp, Color.White, CircleShape), // White border for pop
                        contentAlignment = Alignment.Center
                    ) {
                        if (index < currentIndex) { // Only for completed stages
                            Box(
                                modifier = Modifier
                                    .size(dotSize / 2) // Half the size of the outer dot
                                    .background(Color.White, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stage, fontSize = 10.sp, color = Color.Black)
                }
            }
        }
    }
}


@Composable
fun StageDropdownWithConfirmation(currentStage: String, onStageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var selectedStage by remember { mutableStateOf(currentStage) }
    var showDialog by remember { mutableStateOf(false) }

    Column {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20), contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text("Change Stage")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            growthStages.forEach { stage ->
                DropdownMenuItem(text = { Text(stage) }, onClick = {
                    selectedStage = stage
                    expanded = false
                    showDialog = true
                })
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Confirm Stage Change") },
                text = { Text("Are you sure you want to change the stage to $selectedStage?") },
                confirmButton = {
                    TextButton(onClick = {
                        onStageSelected(selectedStage)
                        showDialog = false
                    }) {
                        Text("Confirm", color = Color(0xFF1B5E20))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun EstimatedTimeline(currentStage: String) {
    val timelines = mapOf(
        "Sown" to "Expected to sprout in 7 days",
        "Sprouted" to "Vegetative stage in 14 days",
        "Vegetative" to "Flowering in 20 days",
        "Flowering" to "Ready to harvest in 30 days",
        "Harvesting" to "Harvest complete! No further estimates."
    )

    Text(text = timelines[currentStage] ?: "", fontSize = 12.sp, color = Color.DarkGray)
}

@Composable
fun getStageTips(stage: String): String {
    return when (stage) {
        "Sown" -> "Ensure proper watering and light exposure. Monitor for initial sprouts daily!"
        "Sprouted" -> "Provide adequate sunlight and consider initial nutrient feeding if needed. Protect from pests."
        "Vegetative" -> "Focus on strong leaf growth. Maintain consistent watering and consider pruning for better yield."
        "Flowering" -> "Increase phosphorus and potassium for healthy bloom development. Be gentle with handling flowers."
        "Harvesting" -> "Harvest at the optimal time for best quality. Clean tools and prepare for the next cycle!"
        else -> "No specific tips available for this stage."
    }
}

fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "N/A"
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun updateCropStageInFirestore(producerId: String, cropId: String, newStage: String, timestamp: Long) {
    val db = FirebaseFirestore.getInstance()
    db.collection("producers")
        .document(producerId)
        .collection("crops")
        .document(cropId)
        .update(
            mapOf(
                "growthStage" to newStage,
                "lastUpdateTimestamp" to timestamp
            )
        )
        .addOnSuccessListener { println("Crop stage and timestamp updated successfully!") }
        .addOnFailureListener { e -> println("Error updating crop stage: $e") }
}