//CartWishlistActions
package com.igdtuw.greenbasket.ui.consumer

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController



@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CartWishlistActions(
    navController: NavController?,
    @SuppressLint("ContextCastToActivity") sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val cartCount by sharedViewModel.cartCount.collectAsState()
    val wishlistItems by sharedViewModel.wishlistItems.collectAsState()

    val wishlistCount = wishlistItems.size

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(onClick = { navController?.navigate("wishlist") }) {
                Icon(
                    Icons.Default.Favorite, contentDescription = "Wishlist",
                    tint = Color.White
                )
            }
            AnimatedBadge(count = wishlistCount, color = Color.Red)
        }

        Box {
            IconButton(onClick = { navController?.navigate("my_cart") }) {
                Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
            }
            AnimatedBadge(count = cartCount, color = Color(0xFF388E3C))
        }
    }
}

@Composable
fun AnimatedBadge(count: Int, color: Color) {
    AnimatedVisibility(
        visible = count > 0,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Badge(
            modifier = Modifier.offset(x = 8.dp, y = (-4).dp),
            containerColor = color
        ) {
            Text(
                text = if (count > 99) "99+" else "$count",
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}
