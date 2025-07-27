//LiveChatScreen
package com.igdtuw.greenbasket.ui.producer


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme

data class Consumer(
    val name: String,
    val itemsBought: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveChatScreen(consumers: List<Consumer>, onBack: () -> Unit = {}) {
    var selectedConsumer by remember { mutableStateOf<Consumer?>(null) }
    var chatInput by remember { mutableStateOf(TextFieldValue("")) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Live Chat",
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
        },
        containerColor = Color.White
    ) { paddingValues ->
        if (consumers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No Consumers in Your Chat List", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = paddingValues,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(12.dp)
            ) {
                items(consumers) { consumer ->
                    ConsumerCard(
                        consumer = consumer,
                        onChatClick = { selectedConsumer = consumer },
                        onCallClick = { /* Handle audio call */ },
                        onVideoClick = { /* Handle video call */ }
                    )
                }
            }
        }
    }
}

@Composable
fun ConsumerCard(
    consumer: Consumer,
    onChatClick: () -> Unit,
    onCallClick: () -> Unit,
    onVideoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Reviewer Avatar",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = consumer.name,
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.Black)
                    )
                    Text(
                        text = "Purchased: ${consumer.itemsBought.joinToString()}",
                        color = Color(0xFF1B5E20),
                        fontSize = 14.sp
                    )
                }
            }

            Row {
                IconButton(onClick = onCallClick) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint=Color.Black)
                }
                IconButton(onClick = onVideoClick) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video", tint=Color.Black)
                }
                IconButton(onClick = onChatClick) {
                    Icon(Icons.Default.Message, contentDescription = "Chat", tint=Color.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
    selectedConsumer: Consumer,
    chatInput: TextFieldValue,
    onInputChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = chatInput,
            onValueChange = onInputChange,
            placeholder = { Text("Message ${selectedConsumer.name}...") },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )

        )
        IconButton(onClick = onSend) {
            Icon(Icons.Default.Send, contentDescription = "Send")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun LiveChatScreenPreview() {
    GreenBasketTheme(content = {
        LiveChatScreen(
            consumers = listOf(
                Consumer("Riya", listOf("Tomatoes", "Spinach")),
                Consumer("Amit", listOf("Wheat"))
            )
        )
    }, isProducer = false)
}
