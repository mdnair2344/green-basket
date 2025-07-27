// ConsumerSideDrawer.kt
package com.igdtuw.greenbasket.ui.consumer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage


object Routes {
    const val Dashboard = "consumer_dashboard"
    const val ShopProducts = "shop_products"
    const val BulkOrders = "bulk_event_order"
    const val MyOrders = "my_orders"
    const val MyCart = "my_cart"
    const val Wishlist = "wishlist"
    const val MyReviews = "my_reviews_screen"
    const val Chat = "chat_with_producers"
    const val Deals = "deals_screen"
    const val Refunds = "refunds_screen"
    const val Help = "help"
    const val Settings = "settings"
    const val Profile = "profile"
    const val Logout = "logout"
}

@Composable
fun ConsumerSideDrawer(
    navController: NavController,
    closeDrawer: () -> Unit
) {
    val user = remember { FirebaseAuth.getInstance().currentUser }
    val uid = user?.uid
    var firestoreName by remember { mutableStateOf<String?>(null) }
    var imageUri by remember { mutableStateOf<String?>(null) }
    // Fetch name from Firestore
    LaunchedEffect(uid) {
        uid?.let {
            val db = FirebaseFirestore.getInstance()
            try {
                val doc = db.collection("users").document(uid).get().await()
                firestoreName = doc.getString("name") ?: "User"
                imageUri = doc.getString("imageUri")
            } catch (e: Exception) {
                firestoreName = "User"
                imageUri = null
            }
        }
    }

    val displayName = firestoreName ?: "User"

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {
        // 👇 Clickable profile section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate(Routes.Profile) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    closeDrawer()
                }
                .padding(vertical = 24.dp),
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
                        .clip(CircleShape)
                        .background(ConsumerPrimaryVariant.copy(alpha = 0.1f))
                        .padding(8.dp),
                    tint = ConsumerPrimaryVariant
                )
            }



            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "Welcome,",
                    fontWeight = FontWeight.Normal,
                    color = ConsumerPrimaryVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = displayName,
                    fontWeight = FontWeight.Bold,
                    color = ConsumerPrimaryVariant,
                    fontSize = MaterialTheme.typography.headlineSmall.fontSize
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Menu items
        DrawerMenuItem(Icons.Default.Home, "Dashboard") {
            navController.navigate("HomeScreen")
            closeDrawer()
        }

        DrawerMenuItem(Icons.Default.ShoppingBasket, "Shop Products") {
            navController.navigate(Routes.ShopProducts)
            closeDrawer()
        }

        DrawerMenuItem(Icons.Default.Event, "Bulk Event Orders") {
            navController.navigate(Routes.BulkOrders)
            closeDrawer()
        }

        DrawerMenuItem(Icons.AutoMirrored.Filled.ListAlt, "My Orders") {
            navController.navigate(Routes.MyOrders)
            closeDrawer()
        }

        DrawerMenuItem(Icons.Default.ShoppingCart, "My Cart") {
            navController.navigate(Routes.MyCart)
            closeDrawer()
        }

        DrawerMenuItem(Icons.Default.Favorite, "Wishlist") {
            navController.navigate(Routes.Wishlist)
            closeDrawer()
        }

        DrawerMenuItem(Icons.Default.Star, "My Reviews") {
            navController.navigate("my_reviews_screen/$uid")

            closeDrawer()
        }

        DrawerMenuItem(Icons.AutoMirrored.Filled.Chat, "Chat with Producers") {
            navController.navigate(Routes.Chat)
            closeDrawer()
        }

        DrawerMenuItem(Icons.Default.LocalOffer, "Deals") {
            navController.navigate(Routes.Deals)
            closeDrawer()
        }

        DrawerMenuItem(Icons.AutoMirrored.Filled.ReceiptLong, "Refunds") {
            navController.navigate(Routes.Refunds)
            closeDrawer()
        }

        DrawerMenuItem(Icons.AutoMirrored.Filled.HelpOutline, "Help") {
            navController.navigate(Routes.Help)
            closeDrawer()
        }

        DrawerMenuItem(Icons.Default.Settings, "Settings") {
            navController.navigate(Routes.Settings)
            closeDrawer()
        }

        DrawerMenuItem(Icons.AutoMirrored.Filled.Logout, "Logout") {
            navController.navigate("logout")
            closeDrawer()
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}


@Composable
fun DrawerMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$text icon",
            tint = ConsumerPrimaryVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            color = ConsumerPrimaryVariant,
            fontWeight = FontWeight.Medium,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ConsumerSideDrawerPreview() {
    GreenBasketTheme(isProducer = false) {
        val navController = rememberNavController()
        ConsumerSideDrawer(navController = navController, closeDrawer = {})
    }
}
