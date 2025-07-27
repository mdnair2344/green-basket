//ShopCategoryScreen
package com.igdtuw.greenbasket.ui.consumer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material.icons.filled.* // Import all needed icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.tasks.await

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "fruits" -> Icons.Default.Eco
        "vegetables" -> Icons.Default.LocalFlorist
        "grains" -> Icons.Default.Spa
        "dairy" -> Icons.Default.LocalDining
        "herbs" -> Icons.Default.Grass
        "organic" -> Icons.Default.EnergySavingsLeaf
        else -> Icons.Default.ShoppingCart
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ShopCategoryScreen(
    category: String,
    navController: NavController
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var displayCrops by remember { mutableStateOf<List<DisplayCrop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val sharedViewModel: SharedViewModel = hiltViewModel()

    LaunchedEffect(category) {
        isLoading = true
        errorMessage = null
        scope.launch(Dispatchers.IO) {
            try {
                // Step 1: Fetch all producer user documents
                val producerUsersSnapshot = db.collection("users")
                    .whereEqualTo("userType", "Producer")
                    .get().await()

                val producerInfoMap = producerUsersSnapshot.documents.associate { doc ->
                    val producerId = doc.id
                    val producerName = doc.getString("name") ?: "Unknown Producer"
                    val farmName = doc.getString("farmName") ?: "Unknown Farm"
                    producerId to (producerName to farmName)
                }

                // Step 2: Fetch all crops using collectionGroup query
                val cropsSnapshot = db.collectionGroup("crops").get().await()

                val fetchedCrops = cropsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Crop::class.java)?.takeIf {
                        it.category.trim().equals(category.trim(), ignoreCase = true)
                    }?.copy(id = doc.id)
                }

                // Step 3: Combine crops with producer info
                val combinedCrops = mutableListOf<DisplayCrop>()
                for (doc in cropsSnapshot.documents) {
                    val crop = doc.toObject(Crop::class.java)?.copy(id = doc.id)
                    if (crop != null && crop.category.trim().equals(category.trim(), ignoreCase = true)) {
                        val producerId = doc.reference.parent.parent?.id
                        val (pName, fName) = producerInfoMap[producerId] ?: ("Unknown Producer" to "Unknown Farm")
                        combinedCrops.add(DisplayCrop(crop, pName, fName))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Shop $category",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel)
                }
            )

        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                errorMessage != null -> {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }

                displayCrops.isEmpty() -> {
                    Text("No products available in $category category.", modifier = Modifier.padding(16.dp))
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(displayCrops) { displayCrop ->
                            ConsumerCropCard(displayCrop = displayCrop, navController)
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun ProductCard(
    product: Crop,
    onAddToCart: () -> Unit
) {
    val icon = getCategoryIcon(product.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = product.category,
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (product.imageUri.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(product.imageUri),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(text = product.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "₹${product.pricePerKg} per kg", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Qty: ${product.quantity}kg", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddToCart,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Add to Cart",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add to Cart", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
