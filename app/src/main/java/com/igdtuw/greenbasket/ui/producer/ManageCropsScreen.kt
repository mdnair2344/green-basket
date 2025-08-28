//ManageCropsScreen
package com.igdtuw.greenbasket.ui.producer // Adjust package name as per your project

import android.provider.MediaStore
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.draw.clip
import androidx.navigation.NavController
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.compose.foundation.text.KeyboardOptions
import com.igdtuw.greenbasket.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.ExposedDropdownMenuBox
//import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuDefaults
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType





// Data class for Crop
data class Crop(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val variety: String = "",
    val quantity: Int = 0,
    val pricePerKg: Double = 0.0,
    val description: String = "",
    val imageUri: String = "", // Cloudinary URL for the crop image
    val cloudinaryPublicId: String = "", // NEW FIELD: Cloudinary public_id for better management
    val producer: String = ""
)


@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCropsScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    if (uid == null) {
        Text("Please log in to manage your crops.", modifier = Modifier.padding(16.dp))
        return
    }

    var crops by remember { mutableStateOf<List<Crop>>(emptyList()) }
    var selectedCrop by remember { mutableStateOf<Crop?>(null) }
    val showDialog = remember { mutableStateOf(false) }
    var confirmDeleteCropId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uid) {
        db.collection("producers").document(uid).collection("crops")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error fetching crops: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                crops = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Crop::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Crops", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20)
                ),
                actions = {
                    IconButton(onClick = {
                        generateCropPdf(context, crops)
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Download Crop PDF", tint = Color.White)
                    }
                }

            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                selectedCrop = null
                showDialog.value = true
            },
                containerColor = ConsumerPrimaryVariant,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Crop")
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(16.dp)
        ) {
            if (crops.isEmpty()) {
                Text("No crops added yet. Tap '+' to add one.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(crops) { crop ->
                        CropItem(
                            crop = crop,
                            onEdit = {
                                selectedCrop = crop
                                showDialog.value = true
                            },
                            onDelete = {
                                confirmDeleteCropId = it.id
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog.value) {
        AddEditCropDialog(
            crop = selectedCrop,
            onDismiss = { showDialog.value = false },
            onSave = { cropToSave, imageFileUri ->
                scope.launch(Dispatchers.IO) {
                    val producerId = uid
                    val cropId = cropToSave.id.ifBlank {
                        db.collection("producers").document(producerId).collection("crops").document().id
                    }
                    val oldPublicId = selectedCrop?.cloudinaryPublicId?.takeIf { selectedCrop!!.imageUri.isNotBlank() }

                    if (imageFileUri != null) {
                        val folderPath = "Medias/$producerId/crops/$cropId"

                        uploadToCloudinary(context, imageFileUri, folderPath,
                            onSuccess = { newImageUrl, newPublicId ->
                                val newCrop = cropToSave.copy(
                                    id = cropId,
                                    imageUri = newImageUrl,
                                    cloudinaryPublicId = newPublicId,
                                    producer = producerId
                                )

                                db.collection("producers").document(producerId)
                                    .collection("crops").document(cropId)
                                    .set(newCrop)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Crop saved successfully!", Toast.LENGTH_SHORT).show()
                                        showDialog.value = false
                                        if (oldPublicId != null && oldPublicId != newPublicId) {
                                            simulateCloudinaryDeletion(
                                                imageUrl = "https://res.cloudinary.com/dgbtfbkk3/image/upload/$oldPublicId",
                                                onSuccess = {
                                                    Toast.makeText(context, "Old image (simulated) deleted from Cloudinary.", Toast.LENGTH_SHORT).show()
                                                },
                                                onFailure = { errorMsg ->
                                                    Toast.makeText(context, "Failed to simulate old image deletion: $errorMsg", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Error saving crop: ${it.message}", Toast.LENGTH_LONG).show()
                                    }
                            },
                            onFailure = {
                                Toast.makeText(context, "Image upload failed: $it", Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        val updatedCrop = cropToSave.copy(
                            id = cropId,
                            imageUri = selectedCrop?.imageUri ?: "",
                            cloudinaryPublicId = selectedCrop?.cloudinaryPublicId ?: "",
                            producer = producerId
                        )

                        db.collection("producers").document(producerId)
                            .collection("crops").document(cropId)
                            .set(updatedCrop)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Crop saved successfully!", Toast.LENGTH_SHORT).show()
                                showDialog.value = false
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error saving crop: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            }
        )
    }

    confirmDeleteCropId?.let { cropIdToDelete ->
        AlertDialog(
            onDismissRequest = { confirmDeleteCropId = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this crop? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val producerId = uid
                        val cropRef = db.collection("producers").document(producerId).collection("crops").document(cropIdToDelete)
                        cropRef.get().addOnSuccessListener { docSnap ->
                            val cropToDelete = docSnap.toObject(Crop::class.java)
                            val imageUrl = cropToDelete?.imageUri
                            val publicId = cropToDelete?.cloudinaryPublicId
                            cropRef.delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Crop deleted", Toast.LENGTH_SHORT).show()
                                    confirmDeleteCropId = null
                                    if (imageUrl != null && publicId != null) {
                                        simulateCloudinaryDeletion(
                                            imageUrl = imageUrl,
                                            onSuccess = {
                                                Toast.makeText(context, "Image (simulated) deleted", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {
                                                Toast.makeText(context, "Simulated deletion failed: $it", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to delete crop: ${it.message}", Toast.LENGTH_SHORT).show()
                                    confirmDeleteCropId = null
                                }
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to fetch crop: ${it.message}", Toast.LENGTH_SHORT).show()
                            confirmDeleteCropId = null
                        }
                    }
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCropId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun CropItem(crop: Crop, onEdit: (Crop) -> Unit, onDelete: (Crop) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1), // CHANGED TO THE LIVE CHAT CARD COLOR
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Image on the Left
            if (crop.imageUri.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(crop.imageUri),
                    contentDescription = "Image of ${crop.name}",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 12.dp)
                        .clip(CircleShape), // Clip the image to a circle
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder if no image
                Icon(
                    imageVector = Icons.Default.Add, // Or a generic crop icon
                    contentDescription = "No image available",
                    modifier = Modifier
                        .size(80.dp)
                        .padding(end = 12.dp)
                        .background(Color.LightGray) // Background for placeholder
                        .clip(CircleShape),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = crop.name, style = MaterialTheme.typography.titleMedium)
                Text("Variety: ${crop.variety}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("Qty: ${crop.quantity}kg, ₹${crop.pricePerKg}/kg", style = MaterialTheme.typography.bodyMedium)
                // You can add more crop details here if desired
            }

            // Action Icons (Edit & Delete)
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onEdit(crop) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Crop", tint = ConsumerPrimaryVariant)
                }
                IconButton(onClick = { onDelete(crop) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Crop", tint = Color.Red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCropDialog(
    crop: Crop?, // Null for adding, non-null for editing
    onDismiss: () -> Unit,
    onSave: (Crop, Uri?) -> Unit // Pass the new image URI separately
) {
    val darkGreen = Color(0xFF1B5E20)

    // ---------- Catalog (Category -> Name -> Varieties) ----------
    val catalog: Map<String, Map<String, List<String>>> = mapOf(
        "Fruits" to mapOf(
            "Mango" to listOf("Alphonso", "Dasheri", "Langra", "Himsagar", "Totapuri", "Kesar", "Neelum", "Raspuri", "Mallika", "Other"),
            "Apple" to listOf("Gala", "Fuji", "Honeycrisp", "Granny Smith", "Red Delicious", "Golden Delicious", "Ambri", "Shimla", "Kashmiri", "Other"),
            "Guava" to listOf("Allahabadi", "Lucknow 49", "Pearl", "Lalit", "Banarasi", "Pink Guava", "Thai Guava", "Other"),
            "Banana" to listOf("Robusta", "Nendran", "Rasthali", "Poovan", "Monthan", "Grand Naine", "Dwarf Cavendish", "Red Banana", "Other"),
            "Papaya" to listOf("Red Lady", "Pusa Delicious", "Taiwan Red", "Coorg Honey Dew", "Washington", "Other"),
            "Grapes" to listOf("Thompson Seedless", "Sharad Seedless", "Bangalore Blue", "Anab-e-Shahi", "Flame Seedless", "Crimson", "Black Grapes", "Other"),
            "Pomegranate" to listOf("Bhagwa", "Ganesh", "Arakta", "Kandhari", "Ruby", "Other"),
            "Orange" to listOf("Nagpur", "Malta", "Kinnow", "Coorg", "Blood Orange", "Jaffa", "Other"),
            "Watermelon" to listOf("Sugar Baby", "Arka Manik", "Charleston Grey", "Icebox", "Yellow Crimson", "Other"),
            "Pineapple" to listOf("Queen", "Kew", "Mauritius", "MD2", "Other"),
            "Litchi" to listOf("Shahi", "Dehradun", "Bombai", "Bedana", "Other"),
            "Jackfruit" to listOf("Sweet", "Soft Flesh", "Firm Flesh", "Mini", "Other"),
            "Chikoo (Sapota)" to listOf("Kalipatti", "Pala", "CO-2", "Other"),
            "Peach" to listOf("Indian Peach", "Clingstone", "Freestone", "Other"),
            "Pear" to listOf("Indian Pear", "Patharnakh", "Kieffer", "Other"),
            "Plum" to listOf("Kala Amritsari", "Sutlej Purple", "Alu Bukhara", "Other"),
            "Coconut" to listOf("Tender Coconut", "Mature Coconut", "Hybrid", "Other"),
            "Custard Apple" to listOf("Balanagar", "Red Sitaphal", "Hybrid", "Other"),
            "Strawberry" to listOf("Chandler", "Sweet Charlie", "Winter Dawn", "Other"),
            "Dragon Fruit" to listOf("White Flesh", "Red Flesh", "Yellow Dragon", "Other"),
            "Other" to listOf("Other")
        ),

        "Vegetables" to mapOf(
            "Onion" to listOf("Red", "White", "Yellow", "Spring", "Shallots", "Other"),
            "Potato" to listOf("Kufri Jyoti", "Kufri Pukhraj", "Baby", "Chipsona", "Desiree", "Other"),
            "Tomato" to listOf("Cherry", "Roma", "Heirloom", "Hybrid", "Beefsteak", "Plum", "Other"),
            "Cabbage" to listOf("Green", "Red", "Savoy", "Chinese (Napa)", "Pointed", "Other"),
            "Cauliflower" to listOf("Snowball", "Purple", "Cheddar", "Green", "Romanesco", "Other"),
            "Brinjal (Eggplant)" to listOf("Bharta", "Green Long", "Round Purple", "Black Beauty", "White Eggplant", "Other"),
            "Okra (Ladyfinger)" to listOf("Arka Anamika", "Parbhani Kranti", "Hybrid", "Clemson Spineless", "Other"),
            "Beans" to listOf("French Beans", "Cluster Beans (Gawar)", "Cowpea (Lobia)", "Broad Beans", "Hyacinth Beans", "Other"),
            "Chillies" to listOf("Green", "Red Dry", "Bird's Eye", "Byadgi", "Guntur", "Bhut Jolokia", "Other"),
            "Bitter Gourd (Karela)" to listOf("Indian Long", "Short", "Hybrid", "Other"),
            "Bottle Gourd (Lauki)" to listOf("Long", "Round", "Hybrid", "Other"),
            "Pumpkin" to listOf("Orange", "Green", "Ash Gourd", "Other"),
            "Carrot" to listOf("Orange", "Black", "Red", "Baby Carrot", "Other"),
            "Radish" to listOf("White Long", "Round Red", "Japanese Minowase", "Other"),
            "Spinach" to listOf("Indian Palak", "Baby Spinach", "Other"),
            "Amaranthus (Chaulai)" to listOf("Green", "Red", "Other"),
            "Coriander Leaves" to listOf("Local", "Hybrid", "Other"),
            "Mint Leaves" to listOf("Peppermint", "Spearmint", "Pudina", "Other"),
            "Cucumber" to listOf("Green Long", "Hybrid", "English Cucumber", "Gherkin", "Other"),
            "Capsicum (Bell Pepper)" to listOf("Green", "Red", "Yellow", "Orange", "Other"),
            "Mushroom" to listOf("Button", "Oyster", "Shiitake", "Portobello", "Other"),
            "Turnip" to listOf("White", "Purple Top", "Other"),
            "Sweet Corn" to listOf("Yellow", "White", "Baby Corn", "Other"),
            "Other" to listOf("Other")
        ),
        "Dairy Products" to mapOf(
            "Milk" to listOf("Cow", "Buffalo", "A2", "Organic", "Other"),
            "Curd" to listOf("Plain", "Greek", "Probiotic", "Other"),
            "Paneer" to listOf("Malai", "Regular", "Low Fat", "Other"),
            "Butter" to listOf("Salted", "Unsalted", "White Butter", "Other"),
            "Cheese" to listOf("Cheddar", "Mozzarella", "Paneer Cheese", "Other"),
            "Ghee" to listOf("Cow Ghee", "Buffalo Ghee", "Organic Ghee", "Other"),
            "Other" to listOf("Other")
        ),
        "Pulses" to mapOf(
            "Chana" to listOf("Desi", "Kabuli", "Bengal Gram", "Other"),
            "Moong" to listOf("Sabut (Whole)", "Dhuli (Split)", "Green Gram", "Other"),
            "Masoor" to listOf("Whole", "Split", "Brown Lentil", "Other"),
            "Urad" to listOf("Whole", "Split", "Black Gram", "Other"),
            "Arhar (Toor)" to listOf("Pigeon Pea", "Split Toor Dal", "Other"),
            "Rajma" to listOf("Red", "Jammu Rajma", "Chitra", "Other"),
            "Other" to listOf("Other")
        ),
        "Grains" to mapOf(
            "Wheat" to listOf("Sharbati", "Lokwan", "Durum", "Other"),
            "Rice" to listOf("Basmati", "Sona Masoori", "Kolam", "Brown Rice", "Matta", "Jasmine", "Other"),
            "Maize" to listOf("Sweet", "Dent", "Flint", "Baby Corn", "Other"),
            "Bajra (Pearl Millet)" to listOf("White Bajra", "Grey Bajra", "Other"),
            "Jowar (Sorghum)" to listOf("White", "Red", "Hybrid", "Other"),
            "Ragi (Finger Millet)" to listOf("Whole", "Sprouted", "Other"),
            "Other" to listOf("Other")
        )
    )
    val categoryOptions = remember { catalog.keys.toList() }

    // ---------- State (pre-fill from crop) ----------
    var category by remember { mutableStateOf(crop?.category ?: "") }
    var nameSel by remember { mutableStateOf(crop?.name?.takeIf { it.isNotBlank() } ?: "") }
    var varietySel by remember { mutableStateOf(crop?.variety?.takeIf { it.isNotBlank() } ?: "") }

    // If incoming crop has a name/variety not present in catalog, treat as "Other" + custom text
    val isNameKnown = category.isNotBlank() &&
            catalog[category]?.containsKey(nameSel) == true && nameSel != "Other"
    val isVarietyKnown = isNameKnown &&
            catalog[category]?.get(nameSel)?.contains(varietySel) == true && varietySel != "Other"

    var customName by remember { mutableStateOf(if (!isNameKnown && nameSel.isNotBlank()) nameSel else "") }
    var customVariety by remember { mutableStateOf(if (!isVarietyKnown && varietySel.isNotBlank()) varietySel else "") }

    // Quantity / Price / Description
    var quantityText by remember { mutableStateOf(crop?.quantity?.takeIf { it > 0 }?.toString() ?: "") }
    var priceText by remember { mutableStateOf(crop?.pricePerKg?.takeIf { it > 0.0 }?.toString() ?: "") }
    var description by remember { mutableStateOf(crop?.description ?: "") }

    // Image
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        newImageUri = uri
    }

    // ---------- Dependent options ----------
    val nameOptions: List<String> = remember(category) {
        if (category.isBlank()) emptyList()
        else (catalog[category]?.keys?.toList() ?: emptyList())
    }

    val varietyOptions: List<String> = remember(category, nameSel) {
        if (category.isBlank() || nameSel.isBlank()) emptyList()
        else (catalog[category]?.get(nameSel) ?: emptyList())
    }

    // ---------- Keep invariants ----------
    // If category changes, reset name & variety
    LaunchedEffect(category) {
        nameSel = ""
        varietySel = ""
        customName = ""
        customVariety = ""
    }

    // If name changes, reset variety
    LaunchedEffect(nameSel) {
        varietySel = ""
        customVariety = ""
        // If name == Other, force variety == Other as requested
        if (nameSel == "Other") {
            varietySel = "Other"
        }
    }

    // ---------- Validation (show only after Save is pressed) ----------
    var showErrors by remember { mutableStateOf(false) }
    var categoryErr by remember { mutableStateOf<String?>(null) }
    var nameErr by remember { mutableStateOf<String?>(null) }
    var varietyErr by remember { mutableStateOf<String?>(null) }
    var quantityErr by remember { mutableStateOf<String?>(null) }
    var priceErr by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        categoryErr = if (category.isBlank()) "Please select a category." else null

        if (nameSel.isBlank()) {
            nameErr = "Please select a name."
        } else if (nameSel == "Other" && customName.isBlank()) {
            nameErr = "Please type the crop name."
        } else {
            nameErr = null
        }

        if (nameSel == "Other") {
            varietyErr = if (customVariety.isBlank()) "Please type the variety." else null
        } else {
            varietyErr = when {
                varietySel.isBlank() -> "Please select a variety."
                varietySel == "Other" && customVariety.isBlank() -> "Please type the variety."
                else -> null
            }
        }

        val q = quantityText.toIntOrNull()
        quantityErr = when {
            quantityText.isBlank() -> "Please enter quantity."
            q == null || q <= 0 -> "Quantity must be a positive whole number."
            else -> null
        }

        val p = priceText.toDoubleOrNull()
        priceErr = when {
            priceText.isBlank() -> "Please enter price per kg."
            p == null || p <= 0.0 -> "Price must be a positive number."
            else -> null
        }

        return listOf(categoryErr, nameErr, varietyErr, quantityErr, priceErr).all { it == null }
    }

    // ---------- UI ----------
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    // Show errors only after Save is attempted
                    showErrors = true
                    if (!validate()) return@Button

                    val finalName = if (nameSel == "Other") customName.trim() else nameSel
                    val finalVariety =
                        if (nameSel == "Other" || varietySel == "Other") customVariety.trim()
                        else varietySel

                    val updatedCrop = Crop(
                        id = crop?.id ?: "",
                        name = finalName,
                        variety = finalVariety,
                        category = category,
                        quantity = quantityText.toIntOrNull() ?: 0,
                        pricePerKg = priceText.toDoubleOrNull() ?: 0.0,
                        description = description,
                        imageUri = crop?.imageUri ?: "",
                        cloudinaryPublicId = crop?.cloudinaryPublicId ?: ""
                    )
                    onSave(updatedCrop, newImageUri)
                },
                // Always enabled; validation runs on click
                enabled = true,
                colors = ButtonDefaults.buttonColors(containerColor = darkGreen)
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = darkGreen)
            }
        },
        title = { Text(if (crop == null) "Add New Crop" else "Edit Crop Details") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = darkGreen,
                    cursorColor = darkGreen,
                    focusedLabelColor = darkGreen
                )

                // ---------- Category (Dropdown) ----------
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = !catExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        isError = showErrors && categoryErr != null,
                        supportingText = {
                            if (showErrors && categoryErr != null)
                                Text(categoryErr!!, color = MaterialTheme.colorScheme.error)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        colors = textFieldColors,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categoryOptions.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    category = selection
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ---------- Name (Dropdown + "Other" -> Text) ----------
                var nameExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = nameExpanded,
                    onExpandedChange = { if (category.isNotBlank()) nameExpanded = !nameExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when {
                            nameSel == "Other" -> "Other"
                            nameSel.isNotBlank() -> nameSel
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Name") },
                        isError = showErrors && nameErr != null,
                        supportingText = {
                            if (showErrors && nameErr != null)
                                Text(nameErr!!, color = MaterialTheme.colorScheme.error)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                        colors = textFieldColors,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = nameExpanded && category.isNotBlank(),
                        onDismissRequest = { nameExpanded = false }
                    ) {
                        nameOptions.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    nameSel = selection
                                    nameExpanded = false
                                }
                            )
                        }
                    }
                }

                if (nameSel == "Other") {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Enter Crop Name") },
                        isError = showErrors && nameErr != null,
                        supportingText = {
                            if (showErrors && nameErr != null)
                                Text(nameErr!!, color = MaterialTheme.colorScheme.error)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ---------- Variety (Dropdown + "Other" -> Text) ----------
                var varietyExpanded by remember { mutableStateOf(false) }
                val varietyReadOnly = !(category.isNotBlank() && nameSel.isNotBlank())

                ExposedDropdownMenuBox(
                    expanded = varietyExpanded,
                    onExpandedChange = {
                        if (nameSel != "Other" && !varietyReadOnly) {
                            varietyExpanded = !varietyExpanded
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when {
                            nameSel == "Other" -> "Other" // forced
                            varietySel == "Other" -> "Other"
                            varietySel.isNotBlank() -> varietySel
                            else -> ""
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Variety") },
                        isError = showErrors && varietyErr != null,
                        supportingText = {
                            if (showErrors && varietyErr != null)
                                Text(varietyErr!!, color = MaterialTheme.colorScheme.error)
                        },
                        trailingIcon = {
                            if (nameSel != "Other") {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = varietyExpanded)
                            }
                        },
                        colors = textFieldColors,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    if (nameSel != "Other") {
                        ExposedDropdownMenu(
                            expanded = varietyExpanded && !varietyReadOnly,
                            onDismissRequest = { varietyExpanded = false }
                        ) {
                            varietyOptions.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection) },
                                    onClick = {
                                        varietySel = selection
                                        varietyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (nameSel == "Other" || varietySel == "Other") {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customVariety,
                        onValueChange = { customVariety = it },
                        label = { Text("Enter Variety") },
                        isError = showErrors && varietyErr != null,
                        supportingText = {
                            if (showErrors && varietyErr != null)
                                Text(varietyErr!!, color = MaterialTheme.colorScheme.error)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ---------- Quantity ----------
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantity (kg)") },
                    isError = showErrors && quantityErr != null,
                    supportingText = {
                        if (showErrors && quantityErr != null)
                            Text(quantityErr!!, color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )

                Spacer(Modifier.height(8.dp))

                // ---------- Price per Kg ----------
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { new ->
                        val cleaned = new.filter { it.isDigit() || it == '.' }
                        if (cleaned.count { it == '.' } <= 1) {
                            priceText = cleaned
                        }
                    },
                    label = { Text("Price per Kg (₹)") },
                    isError = showErrors && priceErr != null,
                    supportingText = {
                        if (showErrors && priceErr != null)
                            Text(priceErr!!, color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )

                Spacer(Modifier.height(8.dp))

                // ---------- Description (optional) ----------
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ---------- Image ----------
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = darkGreen)
                ) {
                    Text("Select Crop Image", color = Color.White)
                }

                newImageUri?.let {
                    Text(
                        "New image selected: ${it.lastPathSegment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                } ?: crop?.imageUri?.let {
                    Text(
                        "Current image: (Click 'Select Crop Image' to change)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    )
}




@RequiresApi(Build.VERSION_CODES.Q)
fun generateCropPdf(context: Context, cropList: List<Crop>) {
    if (cropList.isEmpty()) {
        Toast.makeText(context, "No crops to export.", Toast.LENGTH_SHORT).show()
        return
    }

    val document = PdfDocument()
    val titlePaint = Paint().apply {
        textSize = 18f
        isFakeBoldText = true
        color = android.graphics.Color.BLACK
    }

    val textPaint = Paint().apply {
        textSize = 12f
        color = android.graphics.Color.BLACK
    }

    val whiteTextPaint = Paint().apply {
        textSize = 12f
        color = android.graphics.Color.WHITE
    }

    val headerBgPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#2E7D32")// Dark green
    }

    val rowBgPaint = Paint().apply {
        color = android.graphics.Color.parseColor("#E8F5E9")
    }

    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
    var page = document.startPage(pageInfo)
    var canvas = page.canvas

    val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
    val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 80, 80, true) // Resize if needed

    val logoX = pageInfo.pageWidth - scaledLogo.width - 40f
    val logoY = 40f
    canvas.drawBitmap(scaledLogo, logoX, logoY, null)

    canvas.drawText("Your Crops List", 40f, 50f, titlePaint)

    val lineSpacing = 25f
    val rowHeight = 25f
    val startX = 40f
    var y = logoY + scaledLogo.height + 20f // Start text 20px below logo

    val columnSpacing = listOf(startX, 80f, 200f, 300f, 400f, 480f)
    val columnWidths = listOf(40f, 120f, 100f, 100f, 80f, 90f)

    // Draw header background
    canvas.drawRect(
        startX,
        y - 15f,
        startX + columnWidths.sum(),
        y + 10f,
        headerBgPaint
    )

    val headers = listOf("S.No", "Name", "Variety", "Category", "Qty", "Price/kg")
    headers.forEachIndexed { index, text ->
        canvas.drawText(text, columnSpacing[index], y, whiteTextPaint)
    }

    y += lineSpacing

    cropList.forEachIndexed { index, crop ->
        // Check for page break
        if (y + rowHeight > 800f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
            canvas.drawText("Continued...", startX, y, textPaint)
            y += lineSpacing
        }

        // Draw row background
        canvas.drawRect(
            startX,
            y - 15f,
            startX + columnWidths.sum(),
            y + 10f,
            rowBgPaint
        )

        val row = listOf(
            (index + 1).toString(),
            crop.name,
            crop.variety,
            crop.category,
            "${crop.quantity}kg",
            "₹${crop.pricePerKg}"
        )

        row.forEachIndexed { i, text ->
            canvas.drawText(text, columnSpacing[i], y, textPaint)
        }

        y += lineSpacing
    }

    document.finishPage(page)

    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val fileName = "CropList_${sdf.format(Date())}.pdf"

    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    val contentResolver = context.contentResolver
    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

    try {
        uri?.let {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                document.writeTo(outputStream)
                Toast.makeText(context, "PDF saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
            }
        } ?: throw IOException("Failed to create PDF file.") as Throwable
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
        document.close()
    }

}

