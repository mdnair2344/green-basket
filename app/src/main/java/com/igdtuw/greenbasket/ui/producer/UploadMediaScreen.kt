//UploadMediaScreen
package com.igdtuw.greenbasket.ui.producer

import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import java.time.Instant
import java.time.ZoneId


data class Certificate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val issueDate: String = "",
    val expiryDate: String = "",
    val authority: String = "",
    val status: String = "",
    val certificateUrl: String = "",
    val cloudinaryPublicId: String = "",
    val category: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UploadMediaScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val producerId = FirebaseAuth.getInstance().currentUser?.uid

    if (producerId == null) {
        Text("Please log in to manage your certificates.", modifier = Modifier.padding(16.dp))
        return
    }

    var producerCertificates by remember { mutableStateOf<List<Certificate>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var confirmDeleteCertificateId by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(producerId) {
        db.collection("producers").document(producerId).collection("certificates")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error fetching certificates: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                producerCertificates = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Certificate::class.java)?.copy(id = doc.id)
                } ?: emptyList()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Upload Certificates",
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
                onClick = { showDialog = true },
                containerColor = DarkGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Upload Certificate")
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Your Certificates", fontSize = 24.sp, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

            if (producerCertificates.isEmpty()) {
                Text("No certificates added yet. Tap '+' to upload one.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(producerCertificates) { certificate ->
                        CertificateItem(certificate = certificate, onDelete = {
                            confirmDeleteCertificateId = it.id
                        })
                    }
                }
            }
        }
    }

    if (showDialog) {
        UploadCertificateDialog(onDismiss = { showDialog = false }) { certData, fileUri ->
            scope.launch(Dispatchers.IO) {
                val newCertDocRef = db.collection("producers").document(producerId).collection("certificates").document()
                val certId = newCertDocRef.id
                val certToUpload = certData.copy(id = certId)
                val folderPath = "Medias/$producerId/Certificates/$certId"

                uploadToCloudinary(
                    context, fileUri, folderPath,
                    onSuccess = { url, publicId ->
                        val certWithUrlAndPublicId = certToUpload.copy(certificateUrl = url, cloudinaryPublicId = publicId)
                        newCertDocRef.set(certWithUrlAndPublicId)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Certificate uploaded successfully!", Toast.LENGTH_SHORT).show()
                                showDialog = false
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to save certificate: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    },
                    onFailure = {
                        Toast.makeText(context, "Upload failed: $it", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    confirmDeleteCertificateId?.let { certIdToDelete ->
        AlertDialog(
            onDismissRequest = { confirmDeleteCertificateId = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this certificate? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        val certRef = db.collection("producers").document(producerId)
                            .collection("certificates").document(certIdToDelete)

                        certRef.get().addOnSuccessListener { documentSnapshot ->
                            val certificateToDelete = documentSnapshot.toObject(Certificate::class.java)
                            val publicId = certificateToDelete?.cloudinaryPublicId
                            val certificateUrl = certificateToDelete?.certificateUrl

                            certRef.delete()
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Deleted from Firestore", Toast.LENGTH_SHORT).show()
                                    confirmDeleteCertificateId = null
                                    if (publicId != null && certificateUrl != null) {
                                        simulateCloudinaryDeletion(certificateUrl,
                                            onSuccess = {
                                                Toast.makeText(context, "Deleted from Cloudinary (simulated)", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {
                                                Toast.makeText(context, "Simulated deletion failed: $it", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Delete failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                    confirmDeleteCertificateId = null
                                }
                        }
                    }
                }) {
                    Text("Delete", color = DarkGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCertificateId = null }) {
                    Text("Cancel", color = DarkGreen)
                }
            }
        )
    }
}

@Composable
fun CertificateItem(certificate: Certificate, onDelete: (Certificate) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Name: ${certificate.name}", style = MaterialTheme.typography.titleMedium, color=ConsumerPrimaryVariant)
            Text("Category: ${certificate.category}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Authority: ${certificate.authority}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Issue Date: ${certificate.issueDate}", style = MaterialTheme.typography.bodySmall)
            Text("Expiry Date: ${certificate.expiryDate}", style = MaterialTheme.typography.bodySmall)
            Text("Status: ${certificate.status}", style = MaterialTheme.typography.bodyMedium, color = if (certificate.status == "Valid") ConsumerPrimaryVariant else Color.Red)

            if (certificate.certificateUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(certificate.certificateUrl),
                    contentDescription = "Certificate Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Fit
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { onDelete(certificate) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadCertificateDialog(
    onDismiss: () -> Unit,
    onUpload: (Certificate, Uri) -> Unit
) {
    val context = LocalContext.current

    // --- Field state ---
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Category dropdown
    val categoryOptions = listOf(
        "Organic", "ISO 22000", "HACCP", "FSSAI", "Fair Trade", "GAP",
        "Halal", "Kosher", "Non-GMO", "Other"
    )
    var categoryExpanded by remember { mutableStateOf(false) }
    var categoryChoice by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }

    // Authority dropdown
    val authorityOptions = listOf(
        "FSSAI", "APEDA", "ISO", "BIS", "NABL", "USDA Organic",
        "EU Organic", "State Agriculture Dept", "Other"
    )
    var authorityExpanded by remember { mutableStateOf(false) }
    var authorityChoice by remember { mutableStateOf("") }
    var customAuthority by remember { mutableStateOf("") }

    // Date picking state
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    var issuePickerOpen by remember { mutableStateOf(false) }
    var expiryPickerOpen by remember { mutableStateOf(false) }

    var issueEpoch by remember { mutableStateOf<Long?>(null) }
    var expiryEpoch by remember { mutableStateOf<Long?>(null) }

    fun epochToLocalDateString(epoch: Long?): String {
        if (epoch == null) return ""
        val ld = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()
        return ld.format(dateFormatter)
    }

    // File
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        fileUri = uri
    }

    // --- Validation helpers ---
    fun isValidRequiredText(s: String, min: Int = 3) = s.trim().length >= min
    var nameError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var authorityError by remember { mutableStateOf<String?>(null) }
    var issueError by remember { mutableStateOf<String?>(null) }
    var expiryError by remember { mutableStateOf<String?>(null) }
    var crossDateError by remember { mutableStateOf<String?>(null) }
    var fileError by remember { mutableStateOf<String?>(null) }

    fun validateAll(): Boolean {
        var ok = true
        nameError = if (!isValidRequiredText(name)) { ok = false; "Enter at least 3 characters." } else null

        // Category
        val finalCategory = if (categoryChoice == "Other") customCategory else categoryChoice
        categoryError = when {
            categoryChoice.isBlank() -> { ok = false; "Select a category." }
            categoryChoice == "Other" && !isValidRequiredText(customCategory) -> { ok = false; "Specify the category." }
            else -> null
        }

        // Authority
        val finalAuthority = if (authorityChoice == "Other") customAuthority else authorityChoice
        authorityError = when {
            authorityChoice.isBlank() -> { ok = false; "Select an issuing authority." }
            authorityChoice == "Other" && !isValidRequiredText(customAuthority) -> { ok = false; "Specify the authority." }
            else -> null
        }

        // Dates
        issueError = if (issueEpoch == null) { ok = false; "Select an issue date." } else null
        expiryError = if (expiryEpoch == null) { ok = false; "Select an expiry date." } else null

        crossDateError = null
        if (issueEpoch != null && expiryEpoch != null) {
            val issueDate = Instant.ofEpochMilli(issueEpoch!!).atZone(ZoneId.systemDefault()).toLocalDate()
            val expiryDate = Instant.ofEpochMilli(expiryEpoch!!).atZone(ZoneId.systemDefault()).toLocalDate()
            if (!issueDate.isBefore(expiryDate)) {
                ok = false
                crossDateError = "Issue date must be **before** expiry date."
            }
        }

        fileError = if (fileUri == null) { ok = false; "Please select a certificate file." } else null

        return ok
    }

    // --- UI ---
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Producer Certificate") },
        text = {
            Column {
                fun modifier() = Modifier.fillMaxWidth()
                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkGreen,
                    cursorColor = DarkGreen,
                    focusedLabelColor = DarkGreen
                )

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError != null) nameError = null
                    },
                    label = { Text("Certificate Name*") },
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                    singleLine = true,
                    modifier = modifier(),
                    colors = fieldColors
                )

                // Description (optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = modifier(),
                    colors = fieldColors
                )

                Spacer(Modifier.height(4.dp))

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = modifier()
                ) {
                    OutlinedTextField(
                        value = if (categoryChoice.isBlank()) "" else categoryChoice,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        isError = categoryError != null,
                        supportingText = { categoryError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    categoryChoice = option
                                    categoryExpanded = false
                                    categoryError = null
                                    if (option != "Other") customCategory = ""
                                }
                            )
                        }
                    }
                }

                if (categoryChoice == "Other") {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = {
                            customCategory = it
                            categoryError = null
                        },
                        label = { Text("Specify Category*") },
                        isError = categoryError != null,
                        supportingText = { categoryError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = modifier(),
                        colors = fieldColors
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Authority dropdown
                ExposedDropdownMenuBox(
                    expanded = authorityExpanded,
                    onExpandedChange = { authorityExpanded = !authorityExpanded },
                    modifier = modifier()
                ) {
                    OutlinedTextField(
                        value = if (authorityChoice.isBlank()) "" else authorityChoice,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Issued By Authority*") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = authorityExpanded) },
                        isError = authorityError != null,
                        supportingText = { authorityError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = authorityExpanded,
                        onDismissRequest = { authorityExpanded = false }
                    ) {
                        authorityOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    authorityChoice = option
                                    authorityExpanded = false
                                    authorityError = null
                                    if (option != "Other") customAuthority = ""
                                }
                            )
                        }
                    }
                }

                if (authorityChoice == "Other") {
                    OutlinedTextField(
                        value = customAuthority,
                        onValueChange = {
                            customAuthority = it
                            authorityError = null
                        },
                        label = { Text("Specify Authority*") },
                        isError = authorityError != null,
                        supportingText = { authorityError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = modifier(),
                        colors = fieldColors
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Issue Date picker trigger
                Text("Issue Date*", fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = { issuePickerOpen = true },
                    modifier = modifier()
                ) {
                    Text(epochToLocalDateString(issueEpoch) .ifEmpty { "Select issue date" })
                }
                if (issueError != null) {
                    Text(issueError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(6.dp))

                // Expiry Date picker trigger
                Text("Expiry Date*", fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = { expiryPickerOpen = true },
                    modifier = modifier()
                ) {
                    Text(epochToLocalDateString(expiryEpoch) .ifEmpty { "Select expiry date" })
                }
                if (expiryError != null) {
                    Text(expiryError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                if (crossDateError != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(crossDateError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }

                Spacer(Modifier.height(8.dp))

                // File picker
                Button(
                    onClick = { launcher.launch(arrayOf("application/pdf", "image/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = Color.White)
                ) {
                    Text("Select Certificate File")
                }
                if (fileUri != null) {
                    Text(
                        "Selected: ${fileUri!!.lastPathSegment ?: "File"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                if (fileError != null) {
                    Text(fileError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate all fields first
                    if (!validateAll()) return@Button

                    // Build final values
                    val finalCategory = if (categoryChoice == "Other") customCategory.trim() else categoryChoice.trim()
                    val finalAuthority = if (authorityChoice == "Other") customAuthority.trim() else authorityChoice.trim()

                    val issueDate = Instant.ofEpochMilli(issueEpoch!!).atZone(ZoneId.systemDefault()).toLocalDate()
                    val expiryDate = Instant.ofEpochMilli(expiryEpoch!!).atZone(ZoneId.systemDefault()).toLocalDate()

                    // Status (based on expiry vs today)
                    val status = if (expiryDate.isAfter(LocalDate.now())) "Valid" else "Expired"

                    // Create Certificate using **named** args to avoid mis-mapping
                    val certData = Certificate(
                        name = name.trim(),
                        description = description.trim(),
                        issueDate = issueDate.format(dateFormatter),
                        expiryDate = expiryDate.format(dateFormatter),
                        authority = finalAuthority,
                        status = status,
                        category = finalCategory
                    )

                    isUploading = true
                    onUpload(certData, fileUri!!)
                },
                enabled = !isUploading,
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = Color.White)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Upload Certificate")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    // --- DatePicker Dialogs ---

    if (issuePickerOpen) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = issueEpoch ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { issuePickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    issueEpoch = state.selectedDateMillis
                    issuePickerOpen = false
                    // Clear cross-field error if previously set
                    crossDateError = null
                    issueError = null
                }) { Text("OK", color = DarkGreen) }
            },
            dismissButton = { TextButton(onClick = { issuePickerOpen = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }

    if (expiryPickerOpen) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = expiryEpoch ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { expiryPickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    expiryEpoch = state.selectedDateMillis
                    expiryPickerOpen = false
                    crossDateError = null
                    expiryError = null
                }) { Text("OK", color = DarkGreen) }
            },
            dismissButton = { TextButton(onClick = { expiryPickerOpen = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = state)
        }
    }
}

