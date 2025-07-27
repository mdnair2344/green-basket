//ShopProductsScreen
package com.igdtuw.greenbasket.ui.consumer // New package for consumer UI

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Re-use Crop data class (assuming it's in a shared/common package or accessible)
//import com.igdtuw.greenbasket.ui.producer.Crop // Adjust path if needed

// Data class to combine Crop with Producer info for display
data class DisplayCrop(
    val crop: Crop,
    val producerName: String,
    val farmName: String // Optional, but good for consistent info
)

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShopProductsScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var displayCrops by remember { mutableStateOf<List<DisplayCrop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val sharedViewModel: SharedViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        scope.launch(Dispatchers.IO) {
            try {
                // Step 1: Fetch all producer user documents to get their names and farm names
                val producerUsersSnapshot = db.collection("users")
                    .whereEqualTo("userType", "Producer")
                    .get().await()

                val producerInfoMap = producerUsersSnapshot.documents.associate { doc ->
                    val producerId = doc.id
                    val producerName = doc.getString("name") ?: "Unknown Producer"
                    val farmName = doc.getString("farmName") ?: "Unknown Farm"
                    producerId to (producerName to farmName)
                }

                // Step 2: Fetch all crops using a collection group query
                val cropsSnapshot = db.collectionGroup("crops").get().await()

                val fetchedCrops = cropsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Crop::class.java)?.copy(id = doc.id)
                }

                // Step 3: Combine crops with producer info
                val combinedCrops = mutableListOf<DisplayCrop>()
                for (crop in fetchedCrops) {
                    // REVISED Extraction: A more robust way to get producerId when `doc` is directly from collectionGroup
                    // Use `doc.reference.parent.parent?.id` to get the producer ID
                    val producerIdActual = cropsSnapshot.documents.first { it.id == crop.id }.reference.parent.parent?.id

                    if (producerIdActual != null) {
                        val (pName, fName) = producerInfoMap[producerIdActual] ?: ("Unknown Producer" to "Unknown Farm")
                        combinedCrops.add(DisplayCrop(crop, pName, fName))
                    } else {
                        // Handle cases where producerId cannot be extracted
                        combinedCrops.add(DisplayCrop(crop, "Unknown Producer", "Unknown Farm"))
                    }
                }
                displayCrops = combinedCrops
                isLoading = false

            } catch (e: Exception) {
                errorMessage = "Error loading crops: ${e.message}"
                isLoading = false
                with(Dispatchers.Main) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(topBar = @androidx.compose.runtime.Composable {
        TopAppBar(
            title = { Text("Available Products", color = Color.White, fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ConsumerPrimaryVariant,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White
            )
            ,
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
        Column(modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(paddingValues)
            .padding(16.dp)
        ) {



            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            } else if (displayCrops.isEmpty()) {
                Text("No crops found from any producer.", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(displayCrops) { displayCrop ->
                        ConsumerCropCard(displayCrop = displayCrop, navController)
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ConsumerCropCard(displayCrop: DisplayCrop, navController: NavController) {
    val cartViewModel: CartViewModel = hiltViewModel()
    val sharedViewModel: SharedViewModel = hiltViewModel()
    val context = LocalContext.current

    val wishlistItems by sharedViewModel.wishlistItems.collectAsState()
    val cartItems by sharedViewModel.cartItems.collectAsState()
    val consumerId by sharedViewModel.userId.collectAsState()

    val isWishlisted = wishlistItems.any { it.crop.id == displayCrop.crop.id }
    val cartItem = cartItems.find { it.product.id == displayCrop.crop.id }
    val isInCart = cartItem != null
    val quantity = cartItem?.quantity ?: 0

    val constructedCartItem = CartItem(
        product = CartableProductInfo(
            id = displayCrop.crop.id,
            name = displayCrop.crop.name,
            price = displayCrop.crop.pricePerKg.toDouble(),
            producerId = displayCrop.crop.producer
        ),
        quantity = quantity
    )

    Log.d("CropDebug", "producerId=${displayCrop.crop.producer}, cropId=${displayCrop.crop.id}")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (consumerId?.isNotBlank() ?: false) {
                    navController.navigate("product_details/${displayCrop.crop.producer}/${displayCrop.crop.id}")
                } else {
                    Toast.makeText(context, "User not signed in yet", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1FFF1)) // light green
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (!displayCrop.crop.imageUri.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(displayCrop.crop.imageUri),
                    contentDescription = "Image of ${displayCrop.crop.name}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayCrop.crop.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = ConsumerPrimaryVariant
                    )
                    Text(
                        text = "Variety: ${displayCrop.crop.variety}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Price: ₹${displayCrop.crop.pricePerKg}/kg, Qty: ${displayCrop.crop.quantity}kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ConsumerPrimaryVariant
                    )
                    Text(
                        text = "By: ${displayCrop.producerName} (${displayCrop.farmName})",
                        style = MaterialTheme.typography.bodySmall,
                        color = ConsumerPrimaryVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    IconButton(onClick = {
                        if (isWishlisted) {
                            sharedViewModel.removeFromWishlistWithUndo(displayCrop.crop.id)
                            Toast.makeText(context, "Removed from wishlist", Toast.LENGTH_SHORT).show()
                        } else {
                            sharedViewModel.addToWishlist(displayCrop.crop)
                            Toast.makeText(context, "Added to wishlist", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = ConsumerPrimaryVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!isInCart) {
                        Button(
                            onClick = {
                                sharedViewModel.addToCart(displayCrop.crop)
                                Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add to Cart", color = Color.White, fontSize = 12.sp)
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Button(
                                onClick = {
                                    sharedViewModel.decreaseQuantity(constructedCartItem)
                                },
                                enabled = quantity > 1,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (quantity > 1) Color(0xFF1B5E20) else Color.Gray
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("-", color = Color.White)
                            }

                            Text(
                                text = quantity.toString(),
                                fontSize = 14.sp,
                                modifier = Modifier.width(24.dp),
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    sharedViewModel.increaseQuantity(constructedCartItem)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("+", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
