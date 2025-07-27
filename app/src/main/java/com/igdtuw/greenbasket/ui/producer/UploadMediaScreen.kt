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

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var authority by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        fileUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Producer Certificate") },
        text = {
            Column {
                fun modifier() = Modifier
                    .fillMaxWidth()
                val colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkGreen,
                    cursorColor = DarkGreen,
                    focusedLabelColor = DarkGreen
                )

                OutlinedTextField(name, { name = it }, label = { Text("Certificate Name") }, modifier = modifier(), colors = colors)
                OutlinedTextField(description, { description = it }, label = { Text("Description (Optional)") }, modifier = modifier(), colors = colors)
                OutlinedTextField(category, { category = it }, label = { Text("Category (e.g., Organic, ISO)") }, modifier = modifier(), colors = colors)
                OutlinedTextField(issueDate, { issueDate = it }, label = { Text("Issue Date (YYYY-MM-DD)") }, modifier = modifier(), colors = colors)
                OutlinedTextField(expiryDate, { expiryDate = it }, label = { Text("Expiry Date (YYYY-MM-DD)") }, modifier = modifier(), colors = colors)
                OutlinedTextField(authority, { authority = it }, label = { Text("Issued By Authority") }, modifier = modifier(), colors = colors)

                Button(
                    onClick = { launcher.launch(arrayOf("application/pdf", "image/*")) },
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkGreen, contentColor = Color.White)
                ) {
                    Text("Select Certificate File")
                }

                fileUri?.let {
                    Text("Selected: ${it.lastPathSegment ?: "File"}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileUri == null || name.isBlank() || issueDate.isBlank() || expiryDate.isBlank() || authority.isBlank() || category.isBlank()) {
                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val issue = runCatching { LocalDate.parse(issueDate, formatter) }.getOrNull()
                    val expiry = runCatching { LocalDate.parse(expiryDate, formatter) }.getOrNull()

                    if (issue == null || expiry == null) {
                        Toast.makeText(context, "Invalid date format. Use YYYY-MM-DD.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val status = if (expiry.isAfter(LocalDate.now())) "Valid" else "Expired"
                    val certData = Certificate(name, description, issueDate, expiryDate, authority, status, category = category)

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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
