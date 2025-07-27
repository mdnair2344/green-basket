//ConsumerHomeScreen
package com.igdtuw.greenbasket.ui.consumer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.igdtuw.greenbasket.R
import com.igdtuw.greenbasket.ui.theme.*

data class ConsumerFeature(
    val id: String,
    val text: String,
    val icon: Any,
    val isImageVector: Boolean,
    val backgroundColor: Color,
    val iconColor: Color,
    val onClick: () -> Unit = {}
)

@Composable
fun ConsumerHomeScreen(navController: NavController) {
    val features = listOf(
        ConsumerFeature(
            id = "fruits", text = "Shop Fruits",
            icon = Icons.Default.LocalGroceryStore, isImageVector = true,
            backgroundColor = ConsumerCardBackground1, iconColor = TextColorDark,
            onClick = { navController.navigate("shop_category_screen/Fruits") }
        ),
        ConsumerFeature(
            id = "vegetables", text = "Shop Vegetables",
            icon = Icons.Default.LocalFlorist, isImageVector = true,
            backgroundColor = ConsumerCardBackground2, iconColor = TextColorDark,
            onClick = { navController.navigate("shop_category_screen/Vegetables") }
        ),
        ConsumerFeature(
            id = "grains", text = "Shop Grains",
            icon = Icons.Default.Spa, isImageVector = true,
            backgroundColor = ConsumerCardBackground3, iconColor = TextColorDark,
            onClick = { navController.navigate("shop_category_screen/Grains") }
        ),
        ConsumerFeature(
            id = "dairy", text = "Shop Dairy Products",
            icon = Icons.Default.LocalDining, isImageVector = true,
            backgroundColor = ConsumerCardBackground4, iconColor = TextColorDark,
            onClick = { navController.navigate("shop_category_screen/Dairy") }
        ),
        ConsumerFeature(
            id = "pulses", text = "Shop Pulses",
            icon = Icons.Default.EmojiNature, // Or use RiceBowl if you add your own
            isImageVector = true,
            backgroundColor = ConsumerCardBackground5, iconColor = TextColorDark,
            onClick = { navController.navigate("shop_category_screen/Pulses") }
        ),
        ConsumerFeature(
            id = "reviews", text = "My Reviews",
            icon = Icons.Default.Star, isImageVector = true,
            backgroundColor = ConsumerCardBackground1, iconColor = TextColorDark,
            onClick = {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                uid?.let {
                    navController.navigate("my_reviews_screen/$it")
                }
            }
        ),
        ConsumerFeature(
            id = "deals", text = "Deals & Offers",
            icon = Icons.Default.LocalOffer, isImageVector = true,
            backgroundColor = ConsumerCardBackground2, iconColor = TextColorDark,
            onClick = { navController.navigate("deals_screen") }
        ),
        ConsumerFeature(
            id = "refunds", text = "My Refunds",
            icon = Icons.AutoMirrored.Filled.ReceiptLong, isImageVector = true,
            backgroundColor = ConsumerCardBackground3, iconColor = Color.White,
            onClick = { navController.navigate("refunds_screen") }
        ),
        ConsumerFeature(
            id = "cctv", text = "Live CCTV Feed",
            icon = Icons.Default.CameraAlt, isImageVector = true,
            backgroundColor = ConsumerCardBackground4, iconColor = TextColorDark,
            onClick = { navController.navigate("live_cctv_list") }
        ),
        ConsumerFeature(
            id = "certifications", text = "Certifications",
            icon = Icons.Default.VerifiedUser, isImageVector = true,
            backgroundColor = ConsumerCardBackground5, iconColor = Color.White,
            onClick = { navController.navigate("certifications_list") }
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(features) { feature ->
            if (feature.isImageVector) {
                ConsumerFeatureCard(
                    imageVectorIcon = feature.icon as ImageVector,
                    text = feature.text,
                    backgroundColor = feature.backgroundColor,
                    iconColor = feature.iconColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    onClick = feature.onClick
                )
            } else {
                ConsumerFeatureCard(
                    painterIcon = feature.icon as androidx.compose.ui.graphics.painter.Painter,
                    text = feature.text,
                    backgroundColor = feature.backgroundColor,
                    iconColor = feature.iconColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    onClick = feature.onClick
                )
            }
        }
    }
}

@Composable
fun ConsumerFeatureCard(
    imageVectorIcon: ImageVector,
    text: String,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ConsumerFeatureCardInternal(
        icon = imageVectorIcon,
        isImageVector = true,
        text = text,
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun ConsumerFeatureCard(
    painterIcon: androidx.compose.ui.graphics.painter.Painter,
    text: String,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ConsumerFeatureCardInternal(
        icon = painterIcon,
        isImageVector = false,
        text = text,
        backgroundColor = backgroundColor,
        iconColor = iconColor,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun ConsumerFeatureCardInternal(
    icon: Any,
    isImageVector: Boolean,
    text: String,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isImageVector) {
                    Icon(
                        imageVector = icon as ImageVector,
                        contentDescription = text,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Icon(
                        painter = icon as androidx.compose.ui.graphics.painter.Painter,
                        contentDescription = text,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    color = TextColorDark
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Navigate",
                tint = TextColorDark.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
