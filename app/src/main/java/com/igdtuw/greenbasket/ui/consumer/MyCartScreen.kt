//MyCartScreen
package com.igdtuw.greenbasket.ui.consumer


import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.igdtuw.greenbasket.R
import com.igdtuw.greenbasket.ui.theme.*
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCartScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    val currentUserUid = Firebase.auth.currentUser?.uid
    val currentUserEmail = Firebase.auth.currentUser?.email ?: ""

    val cartItems by sharedViewModel.cartItems.collectAsState()
    val cartTotal = (cartItems.sumOf { it.product.price * it.quantity })

    LaunchedEffect(currentUserUid) {
        if (currentUserUid != null) {
            sharedViewModel.fetchCartRealtime(currentUserUid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cart", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (cartItems.isEmpty()) {
                EmptyCartContent(navController)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        CartItemCard(
                            item = item,
                            onRemoveClick = { sharedViewModel.removeFromCart(item) },
                            onIncreaseQuantity = { sharedViewModel.increaseQuantity(item) },
                            onDecreaseQuantity = { sharedViewModel.decreaseQuantity(item) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground2),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total:", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = TextColorDark)
                            Text("₹${"%.2f".format(cartTotal)}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold), color = ConsumerPrimaryVariant)
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val distinctProducerIds = cartItems.map { it.product.producerId }.toSet()

                                if (distinctProducerIds.size > 1) {
                                    Toast.makeText(
                                        context,
                                        "Please order from one producer at a time.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    return@Button
                                }

                                val producerId = distinctProducerIds.first()
                                val orderId = firestore.collection("orders").document().id

                                val orderItems = cartItems.map {
                                    mapOf(
                                        "productId" to it.product.id,
                                        "productName" to it.product.name,
                                        "quantity" to it.quantity,
                                        "unitPrice" to it.product.price
                                    )
                                }

                                // STEP 1: Validate quantity for each crop before proceeding
                                val cropChecks = cartItems.map { item ->
                                    firestore.collection("producers")
                                        .document(producerId)
                                        .collection("crops")
                                        .document(item.product.id)
                                        .get()
                                }

                                // Run all quantity fetches concurrently
                                Tasks.whenAllSuccess<DocumentSnapshot>(cropChecks)
                                    .addOnSuccessListener { results ->
                                        for ((index, snapshot) in results.withIndex()) {
                                            val item = cartItems[index]
                                            val availableQty = snapshot.getLong("quantity")?.toInt() ?: 0


                                            if (item.quantity > availableQty) {
                                                Toast.makeText(
                                                    context,
                                                    "Only $availableQty units of ${item.product.name} available.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@addOnSuccessListener
                                            }
                                        }

                                        // STEP 2: Check producer time restriction
                                        firestore.collection("accept5to10").document(producerId).get()
                                            .addOnSuccessListener { doc ->
                                                val isRestricted = doc.exists()

                                                if (isRestricted) {
                                                    val now = LocalTime.now()
                                                    val start = LocalTime.of(5, 0)
                                                    val end = LocalTime.of(22, 0)

                                                    if (now.isBefore(start) || now.isAfter(end)) {
                                                        Toast.makeText(
                                                            context,
                                                            "This producer accepts orders only between 5 a.m. to 10 p.m.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                        return@addOnSuccessListener
                                                    }
                                                }

                                                // STEP 3: Place order
                                                val orderDateMillis = System.currentTimeMillis()
                                                val orderData = mapOf(
                                                    "orderId" to orderId,
                                                    "userId" to currentUserUid,
                                                    "userEmail" to currentUserEmail,
                                                    "totalAmount" to cartTotal,
                                                    "status" to "waiting_for_approval",
                                                    "orderDate" to orderDateMillis,
                                                    "items" to orderItems,
                                                    "producerId" to producerId
                                                )

                                                firestore.collection("orders")
                                                    .document(orderId)
                                                    .set(orderData)
                                                    .addOnSuccessListener {
                                                        val createdOrder = Order(
                                                            orderId = orderId,
                                                            userId = currentUserUid ?: "",
                                                            userEmail = currentUserEmail,
                                                            totalAmount = cartTotal,
                                                            status = "waiting_for_approval",
                                                            orderDate = orderDateMillis,
                                                            items = cartItems.map {
                                                                OrderItem(
                                                                    productId = it.product.id,
                                                                    productName = it.product.name,
                                                                    quantity = it.quantity,
                                                                    unitPrice = it.product.price
                                                                )
                                                            },
                                                            producerId = producerId
                                                        )

                                                        sharedViewModel.setLatestOrder(createdOrder)
                                                        navController.navigate("waitingForApproval/${orderId}")
                                                    }
                                                    .addOnFailureListener {
                                                        Toast.makeText(context, "Order submission failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Could not verify producer availability.", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Error checking product stock.", Toast.LENGTH_SHORT).show()
                                    }

                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
                        ) {
                            Text(
                                "Proceed to Checkout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onRemoveClick: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextColorDark)
                Text("₹${item.product.price}", style = MaterialTheme.typography.bodySmall, color = TextColorDark.copy(alpha = 0.7f))
            }
            Spacer(Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecreaseQuantity) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                }
                Text("${item.quantity}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = TextColorDark, modifier = Modifier.padding(horizontal = 4.dp))
                IconButton(onClick = onIncreaseQuantity) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("₹${"%.2f".format(item.product.price * item.quantity)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = ConsumerPrimaryVariant)
        }
    }
}

@Composable
fun EmptyCartContent(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_cart),
            contentDescription = "Empty Cart",
            modifier = Modifier.size(300.dp),
            alpha = 0.6f
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Your GreenBasket cart is empty!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Start adding some fresh organic produce to your basket.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { navController.navigate("shop_products") },
            colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Start Shopping", color = Color.White)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun MyCartScreenPreview() {
    GreenBasketTheme(isProducer = false) {
        MyCartScreen(navController = rememberNavController())
    }
}
