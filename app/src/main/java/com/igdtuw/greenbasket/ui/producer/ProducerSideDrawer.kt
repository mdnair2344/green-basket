//ProducerSideDrawer
package com.igdtuw.greenbasket.ui.producer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant

@Composable
fun ProducerSideDrawer(
    onHomeClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onEarningsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCropCalendarClick: () -> Unit,
    onBulkOrderNotificationClick: () -> Unit,
    onAcceptRejectOrdersClick: () -> Unit,
    onLiveChatClick: () -> Unit,
    onRestockClick: () -> Unit,
    onNearbyProducersClick: () -> Unit,
    onUploadMediaClick: () -> Unit,
    onDiscountFeaturesClick: () -> Unit,
    onDemoGuidanceClick: () -> Unit,
    onReviewsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val uid = user?.uid
    var firestoreName by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<String?>(null) }

    // Fetch the name from Firestore
    LaunchedEffect(uid) {
        uid?.let {
            val db = FirebaseFirestore.getInstance()
            try {
                val doc = db.collection("users").document(uid).get().await()
                firestoreName = doc.getString("name") ?: "Producer"
                imageUri = doc.getString("imageUri")
            } catch (e: Exception) {
                firestoreName = "Producer"
                imageUri = null
            }
        }
    }

    val displayName = firestoreName ?: "Producer"

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.White)
            .padding(vertical = 16.dp)
    ) {
        // Profile Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onProfileClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!imageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ConsumerPrimaryVariant.copy(alpha = 0.1f)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default Profile Picture",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape),
                            //.background(ConsumerPrimaryVariant.copy(alpha = 0.1f))
                            //.padding(8.dp),
                        tint = ConsumerPrimaryVariant
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Hello,",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = displayName,
                        fontSize = 18.sp,
                        color = ConsumerPrimaryVariant
                    )
                }
            }
        }

        // Divider
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Sidebar items
        item {
            ProducerSidebarItem(Icons.Default.Home, "Dashboard", onHomeClick)
            ProducerSidebarItem(Icons.Default.Inventory, "Inventory", onInventoryClick)
            ProducerSidebarItem(Icons.Default.Notifications, "Bulk Orders", onBulkOrderNotificationClick)
            ProducerSidebarItem(Icons.Default.LocalShipping, "Accept/Reject Orders", onAcceptRejectOrdersClick)
            ProducerSidebarItem(Icons.Default.Chat, "Live Chat", onLiveChatClick)
            ProducerSidebarItem(Icons.Default.Restore, "Restock", onRestockClick)
            ProducerSidebarItem(Icons.Default.CalendarToday, "Crop Stage", onCropCalendarClick)
            ProducerSidebarItem(Icons.Default.LocationOn, "Nearby Producers", onNearbyProducersClick)
            ProducerSidebarItem(Icons.Default.UploadFile, "Upload Certificates", onUploadMediaClick)
            ProducerSidebarItem(Icons.Default.LocalOffer, "Discount Features", onDiscountFeaturesClick)
            ProducerSidebarItem(Icons.Default.Slideshow, "Demo Guidance Video", onDemoGuidanceClick)
            ProducerSidebarItem(Icons.Default.Star, "Reviews and Ratings", onReviewsClick)
            ProducerSidebarItem(Icons.Default.AttachMoney, "Earnings", onEarningsClick)
            ProducerSidebarItem(Icons.Default.Settings, "Settings", onSettingsClick)
            ProducerSidebarItem(Icons.Default.Logout, "Logout", onLogoutClick)
        }
        item{Spacer(modifier = Modifier.width(50.dp))}
    }

}

@Composable
fun ProducerSidebarItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = ConsumerPrimaryVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, fontSize = 16.sp, color = ConsumerPrimaryVariant)
    }
}

@Preview(showBackground = true, widthDp = 320, heightDp = 800)
@Composable
fun PreviewProducerSideDrawer() {
    ProducerSideDrawer(
        onHomeClick = {},
        onInventoryClick = {},
        onOrdersClick = {},
        onEarningsClick = {},
        onSettingsClick = {},
        onCropCalendarClick = {},
        onBulkOrderNotificationClick = {},
        onAcceptRejectOrdersClick = {},
        onLiveChatClick = {},
        onRestockClick = {},
        onNearbyProducersClick = {},
        onUploadMediaClick = {},
        onDiscountFeaturesClick = {},
        onDemoGuidanceClick = {},
        onReviewsClick = {},
        onLogoutClick = {},
        onProfileClick = {}
    )
}
