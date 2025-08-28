package com.igdtuw.greenbasket.ui.consumer

import android.widget.Toast
import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.tasks.await
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import androidx.compose.material3.Button
import android.content.Context
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            IconButton(
                onClick = {
                    if (!isReadOnly) onRatingChanged(i)
                },
                enabled = !isReadOnly
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Star $i",
                    tint = if (i <= rating) Color(0xFFFFC107) else Color.Gray
                )
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review, viewModel: ProductDetailViewModel) {
    var consumerName by remember { mutableStateOf("Loading...") }

    LaunchedEffect(review.consumerId) {
        viewModel.getConsumerName(review.consumerId) { name ->
            consumerName = name ?: review.consumerId // fallback to UID
        }
    }

    Card(
        modifier = Modifier
            .width(300.dp) // So only one card shows at a time
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = ConsumerCardBackground1
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "By: $consumerName",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            review.timestamp?.toDate()?.let { date ->
                Text(
                    text = "Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                repeat(5) { index ->
                    val rating = (review.rating as? Number)?.toInt() ?: 0

                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = if (index < review.rating) Color(0xFFFFC107) else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}



@Composable
fun ReviewCarousel(reviews: List<Review>, viewModel: ProductDetailViewModel) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(reviews) { review ->
            ReviewCard(review = review, viewModel = viewModel)
        }
    }
}
fun makePhoneCall(context: Context, phoneNumber: String) {
    val cleanNumber = phoneNumber.filter { it.isDigit() }
    val callIntent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$cleanNumber")
    }
    context.startActivity(callIntent)
}
fun startVideoCall(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://wa.me/$phone") // Can be replaced by another video call intent
    }
    context.startActivity(intent)
}
fun openChatOrSms(context: Context, phone: String, producerName: String) {
    val cleanPhone = phone.filter { it.isDigit() }
    val prefill = "Hi $producerName, I'm interested in discussing about your product."

    val waUri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(prefill)}")
    val waIntent = Intent(Intent.ACTION_VIEW, waUri).apply {
        setPackage("com.whatsapp")
    }

    try {
        if (waIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(waIntent)
        } else {
            // WhatsApp not installed or number not registered
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

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    producerId: String,
    cropId: String,
    consumerId: String,
    viewModel: ProductDetailViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(),
    navController: NavController
) {
    val crop by viewModel.crop.collectAsState()
    val producer by viewModel.producer.collectAsState()
    val certificates by viewModel.certificates.collectAsState()
    val reviews by viewModel.reviews.collectAsState()

    var reviewText by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val similarCrops by viewModel.similarCrops.collectAsState()

    val scoreViewModel: ConsumerProducerScoreViewModel = hiltViewModel()
    val score by scoreViewModel.score

    LaunchedEffect(producerId) {
        scoreViewModel.fetchSustainabilityScore(producerId)
    }


    LaunchedEffect(cropId) {
        viewModel.loadCrop(producerId, cropId)
        viewModel.loadProducer(producerId)
        viewModel.loadCertificates(producerId, cropId)
        viewModel.loadReviews(cropId)
    }

    LaunchedEffect(crop) {
        Log.d("ProductDetailScreen", "LaunchedEffect(crop) triggered. Crop is: $crop")
        crop?.let {
            viewModel.loadSimilarCropsAcrossProducers(it.id, it.category)
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${producer?.name ?: "Producer"}'s ${crop?.name ?: "Crop"}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel)
                }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        crop?.let { crop ->

            val cartItems by sharedViewModel.cartItems.collectAsState()
            val wishlistItems by sharedViewModel.wishlistItems.collectAsState()

            val isInCart = cartItems.any { cropId== crop.id }
            val existingCartItem = cartItems.find { it.product.id == crop.id }
            val constructedCartItem = existingCartItem ?: run {
                val newItem = CartItem(
                    product = CartableProductInfo(
                        id = crop.id,
                        name = crop.name,
                        price = crop.pricePerKg,
                        producerId = crop.producer
                    ),
                    quantity = 1
                )
                newItem
            }

            val quantity = constructedCartItem.quantity

            val isWishlisted = wishlistItems.any { it.crop.id == crop.id } // ✅ correct



            val context = LocalContext.current
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(paddingValues)
            ) {

                // Crop Images
                item {
                    val imageUris = listOf(crop.imageUri)
                    if (imageUris.isNotEmpty()) {
                        LazyRow {
                            items(imageUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(screenWidth),
                                    //.height(250.dp),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                    }
                }

                // Crop Details
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(crop.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.height(4.dp))
                        Text("Category: ${crop.category}")
                        Text("Price: ₹${crop.pricePerKg}")
                        Text("Description: ${crop.description}")
                    }
                }

                // Action Buttons
                item {

                    val isInCart = cartItems.any { it.product.id == crop.id }
                    val existingCartItem = cartItems.find { it.product.id == crop.id }
                    val quantity = existingCartItem?.quantity ?: 1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isInCart) {
                            Button(
                                onClick = {
                                    sharedViewModel.addToCart(crop)
                                    Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ConsumerPrimaryVariant,
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Add to Cart")
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        existingCartItem?.let {
                                            sharedViewModel.decreaseQuantity(it)
                                        }
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
                                        existingCartItem?.let {
                                            sharedViewModel.increaseQuantity(it)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("+", color = Color.White)
                                }
                            }
                        }

                        IconButton(onClick = {
                            if (isWishlisted) {
                                sharedViewModel.removeFromWishlistWithUndo(crop.id)
                                Toast.makeText(context, "Removed from wishlist", Toast.LENGTH_SHORT).show()
                            } else {
                                sharedViewModel.addToWishlist(crop)
                                Toast.makeText(context, "Added to wishlist", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = ConsumerPrimaryVariant
                            )
                        }
                    }

                }





                // ✅ Producer Information
                producer?.let { prod ->
                    item {
                        Text(
                            text = "Producer Information",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        )
                    }
                    /*item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text("Name: ${prod.name}")
                            Text("Farm: ${prod.farmName}")
                            Text("Phone: ${prod.phone}")
                            Text(
                                text = "Sustainability Score: ${score?.let { "%.1f".format(it) } ?: "Loading..."}",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )

                        }
                    }

                }*/

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Name: ${prod.name}")
                                Text("Farm: ${prod.farmName}")
                                Text("Phone: ${prod.phone}")
                                Text(
                                    text = "Sustainability Score: ${score?.let { "%.1f".format(it) } ?: "Loading..."}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center)
                            {
                                if (prod.imageUri.isNotBlank()) {
                                    AsyncImage(
                                        model = prod.imageUri,
                                        contentDescription = "Producer Profile Image",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .background(
                                                Color.LightGray,
                                                shape = RoundedCornerShape(30.dp)
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Default Profile Image",
                                        modifier = Modifier
                                            .size(60.dp)
                                            .background(
                                                Color.LightGray,
                                                shape = RoundedCornerShape(30.dp)
                                            ),
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Buttons for Call, Video Call, and Chat
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    IconButton(onClick = {
                                        makePhoneCall(context, prod.phone)
                                    }) {
                                        Icon(Icons.Default.Call, contentDescription = "Call")
                                    }
                                    IconButton(onClick = { startVideoCall(context, prod.phone) }) {
                                        Icon(
                                            Icons.Default.VideoCall,
                                            contentDescription = "Video Call"
                                        )
                                    }
                                    IconButton(onClick = {
                                        openChatOrSms(context, prod.phone, prod.name)
                                    }) {
                                        Icon(Icons.Default.Chat, contentDescription = "Chat")
                                    }
                                }
                            }
                        }
                    }

                    // Certificates (if any)
                    if (certificates.isNotEmpty()) {
                        item {
                            Text(
                                "Certificates",
                                Modifier.padding(start = 16.dp, top = 4.dp)
                                //style = MaterialTheme.typography.titleMedium
                            )
                        }
                        item {
                            LazyRow(contentPadding = PaddingValues(4.dp)) {
                                items(certificates) { cert ->
                                    AsyncImage(
                                        model = cert.certificateUrl,
                                        contentDescription = "Certificate",
                                        modifier = Modifier
                                            .width(screenWidth)
                                            //.height(200.dp)
                                            .padding(end = 4.dp),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                            }
                        }
                    }

                    // Review Submission
                    item {
                        Column(Modifier.padding(16.dp)) {
                            Text("Write a Review", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = reviewText,
                                onValueChange = { reviewText = it },
                                label = { Text("Your Review") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Your Rating:")
                            StarRatingBar(rating = rating, onRatingChanged = { rating = it })
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.submitReview(crop.id, producerId, consumerId, reviewText, rating)
                                    reviewText = ""
                                    rating = 0
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ConsumerPrimaryVariant,
                                    contentColor = Color.White
                                )

                            ) {
                                Text("Submit")
                            }
                        }
                    }

                    // Existing Reviews
                    item {
                        Column {
                            Text("Reviews", Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)

                            if (reviews.isNotEmpty()) {
                                ReviewCarousel(reviews = reviews, viewModel = viewModel)
                            } else {
                                Text("No reviews yet.", Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }


                    item {
                        Text(
                            text = "Similar Products",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 24.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (similarCrops.isEmpty()) {
                                item {
                                    Text(
                                        text = "No similar crops found.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(similarCrops) { similarCrop ->
                                    Card(
                                        modifier = Modifier
                                            .width(150.dp)
                                            .height(180.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = ConsumerCardBackground1
                                        ),
                                        onClick = {
                                            navController.navigate("product_details/${similarCrop.producer}/${similarCrop.id}")
                                        }
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.SpaceBetween,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            AsyncImage(
                                                model = similarCrop.imageUri,
                                                contentDescription = "Crop Image",
                                                modifier = Modifier
                                                    .height(80.dp)
                                                    .fillMaxWidth(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = similarCrop.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "₹${similarCrop.pricePerKg}/kg",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}