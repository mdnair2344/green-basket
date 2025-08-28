package com.igdtuw.greenbasket.ui.producer

import android.app.Person
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.launch


data class Producer(
    val uid: String = "",
    val name: String = "",
    val farmName: String = "",
    val mobile: String = "",
    val imageUri: String = ""
)

sealed interface ProducersUiState {
    object Loading : ProducersUiState
    data class Success(val producers: List<Producer>) : ProducersUiState
    data class Error(val message: String) : ProducersUiState
}

@Composable
private fun rememberNearbyProducersState(): State<ProducersUiState> {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUserId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
    val state = remember { mutableStateOf<ProducersUiState>(ProducersUiState.Loading) }
    DisposableEffect(Unit) {
        val listener = firestore.collection("users")
            .whereEqualTo("userType", "Producer")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    state.value = ProducersUiState.Error(error.message ?: "Unknown error")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val uid = doc.getString("uid") ?: doc.id
                            if (uid == currentUserId) return@mapNotNull null // exclude current producer
                            Producer(
                                uid = doc.getString("uid") ?: doc.id,
                                name = doc.getString("name") ?: "Unknown",
                                farmName = doc.getString("farmName") ?: "",
                                mobile = doc.getString("phone") ?: "",
                                imageUri = doc.getString("imageUri") ?: ""
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    state.value = ProducersUiState.Success(list)
                } else {
                    state.value = ProducersUiState.Error("No data")
                }
            }
        onDispose { listener.remove() }
    }
    return state
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyProducersScreen(
    onBack: () -> Unit = {}
) {
    val uiState by rememberNearbyProducersState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Community Network",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack, // fixed reference
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant
                )
            )
        },
        containerColor = Color.White,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        when (uiState) {
            is ProducersUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ProducersUiState.Error -> {
                val msg = (uiState as ProducersUiState.Error).message
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error loading producers: $msg", color = Color.Red)
                }
            }

            is ProducersUiState.Success -> {
                val producers = (uiState as ProducersUiState.Success).producers
                if (producers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No Nearby Producers Found", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .padding(padding)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(producers, key = { it.uid }) { producer ->
                            ProducerCard(
                                producer = producer,
                                onCallClick = {
                                    val phone = producer.mobile.takeIf { it.isNotBlank() }
                                    if (!phone.isNullOrBlank()) {
                                        val intent = Intent(
                                            Intent.ACTION_DIAL,
                                            Uri.parse("tel:$phone")
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        Toast.makeText(context, "Phone number missing", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                },
                                onVideoClick = {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Video call feature under development")
                                    }
                                },
                                onChatClick = {
                                    if (producer.mobile.isNotBlank()) {
                                        openChatOrSms(context, producer.mobile, producer.name)
                                    } else {
                                        Toast.makeText(context, "Phone number missing", Toast.LENGTH_SHORT).show()
                                    }
                                }

                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProducerCard(
    producer: Producer,
    onCallClick: () -> Unit,
    onVideoClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.cardElevation(6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // Profile image or fallback
                if (producer.imageUri.isNotBlank()) {
                    AsyncImage(
                        model = producer.imageUri,
                        contentDescription = "Producer profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray, shape = CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Default avatar",
                        tint = Color.White,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32), shape = CircleShape)
                            .padding(6.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = producer.name,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.Black)
                    )
                    if (producer.farmName.isNotBlank()) {
                        Text("Farm: ${producer.farmName}", fontSize = 14.sp, color = Color(0xFF1B5E20))
                    }
                    if (producer.mobile.isNotBlank()) {
                        Text("Mobile: ${producer.mobile}", fontSize = 13.sp, color = Color.Black)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCallClick) {
                    Icon(Icons.Filled.Call, contentDescription = "Call", tint = Color.Black)
                }
                IconButton(onClick = onVideoClick) {
                    Icon(Icons.Filled.Videocam, contentDescription = "Video", tint = Color.Black)
                }
                IconButton(onClick = onChatClick) {
                    Icon(Icons.Filled.Message, contentDescription = "Chat", tint = Color.Black)
                }
            }
        }
    }
}


private fun openChatOrSms(context: android.content.Context, phone: String, producerName: String) {
    val cleanPhone = phone.filter { it.isDigit() } // ensure digits, include country code like "91..."
    val prefill = "Hi $producerName, I'm interested in discussing about your product."
    try {
        // Try WhatsApp first
        val waUri = android.net.Uri.parse("https://wa.me/$cleanPhone?text=${android.net.Uri.encode(prefill)}")
        val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
            setPackage("com.whatsapp")
        }
        if (waIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(waIntent)
            return
        }
        // Fallback to SMS
        val smsUri = android.net.Uri.parse("sms:$cleanPhone")
        val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", prefill)
        }
        if (smsIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(smsIntent)
        } else {
            Toast.makeText(context, "No messaging app available", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open chat: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}