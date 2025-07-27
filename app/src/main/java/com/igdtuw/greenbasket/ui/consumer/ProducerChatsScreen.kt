//ProducerChatsScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight // For bold text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController // Import NavController
import androidx.navigation.compose.rememberNavController // For Preview
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant // Your custom green color

// Data model for each chat with producer
data class ProducerChat(
    val producerId: String,
    val producerName: String,
    val lastMessage: String,
    val lastMessageTime: String // Example: "10:45 AM" or "Yesterday"
)

// Sample data (moved to global scope for easy access or can be passed)
val sampleProducerChats = listOf(
    ProducerChat("p1", "John's Farm", "Thanks for your order!", "10:45 AM"),
    ProducerChat("p2", "Green Valley", "I'll update stock tomorrow.", "Yesterday"),
    ProducerChat("p3", "Organic Orchard", "New apples available!", "2 days ago")
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProducerChatsScreen(
    navController: NavController? = null, // Added navController as nullable
    pastChats: List<ProducerChat> = sampleProducerChats, // Default for preview/initial use
    onOpenChat: (ProducerChat) -> Unit = {}, // Default empty lambda
    onNewChatClick: () -> Unit = {}, // Default empty lambda
    onBackClick: () -> Unit = { navController?.popBackStack() } // Added onBackClick with safe call
) {
    val sharedViewModel: SharedViewModel = hiltViewModel()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with Producers", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel) // Pass sharedViewModel
                }
            )
        }, containerColor = Color.White,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = ConsumerPrimaryVariant // Use your custom green color
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat", tint = Color.White) // FAB icon white
            }
        }
    ) { paddingValues ->
        if (pastChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No chats yet. Start a new conversation!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(pastChats) { chat ->
                    ProducerChatItem(chat = chat, onClick = { onOpenChat(chat) })
                    Divider(color = Color.LightGray) // Add a subtle divider color
                }
            }
        }
    }
}

@Composable
fun ProducerChatItem(chat: ProducerChat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(ConsumerPrimaryVariant, shape = CircleShape), // Use your custom green color
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.producerName.take(1).uppercase(),
                color = Color.White, // Text on green background should be white
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = chat.producerName, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1B5E20)) // Dark green for name
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.DarkGray // Slightly darker for messages
            )
        }

        Text(
            text = chat.lastMessageTime,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ----------- Sample Preview ---------------

@Preview(showBackground = true, showSystemUi = true) // showSystemUi to see top bar properly
@Composable
fun ProducerChatsScreenPreview() {
    ProducerChatsScreen(
        navController = rememberNavController(), // Provide a mock NavController for preview
        pastChats = sampleProducerChats,
        onOpenChat = {},
        onNewChatClick = {}
    )
}