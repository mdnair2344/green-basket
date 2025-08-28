package com.igdtuw.greenbasket.ui.producer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import com.igdtuw.greenbasket.ui.consumer.SharedViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.igdtuw.greenbasket.ui.consumer.CartWishlistActions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.igdtuw.greenbasket.NavControllerHolder.navController
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Consumer(
    val name: String = "",
    val phone: String = "",
    val imageUri: String = ""
)

fun makePhoneCall2(context: Context, phoneNumber: String) {
    val cleanNumber = phoneNumber.filter { it.isDigit() }
    val callIntent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$cleanNumber")
    }
    context.startActivity(callIntent)
}

fun startVideoCall2(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://wa.me/$phone")
    }
    context.startActivity(intent)
}

fun openChatOrSms2(context: Context, phone: String, consumerName: String) {
    val cleanPhone = phone.filter { it.isDigit() }
    val prefill = "Hi $consumerName, I'm your producer from GreenBasket. Let’s connect."

    val waUri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(prefill)}")
    val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
        setPackage("com.whatsapp")
    }

    try {
        if (waIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(waIntent)
        } else {
            val smsUri = Uri.parse("smsto:$cleanPhone")
            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", prefill)
            }
            if (smsIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(smsIntent)
            } else {
                Toast.makeText(context, "No messaging app available", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open chat: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerChatsScreen(onBack: () -> Unit = {}) {
    val firestore = Firebase.firestore
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var consumerList by remember { mutableStateOf<List<Consumer>>(emptyList()) }
    val currentProducerId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(Unit) {
        scope.launch {
            if (currentProducerId != null) {
                val consumerIds = mutableSetOf<String>()
                val ordersSnapshot = firestore.collection("orders")
                    .whereEqualTo("producerId", currentProducerId)
                    .get().await()

                for (orderDoc in ordersSnapshot.documents) {
                    val consumerId = orderDoc.getString("userId")
                    if (!consumerId.isNullOrEmpty()) {
                        consumerIds.add(consumerId)
                    }
                }

                val consumers = mutableListOf<Consumer>()
                for (consumerId in consumerIds) {
                    val userSnapshot = firestore.collection("users").document(consumerId).get().await()
                    if (userSnapshot.exists()) {
                        consumers.add(
                            Consumer(
                                name = userSnapshot.getString("name") ?: "N/A",
                                phone = userSnapshot.getString("phone") ?: "N/A",
                                imageUri = userSnapshot.getString("imageUri") ?: ""
                            )
                        )
                    }
                }
                consumerList = consumers
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chat with Consumers",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ConsumerPrimaryVariant)
            )
        },
        containerColor = Color.White
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(consumerList) { consumer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (consumer.imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = consumer.imageUri,
                                    contentDescription = "Consumer Image",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray, shape = CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Default Profile",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32), shape = CircleShape)
                                        .padding(6.dp),
                                    tint = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(consumer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Phone: ${consumer.phone}", fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = { makePhoneCall2(context, consumer.phone) }) {
                                Icon(Icons.Default.Call, contentDescription = "Call")
                            }
                            IconButton(onClick = { startVideoCall2(context, consumer.phone) }) {
                                Icon(Icons.Default.VideoCall, contentDescription = "Video Call")
                            }
                            IconButton(onClick = {
                                openChatOrSms2(context, consumer.phone, consumer.name)
                            }) {
                                Icon(Icons.Default.Chat, contentDescription = "Chat")
                            }
                        }
                    }
                }
            }
        }
    }
}