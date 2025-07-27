//WishlistScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.igdtuw.greenbasket.ui.theme.*
import kotlinx.coroutines.tasks.await
import android.widget.Toast // Import Toast for messages
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext // Import LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

// IMPORTANT: For immediate fix, placing these data classes here.
// In your actual project, ensure these are defined ONCE in a central location
// (e.g., com.igdtuw.greenbasket.data package, or within SharedViewModel.kt)
// and then imported wherever needed.




@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val context = LocalContext.current // Get context for Toast messages
    val db = FirebaseFirestore.getInstance()
    val auth = Firebase.auth // Get FirebaseAuth instance

    // State for wishlist items and loading
    var wishlistItems by remember { mutableStateOf<List<WishlistedCrop>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    // Use current user's UID to re-trigger LaunchedEffect if user changes
    val currentUid = auth.currentUser?.uid

    LaunchedEffect(currentUid) {
        if (currentUid == null) {
            loading = false
            wishlistItems = emptyList() // Clear wishlist if no user
            Toast.makeText(context, "Please log in to view your wishlist.", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        // Reference to the user's wishlist in Firestore: consumers/{uid}/wishlist
        // Changed to "consumers/{uid}/wishlist" for consistency with cart
        val wishlistRef = db.collection("consumers").document(currentUid).collection("wishlist")

        loading = true
        try {
            val snapshot = wishlistRef.get().await()
            val fetchedItems = snapshot.documents.mapNotNull { doc ->
                // Make sure your Firestore document for a wishlisted item has a 'crop' field
                // that holds the entire Crop object as a map.
                try {
                    doc.toObject(WishlistedCrop::class.java)
                } catch (e: Exception) {
                    println("Error parsing WishlistedCrop from Firestore: ${e.message}")
                    null
                }
            }
            wishlistItems = fetchedItems
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error loading wishlist: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            loading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("My Wishlist", color = Color.White, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(paddingValues)
                .background(Color.White)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                wishlistItems.isEmpty() -> EmptyWishlistContent(navController = navController) // Pass navController
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wishlistItems, key = { it.crop.id }) { wishlistedCrop ->
                        WishlistItemCard(
                            product = wishlistedCrop.crop,
                            onAddToCart = {
                                // Add to cart
                                sharedViewModel.addToCart(wishlistedCrop.crop) // Default quantity of 1
                                Toast.makeText(context, "${wishlistedCrop.crop.name} added to cart!", Toast.LENGTH_SHORT).show()

                                // Remove from wishlist after adding to cart
                                if (currentUid != null) {
                                    // Use the sharedViewModel's removeFromWishlistWithUndo as it handles UI state
                                    sharedViewModel.removeFromWishlistWithUndo(wishlistedCrop.crop.id)
                                    // The real-time listener in sharedViewModel will update wishlistItems,
                                    // so no direct local state update for removal is needed here.
                                }
                            },
                            onRemoveFromWishlist = {
                                if (currentUid != null) {
                                    // Use the sharedViewModel's removeFromWishlistWithUndo as it handles UI state
                                    sharedViewModel.removeFromWishlistWithUndo(wishlistedCrop.crop.id)
                                    Toast.makeText(context, "${wishlistedCrop.crop.name} removed from wishlist.", Toast.LENGTH_SHORT).show()
                                    // The real-time listener in sharedViewModel will update wishlistItems,
                                    // so no direct local state update for removal is needed here.
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// REMOVED: This local removeFromWishlist function is no longer needed
// because sharedViewModel.removeFromWishlistWithUndo is used instead,
// which centralizes wishlist management and leverages real-time updates.
/*
fun removeFromWishlist(ref: CollectionReference, cropId: String, onSuccess: () -> Unit) {
    ref.document(cropId).delete()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e ->
            e.printStackTrace()
            // Optionally, show a Toast or Snackbar here for the user
            println("Error removing from wishlist: ${e.message}")
        }
}
*/

@Composable
fun WishlistItemCard(product: Crop, onAddToCart: () -> Unit, onRemoveFromWishlist: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImage(
                model = product.imageUri,
                contentDescription = product.name,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)) // Added clipping for better appearance
                    .background(Color.LightGray) // Placeholder background
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextColorDark
                )
                // Safely extract the unit from quantity string
                // Note: product.quantity is the total quantity available, not the unit for pricing
                // Assuming pricePerKg is already per "unit" (e.g., kg or piece)
                // You might need a `product.unit` field in your Crop data class for consistency.
                //val displayUnit = product.quantity.split(" ").getOrElse(1) { "unit" } // Example: "1 kg" -> "kg"
                Text(
                    text = "₹${product.pricePerKg}",// $displayUnit",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextColorDark.copy(alpha = 0.7f)
                )
            }
            Row {
                IconButton(onClick = onAddToCart) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Add to Cart", tint = ConsumerPrimaryVariant)
                }
                IconButton(onClick = onRemoveFromWishlist) {
                    Icon(Icons.Default.Close, contentDescription = "Remove from Wishlist", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun EmptyWishlistContent(navController: NavController) { // Pass navController to navigate
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart, // Use a relevant icon for empty state
            contentDescription = "Empty Wishlist",
            tint = Color.Gray.copy(alpha = 0.6f),
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Your wishlist is empty!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Start adding products you love.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { navController.navigate("shop_products") }, // Navigate to a product listing screen
            colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
        ) {
            Text("Explore Products", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}