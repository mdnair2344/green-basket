//RevenueScreen
package com.igdtuw.greenbasket.ui.producer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.platform.LocalContext


val ProducerCardBackgrounds = listOf(
    Color(0xFFE8F5E9),
    Color(0xFFD0F8CE),
    Color(0xFFC8E6C9),
    Color(0xFFB2DFDB)
)

data class CropRevenue(
    val name: String,
    val emoji: String,
    val quantity: Int,
    val pricePerKg: Double,
    val revenueThisMonth: Double = 0.0,
    val revenueHistory: Map<String, Double> = emptyMap()
)

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RevenueScreen(
    producerId: String,
    viewModel: RevenueViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val crops = viewModel.revenueData


    LaunchedEffect(Unit) {
        viewModel.loadRevenue(producerId)
    }

    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val currentMonth = LocalDate.now().format(formatter)

    val allMonths = crops.flatMap {
        it.revenueHistory.keys + currentMonth
    }.distinct().sortedDescending()

    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var isDialogOpen by remember { mutableStateOf(false) }

    val totalThisMonth = crops.sumOf {
        if (selectedMonth == currentMonth) it.revenueThisMonth
        else it.revenueHistory[selectedMonth] ?: 0.0
    }

    val animatedRevenue by animateFloatAsState(totalThisMonth.toFloat(), label = "")

    val topPerformer = crops.maxByOrNull {
        if (selectedMonth == currentMonth) it.revenueThisMonth
        else it.revenueHistory[selectedMonth] ?: 0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Revenue",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        DropdownMenuMonthSelector(selectedMonth, allMonths) {
                            selectedMonth = it
                        }
                    }
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
                actions = {
                    IconButton(onClick = {
                        viewModel.generateRevenueReportPdf(context, selectedMonth)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download, // Replace with `Icons.Default.Download` if available
                            contentDescription = "Download PDF",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGreen)
            )
        }
        ,
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .background(Color.White)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Total Revenue: ₹${"%.2f".format(animatedRevenue)}",
                style = MaterialTheme.typography.titleLarge,
                color = ConsumerPrimaryVariant,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(crops) { crop ->
                    CropRevenueCard(crop, selectedMonth, topPerformer)
                }
            }
        }
    }

    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDialogOpen = false },
            confirmButton = {
                TextButton(onClick = { isDialogOpen = false }) {
                    Text("OK")
                }
            },
            title = { Text("Coming Soon") },
            text = { Text("Add Crop feature is under development.") }
        )
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CropRevenueCard(crop: CropRevenue, selectedMonth: String, topPerformer: CropRevenue?) {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    val revenue = if (selectedMonth == LocalDate.now().format(formatter))
        crop.revenueThisMonth
    else crop.revenueHistory[selectedMonth] ?: 0.0

    val trend = crop.revenueHistory.entries.sortedByDescending { it.key }
        .take(2).let {
            if (it.size == 2) {
                val (latest, previous) = it
                if (latest.key == selectedMonth && latest.value > previous.value) "📈"
                else if (latest.key == selectedMonth && latest.value < previous.value) "📉"
                else ""
            } else ""
        }

    val maxInHistory = crop.revenueHistory.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val contributionPercent = revenue / maxInHistory

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = ProducerCardBackgrounds.random())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${crop.emoji} ${crop.name}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                if (crop == topPerformer) {
                    Text("🏆", fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("Revenue: ₹${"%.2f".format(revenue)} $trend", style = MaterialTheme.typography.bodyLarge)
            val monthlyQuantity = (revenue / crop.pricePerKg).takeIf { it.isFinite() } ?: 0.0
            Text("Sold: ${"%.1f".format(monthlyQuantity)} kg", style = MaterialTheme.typography.bodySmall)


            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = contributionPercent.toFloat().coerceIn(0f, 1f),
                color = Color(0xFF2E7D32),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}


@Composable
fun DropdownMenuMonthSelector(current: String, months: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(current, color = Color.White)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Month", tint = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = { Text(month) },
                    onClick = {
                        onSelect(month)
                        expanded = false
                    }
                )
            }
        }
    }
}
