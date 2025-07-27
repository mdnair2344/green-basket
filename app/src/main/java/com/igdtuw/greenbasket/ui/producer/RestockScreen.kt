//Restock Screen
package com.igdtuw.greenbasket.ui.producer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant

data class ProductStock(
    val name: String = "",
    val quantity: Int = 0,
    val docId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockScreen(navController: NavController, onBack: () -> Unit = {}) {
    val db = FirebaseFirestore.getInstance()
    val auth = Firebase.auth
    val context = LocalContext.current
    val uid = auth.currentUser?.uid ?: return

    val products = remember { mutableStateListOf<ProductStock>() }
    var showDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<ProductStock?>(null) }
    var restockInput by remember { mutableStateOf(TextFieldValue("")) }

    // Realtime fetch of crops for the logged-in producer
    LaunchedEffect(uid) {
        db.collection("producers").document(uid).collection("crops")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                products.clear()
                snapshot?.documents?.forEach { doc ->
                    val name = doc.getString("name") ?: return@forEach
                    val quantity = doc.getLong("quantity")?.toInt() ?: 0
                    products.add(ProductStock(name = name, quantity = quantity, docId = doc.id))
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Restock",
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
                    containerColor = DarkGreen
                )
            )
        }, containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                items(products) { product ->
                    val lowStock = product.quantity <= 10
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (lowStock) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Product: ${product.name}")
                            Text("Stock: ${product.quantity} kg")
                            if (lowStock) {
                                Text("⚠️ Low Stock! Please restock soon.", color = Color.Red)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    selectedProduct = product
                                    restockInput = TextFieldValue("")
                                    showDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
                            ) {
                                Text("Restock", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Restock dialog
            if (showDialog && selectedProduct != null) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                val newQty = restockInput.text.toIntOrNull()
                                if (newQty != null) {
                                    db.collection("producers").document(uid)
                                        .collection("crops")
                                        .document(selectedProduct!!.docId)
                                        .update("quantity", newQty)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, "Stock updated", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                showDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor =  Color.White)
                        ) {
                            Text("Update")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel", color = ConsumerPrimaryVariant)
                        }
                    },
                    title = { Text("Update Quantity") },
                    text = {
                        Column {
                            Text("Enter new stock for ${selectedProduct?.name}:")
                            Spacer(modifier = Modifier.height(8.dp))
                            TextField(
                                value = restockInput,
                                onValueChange = { restockInput = it },
                                placeholder = { Text("e.g., 50") }
                            )
                        }
                    }
                )
            }
        }
    }
}