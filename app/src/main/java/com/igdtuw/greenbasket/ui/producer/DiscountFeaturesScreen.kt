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
import androidx.compose.ui.text.input.KeyboardType
import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import java.util.Calendar


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

                val today = LocalDate.now()
                val toDeleteIds = mutableListOf<String>()
                val fresh = mutableListOf<DiscountOffer>()

                for (doc in snapshot.documents) {
                    val offer = doc.toObject<DiscountOffer>() ?: continue
                    val isExpiredOrInvalid = try {
                        val end = LocalDate.parse(offer.validTill) // expects YYYY-MM-DD
                        end.isBefore(today)                       // expired if before today
                    } catch (_: Exception) {
                        true // invalid date -> treat as expired/invalid and delete
                    }

                    if (isExpiredOrInvalid) {
                        toDeleteIds += doc.id
                    } else {
                        fresh += offer
                    }
                }

                // Update list with only valid, non-expired offers
                offers.clear()
                offers.addAll(fresh)

                // Batch delete expired/invalid offers
                if (toDeleteIds.isNotEmpty()) {
                    val colRef = firestore.collection("producers")
                        .document(producerUid)
                        .collection("discount_offers")
                    val batch = firestore.batch()
                    toDeleteIds.forEach { id -> batch.delete(colRef.document(id)) }
                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Removed ${toDeleteIds.size} expired offer(s).",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    // You could add .addOnFailureListener { /* log or toast */ }
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

    // ---------------- State ----------------
    var title by remember { mutableStateOf(existingOffer?.title ?: "") }
    var discountValue by remember { mutableStateOf(existingOffer?.discountValue?.toInt()?.toString() ?: "") } // integer string
    var dateText by remember { mutableStateOf(existingOffer?.validTill ?: "") }

    var appliesToAll by remember { mutableStateOf(existingOffer?.appliesToAllCrops ?: false) }
    val cropList = remember { mutableStateListOf<String>() }
    val selectedCrops = remember { mutableStateListOf<String>() }

    // Show errors only after Save is pressed
    var showErrors by remember { mutableStateOf(false) }
    var titleErr by remember { mutableStateOf<String?>(null) }
    var discountErr by remember { mutableStateOf<String?>(null) }
    var dateErr by remember { mutableStateOf<String?>(null) }
    var cropsErr by remember { mutableStateOf<String?>(null) }

    // Load producer crops
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("producers")
            .document(userId)
            .collection("crops")
            .get()
            .addOnSuccessListener { snapshot ->
                cropList.clear()
                cropList.addAll(
                    snapshot.documents.mapNotNull { it.getString("name") }
                        .filter { !it.isNullOrBlank() }
                        .map { it!! }
                )
                if (existingOffer != null && !existingOffer.appliesToAllCrops) {
                    selectedCrops.clear()
                    selectedCrops.addAll(existingOffer.cropNames)
                }
            }
    }

    // Calendar picker -> writes date as YYYY-MM-DD and disallows past dates
    fun openDatePicker() {
        val cal = Calendar.getInstance()

        // Prefill with existing date if valid (keeps current selection)
        runCatching {
            if (dateText.isNotBlank()) {
                val d = LocalDate.parse(dateText)
                cal.set(Calendar.YEAR, d.year)
                cal.set(Calendar.MONTH, d.monthValue - 1)
                cal.set(Calendar.DAY_OF_MONTH, d.dayOfMonth)
            }
        }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = LocalDate.of(y, m + 1, d)
                dateText = picked.toString() // ISO yyyy-MM-dd
            },
            year, month, day
        )

        // Disallow past dates (allows TODAY or later)
        val min = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        dpd.datePicker.minDate = min

        // If you want STRICTLY future (tomorrow and later), use this instead:
        // dpd.datePicker.minDate = min + 24L * 60L * 60L * 1000L

        dpd.show()
    }


    // ---------------- Validation ----------------
    fun validate(): Boolean {
        // Title: alphabets + spaces only
        val titleTrim = title.trim()
        titleErr = when {
            titleTrim.isEmpty() -> "Title is required."
            !titleTrim.matches(Regex("^[A-Za-z ]+\$")) -> "Only alphabets and spaces allowed."
            else -> null
        }

        // Discount: integer 0..100
        val dvInt = discountValue.toIntOrNull()
        discountErr = when {
            discountValue.isBlank() -> "Discount is required."
            dvInt == null -> "Enter a whole number."
            dvInt < 0 || dvInt > 100 -> "Discount must be between 0 and 100."
            else -> null
        }

        // Date: must parse as LocalDate (format yyyy-MM-dd)
        dateErr = try {
            LocalDate.parse(dateText)
            null
        } catch (_: Exception) {
            if (dateText.isBlank()) "End date is required." else "Date must be YYYY-MM-DD."
        }

        // Crops: if not appliesToAll -> must select at least one
        cropsErr = if (!appliesToAll && selectedCrops.isEmpty()) {
            "Select at least one crop or apply to all."
        } else null

        return listOf(titleErr, discountErr, dateErr, cropsErr).all { it == null }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                showErrors = true
                if (!validate()) return@TextButton

                val promo = existingOffer?.promoCode ?: generatePromoCode(title, discountValue)
                val dv = discountValue.toIntOrNull() ?: 0
                val offer = DiscountOffer(
                    id = existingOffer?.id ?: "DEAL${Random.nextInt(1000, 9999)}",
                    title = title.trim(),
                    discountValue = dv.toDouble(), // stored as Double but integer 0..100
                    promoCode = promo,
                    validTill = LocalDate.parse(dateText).toString(),
                    appliesToAllCrops = appliesToAll,
                    cropNames = if (appliesToAll) emptyList() else selectedCrops.toList()
                )
                onSave(offer)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (existingOffer != null) "Edit Offer" else "Add Offer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Title (alphabets & spaces)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Offer Title (alphabets only)") },
                    isError = showErrors && titleErr != null,
                    supportingText = {
                        if (showErrors && titleErr != null)
                            Text(titleErr!!, color = MaterialTheme.colorScheme.error)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Discount % (digits only, 0..100)
                OutlinedTextField(
                    value = discountValue,
                    onValueChange = { new ->
                        // allow digits only
                        val cleaned = new.filter { it.isDigit() }
                        // Optional: cap length to 3 to keep within 0..100
                        discountValue = cleaned.take(3)
                    },
                    label = { Text("Discount Value (%) — 0 to 100") },
                    isError = showErrors && discountErr != null,
                    supportingText = {
                        if (showErrors && discountErr != null)
                            Text(discountErr!!, color = MaterialTheme.colorScheme.error)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Valid till (calendar picker)
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { /* read-only; set by picker */ },
                    label = { Text("Valid Till (YYYY-MM-DD)") },
                    isError = showErrors && dateErr != null,
                    supportingText = {
                        if (showErrors && dateErr != null)
                            Text(dateErr!!, color = MaterialTheme.colorScheme.error)
                    },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { openDatePicker() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDatePicker() }
                )

                // Applies to all
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = appliesToAll,
                        onCheckedChange = { appliesToAll = it }
                    )
                    Text("Apply to all crops")
                }

                // Crop selection (required if not appliesToAll). No dropdowns—just simple checkboxes list.
                if (!appliesToAll) {
                    Text("Select Crops", fontWeight = FontWeight.Bold)
                    if (showErrors && cropsErr != null) {
                        Text(cropsErr!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                    }
                    LazyColumn(
                        modifier = Modifier
                            .heightIn(max = 160.dp)
                            .fillMaxWidth()
                    ) {
                        items(cropList) { crop ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedCrops.contains(crop),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedCrops.add(crop) else selectedCrops.remove(crop)
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

