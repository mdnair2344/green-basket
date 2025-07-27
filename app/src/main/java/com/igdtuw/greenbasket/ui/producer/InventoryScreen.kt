//InventoryScreen
package com.igdtuw.greenbasket.ui.producer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

data class Crop1(
    val name: String = "",
    val variety: String = "",
    var quantity: Int = 0,
    val pricePerKg: Double = 0.0,
    val cropId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val producerId = auth.currentUser?.uid ?: return

    val crops = remember { mutableStateListOf<Crop1>() }
    var selectedCrop by remember { mutableStateOf<Crop1?>(null) }
    var showReduceDialog by remember { mutableStateOf(false) }
    var showSalesDialog by remember { mutableStateOf(false) }

    val darkGreen = Color(0xFF1B5E20)

    // Real-time fetch crops
    LaunchedEffect(Unit) {
        db.collection("producers")
            .document(producerId)
            .collection("crops")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("InventoryScreen", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    crops.clear()
                    for (doc in snapshot.documents) {
                        val crop = doc.toObject(Crop1::class.java)?.copy(cropId = doc.id)
                        crop?.let { crops.add(it) }
                    }
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Your Inventory",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = darkGreen
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .background(Color.White)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(crops) { crop ->
                    CropItem(
                        crop = crop,
                        onRestock = { navController.navigate("RestockScreen") },
                        onReduceStock = {
                            selectedCrop = crop
                            showReduceDialog = true
                        },
                        onShowSales = {
                            selectedCrop = crop
                            showSalesDialog = true
                        }
                    )
                }
            }
        }

        if (showReduceDialog && selectedCrop != null) {
            ReduceStockDialog(
                crop = selectedCrop!!,
                onDismiss = { showReduceDialog = false },
                onReduce = { newQuantity ->
                    val updated = selectedCrop!!.copy(quantity = newQuantity.coerceAtLeast(0))
                    db.collection("producers")
                        .document(producerId)
                        .collection("crops")
                        .document(updated.cropId)
                        .update("quantity", updated.quantity)
                    showReduceDialog = false
                }
            )
        }

        if (showSalesDialog && selectedCrop != null) {
            SalesDialog(
                crop = selectedCrop!!,
                onDismiss = { showSalesDialog = false }
            )
        }
    }
}

@Composable
fun CropItem(
    crop: Crop1,
    onRestock: () -> Unit,
    onReduceStock: () -> Unit,
    onShowSales: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = crop.name, style = MaterialTheme.typography.titleLarge, color = ConsumerPrimaryVariant)
                Text(text = "Variety: ${crop.variety}", color = Color.Black)
                Text(text = "Quantity: ${crop.quantity} kg", color = Color.Black)
                Text(text = "Price: ₹${crop.pricePerKg}/kg", color = Color.Black)
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Restock") },
                        onClick = {
                            expanded = false
                            onRestock()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Reduce Stock") },
                        onClick = {
                            expanded = false
                            onReduceStock()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Last Month's Sales") },
                        onClick = {
                            expanded = false
                            onShowSales()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReduceStockDialog(
    crop: Crop1,
    onDismiss: () -> Unit,
    onReduce: (Int) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val darkGreen = Color(0xFF1B5E20)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Update Stock for ${crop.name}", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Set new quantity") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = darkGreen,
                        cursorColor = darkGreen,
                        focusedLabelColor = darkGreen
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = darkGreen)
                    }
                    Button(
                        onClick = {
                            val quantity = input.toIntOrNull()
                            if (quantity != null) onReduce(quantity)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = darkGreen)
                    ) {
                        Text("Update", color = Color.White)
                    }
                }
            }
        }
    }
}


fun fetchActualSalesForCrop(
    producerId: String,
    cropName: String,
    onResult: (Int) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val today = LocalDate.now()
    val thirtyDaysAgo = today.minusDays(30)

    db.collection("orders")
        .whereEqualTo("producerId", producerId)
        .whereEqualTo("status", "delivered")
        .get()
        .addOnSuccessListener { snapshot ->
            var totalQuantity = 0

            for (doc in snapshot.documents) {
                val orderDateAny = doc.get("orderDate") ?: continue
                val orderDate: LocalDate = when (orderDateAny) {
                    is String -> try {
                        LocalDate.parse(orderDateAny) // Handles "2025-06-30"
                    } catch (e: Exception) {
                        continue
                    }
                    is Long -> try {
                        Instant.ofEpochMilli(orderDateAny)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    } catch (e: Exception) {
                        continue
                    }
                    else -> continue
                }

                if (orderDate.isBefore(thirtyDaysAgo)) continue

                val items = doc.get("items") as? List<Map<String, Any>> ?: continue
                for (item in items) {
                    val productName = item["productName"] as? String ?: continue
                    if (productName.trim().equals(cropName.trim(), ignoreCase = true)) {
                        val qty = (item["quantity"] as? Long)?.toInt() ?: 0
                        totalQuantity += qty
                    }
                }
            }

            onResult(totalQuantity)
        }
        .addOnFailureListener {
            onResult(0)
        }
}



@Composable
fun SalesDialog(crop: Crop1, onDismiss: () -> Unit) {
    val darkGreen = Color(0xFF1B5E20)
    var actualSales by remember { mutableStateOf<Int?>(null) }
    val producerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        fetchActualSalesForCrop(producerId, crop.name) {
            actualSales = it
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = darkGreen)
            }
        },
        title = { Text("Sales Report") },
        text = {
            if (actualSales == null) {
                CircularProgressIndicator()
            } else {
                Text("In the last 30 days, you sold $actualSales kg of ${crop.name}.")
            }
        }
    )
}



