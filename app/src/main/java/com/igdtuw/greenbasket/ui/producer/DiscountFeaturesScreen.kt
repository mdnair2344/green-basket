//DiscountFeaturesScreen
package com.igdtuw.greenbasket.ui.producer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlin.random.Random

data class DiscountOffer(
    val id: String = "DEAL${Random.nextInt(1000, 9999)}",
    val title: String = "",
    val discountValue: Double = 0.0,
    val validTill: String = "", // YYYY-MM-DD
    val promoCode: String = "",
    val appliesToAllCrops: Boolean = false,
    val cropNames: List<String> = emptyList()
)

fun generatePromoCode(title: String, value: String): String {
    val prefix = when (title.lowercase()) {
        "festival offer" -> "FEST"
        "bulk purchase" -> "BULK"
        "new user" -> "NEW"
        "limited time" -> "TIME"
        else -> "SAVE"
    }
    return "$prefix${value.filter { it.isDigit() }}"
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountFeaturesScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val producerUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val offers = remember { mutableStateListOf<DiscountOffer>() }
    var isDialogOpen by remember { mutableStateOf(false) }
    var offerToEdit by remember { mutableStateOf<DiscountOffer?>(null) }

    // Listen to this producer's discounts
    // Replace inside DiscountFeaturesScreen composable
    DisposableEffect(Unit) {
        val listener = firestore.collection("producers")
            .document(producerUid)
            .collection("discount_offers")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                offers.clear()
                for (doc in snapshot.documents) {
                    doc.toObject<DiscountOffer>()?.let { offers.add(it) }
                }
            }
        onDispose { listener.remove() }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Discount Features",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGreen)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    offerToEdit = null
                    isDialogOpen = true
                },
                containerColor = Color(0xFF2E7D32)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }, containerColor = Color.White
    ) { padding ->
        if (offers.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No Discounts Available")
            }
        } else {
            LazyColumn(modifier = Modifier
                .padding(padding)
                .padding(16.dp)
            ) {
                items(offers) { discount ->
                    DiscountCard(
                        discount = discount,
                        context = context,
                        onEdit = {
                            offerToEdit = it
                            isDialogOpen = true
                        },
                        onDelete = {
                            firestore.collection("producers")
                                .document(producerUid)
                                .collection("discount_offers")
                                .document(it.id)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Offer deleted", Toast.LENGTH_SHORT).show()
                                }
                        }

                    )
                }
            }
        }

        if (isDialogOpen) {
            AddEditOfferDialog(
                existingOffer = offerToEdit,
                onDismiss = { isDialogOpen = false },
                // Replace inside onSave lambda of AddEditOfferDialog
                onSave = { offer ->
                    firestore.collection("producers")
                        .document(producerUid)
                        .collection("discount_offers")
                        .document(offer.id)
                        .set(offer)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Offer saved", Toast.LENGTH_SHORT).show()
                            isDialogOpen = false
                        }
                }

            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DiscountCard(
    discount: DiscountOffer,
    context: Context,
    onEdit: (DiscountOffer) -> Unit,
    onDelete: (DiscountOffer) -> Unit
) {
    val today = LocalDate.now()
    val daysLeft = try {
        ChronoUnit.DAYS.between(today, LocalDate.parse(discount.validTill))
    } catch (e: Exception) {
        -1
    }
    val isExpired = daysLeft < 0

    val icon = when (discount.title.lowercase()) {
        "festival offer" -> Icons.Default.Star
        "bulk purchase" -> Icons.Default.ShoppingCart
        else -> Icons.Default.LocalOffer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) Color(0xFFFFEBEE) else ConsumerCardBackground1
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(discount.title, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onEdit(discount) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ConsumerPrimaryVariant)
                }
                IconButton(onClick = { onDelete(discount) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Value: ${discount.discountValue}")
            Text("Promo Code: ${discount.promoCode}", color = Color.Gray)
            Text("Valid Till: ${discount.validTill}", fontStyle = FontStyle.Italic)

            if (isExpired) {
                Text("Status: Expired", color = Color.Red)
            } else {
                Text("⏳ $daysLeft days left", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Promo Code", discount.promoCode))
                    Toast.makeText(context, "Promo code copied!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784))
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
                Spacer(Modifier.width(4.dp))
                Text("Share Promo Code")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AddEditOfferDialog(
    existingOffer: DiscountOffer?,
    onDismiss: () -> Unit,
    onSave: (DiscountOffer) -> Unit
) {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var title by remember { mutableStateOf(existingOffer?.title ?: "") }
    var discountValue by remember { mutableStateOf(existingOffer?.discountValue?.toString() ?: "") }
    var dateText by remember { mutableStateOf(existingOffer?.validTill ?: "") }

    var appliesToAll by remember { mutableStateOf(existingOffer?.appliesToAllCrops ?: false) }
    val cropList = remember { mutableStateListOf<String>() }
    val selectedCrops = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("producers")
            .document(userId)
            .collection("crops")
            .get()
            .addOnSuccessListener { snapshot ->
                cropList.clear()
                cropList.addAll(snapshot.documents.mapNotNull { it.getString("name") })
                if (existingOffer != null && !existingOffer.appliesToAllCrops) {
                    selectedCrops.clear()
                    selectedCrops.addAll(existingOffer.cropNames)
                }
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                try {
                    val date = LocalDate.parse(dateText)
                    val promo = existingOffer?.promoCode ?: generatePromoCode(title, discountValue)
                    val offer = DiscountOffer(
                        id = existingOffer?.id ?: "DEAL${Random.nextInt(1000, 9999)}",
                        title = title,
                        discountValue = discountValue.toDoubleOrNull() ?: 0.0,
                        promoCode = promo,
                        validTill = date.toString(),
                        appliesToAllCrops = appliesToAll,
                        cropNames = if (appliesToAll) emptyList() else selectedCrops.toList()
                    )
                    onSave(offer)
                } catch (e: Exception) {
                    Toast.makeText(context, "Please check date or discount!", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text(if (existingOffer != null) "Edit Offer" else "Add Offer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Offer Title") })
                OutlinedTextField(
                    value = discountValue,
                    onValueChange = { discountValue = it },
                    label = { Text("Discount Value (%)") },
                    singleLine = true
                )
                OutlinedTextField(value = dateText, onValueChange = { dateText = it }, label = { Text("Valid Till (YYYY-MM-DD)") })

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = appliesToAll,
                        onCheckedChange = { appliesToAll = it }
                    )
                    Text("Apply to all crops")
                }

                if (!appliesToAll) {
                    Text("Select Crops", fontWeight = FontWeight.Bold)
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 150.dp)
                            .fillMaxWidth()
                    ) {
                        items(cropList) { crop ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedCrops.contains(crop),
                                    onCheckedChange = {
                                        if (it) selectedCrops.add(crop)
                                        else selectedCrops.remove(crop)
                                    }
                                )
                                Text(crop)
                            }
                        }
                    }
                }
            }
        }
    )
}
