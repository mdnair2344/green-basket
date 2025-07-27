//ConsumerCertificatesScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant

import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Data classes (Certificate, UserProducerInfo, CertificateWithProducer) are in the same package
// (com.igdtuw.greenbasket.ui.theme.consumer), so explicit imports are not needed here.

// IMPORTANT: Ensure your 'Certificate' data class (likely in CommonDataClasses.kt
// within this same package) includes the 'category' field if your Firestore
// documents have it, to avoid parsing issues.
// Example: data class Certificate(..., val category: String = "")


@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerCertificatesScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val sharedViewModel: SharedViewModel = hiltViewModel()
    var certificatesWithProducer by remember { mutableStateOf<List<CertificateWithProducer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            Log.d("CertScreen", "Starting certificate fetch using collectionGroup.")

            // 1. Fetch all producer user documents to get their names and farm names
            // This is still necessary to associate certificates with producer info
            val producerUsersSnapshot = db.collection("users")
                .whereEqualTo("userType", "Producer")
                .get().await()
            Log.d("CertScreen", "Users collection fetched: ${producerUsersSnapshot.documents.size} documents.")
            if (producerUsersSnapshot.isEmpty) {
                Log.d("CertScreen", "No documents found in 'users' collection or no 'Producer' users.")
            }

            val producerInfoMap = producerUsersSnapshot.documents.associate { doc ->
                val producerId = doc.id
                val producerName = doc.getString("name") ?: "Unknown Producer"
                val farmName = doc.getString("farmName") ?: "Unknown Farm"
                producerId to (producerName to farmName)
            }
            Log.d("CertScreen", "Mapped producer users from 'users' collection: ${producerInfoMap.size} entries.")


            // 2. Fetch all certificates using a collection group query
            val certificatesSnapshot = db.collectionGroup("certificates").get().await()
            Log.d("CertScreen", "CollectionGroup 'certificates' fetched: ${certificatesSnapshot.documents.size} documents.")
            if (certificatesSnapshot.isEmpty) {
                Log.d("CertScreen", "No documents found in 'certificates' collection group.")
            }

            val allCertificates = mutableListOf<CertificateWithProducer>()

            // 3. Combine certificates with producer info
            for (certDoc in certificatesSnapshot.documents) {
                val certificate = certDoc.toObject(Certificate::class.java)?.copy(id = certDoc.id)
                if (certificate != null) {
                    // Extract producerId from the document reference path
                    // The path is typically "producers/PRODUCER_ID/certificates/CERTIFICATE_ID"
                    // So, parent.parent.id should give the PRODUCER_ID
                    val producerIdActual = certDoc.reference.parent?.parent?.id

                    if (producerIdActual != null) {
                        val (pName, fName) = producerInfoMap[producerIdActual] ?: ("Unknown Producer" to "Unknown Farm")
                        allCertificates.add(
                            CertificateWithProducer(
                                certificate = certificate,
                                producerName = pName,
                                farmName = fName
                            )
                        )
                        Log.d("CertScreen", "Added certificate: '${certificate.name}' (ID: ${certificate.id}) for producer '${pName}'.")
                    } else {
                        Log.w("CertScreen", "Could not extract producerId from document reference for certificate ${certDoc.id}. Skipping.")
                        // Add with unknown producer info if ID cannot be extracted
                        allCertificates.add(
                            CertificateWithProducer(
                                certificate = certificate,
                                producerName = "Unknown Producer",
                                farmName = "Unknown Farm"
                            )
                        )
                    }
                } else {
                    Log.w("CertScreen", "Failed to parse certificate document ${certDoc.id}. Check Certificate data class fields.")
                }
            }
            certificatesWithProducer = allCertificates.toList()
            isLoading = false
            Log.d("CertScreen", "Total certificates fetched and processed: ${certificatesWithProducer.size}")
            if (certificatesWithProducer.isEmpty()) {
                Log.d("CertScreen", "Final result: No certificates to display after all processing.")
            }

        } catch (e: Exception) {
            errorMessage = "Error fetching certificates: ${e.message}"
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            isLoading = false
            Log.e("CertScreen", "Error during certificate fetch: ${e.message}", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Producer Certificates", color = Color.White, fontWeight = FontWeight.Bold) }, // Title can be adjusted
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel) // Pass sharedViewModel
                }
            )
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text("Loading certificates...", modifier = Modifier.padding(top = 16.dp))
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                Button(onClick = {
                    Log.d("CertScreen", "Retry button clicked.")
                    // This will re-trigger the LaunchedEffect
                    isLoading = true // Setting isLoading to true will cause recomposition and re-run LaunchedEffect
                    errorMessage = null // Clear previous error
                }) {
                    Text("Retry")
                }
            } else if (certificatesWithProducer.isEmpty()) {
                Text(
                    text = "No certificates available at the moment.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(certificatesWithProducer, key = { it.certificate.id }) { data ->
                        CertificateCardForConsumer(data = data)
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CertificateCardForConsumer(data: CertificateWithProducer) {
    val certificate = data.certificate
    val producerName = data.producerName
    val farmName = data.farmName

    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }

    val parsedIssueDate: LocalDate? = remember(certificate.issueDate) {
        try {
            LocalDate.parse(certificate.issueDate, dateFormatter)
        } catch (e: DateTimeParseException) {
            Log.e("CertCard", "Error parsing issue date: '${certificate.issueDate}' for certificate ID: ${certificate.id}", e)
            null
        }
    }

    val parsedExpiryDate: LocalDate? = remember(certificate.expiryDate) {
        try {
            LocalDate.parse(certificate.expiryDate, dateFormatter)
        } catch (e: DateTimeParseException) {
            Log.e("CertCard", "Error parsing expiry date: '${certificate.expiryDate}' for certificate ID: ${certificate.id}", e)
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$producerName - $farmName",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (certificate.certificateUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(certificate.certificateUrl),
                    contentDescription = "Certificate image for ${certificate.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No image available",
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = "Certificate: ${certificate.name}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Issued by: ${certificate.authority}", style = MaterialTheme.typography.bodyMedium)

            if (parsedIssueDate != null) {
                Text(text = "Issue Date: ${parsedIssueDate.format(dateFormatter)}", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = "Issue Date: ${certificate.issueDate} (Invalid Format)", style = MaterialTheme.typography.bodySmall, color = Color.Red)
            }

            if (parsedExpiryDate != null) {
                Text(text = "Expiry Date: ${parsedExpiryDate.format(dateFormatter)}", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(text = "Expiry Date: ${certificate.expiryDate} (Invalid Format)", style = MaterialTheme.typography.bodySmall, color = Color.Red)
            }

            Text(
                text = "Status: ${certificate.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (certificate.status == "Valid") ConsumerPrimaryVariant else Color.Red
            )

            if (certificate.description.isNotBlank()) {
                Text(text = "Description: ${certificate.description}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}