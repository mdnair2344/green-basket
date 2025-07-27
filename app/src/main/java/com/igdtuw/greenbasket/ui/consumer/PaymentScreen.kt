//PaymentScreen
package com.igdtuw.greenbasket.ui.consumer

import android.R.attr.type
import android.annotation.SuppressLint
import android.os.Build
import android.util.Log // Make sure Log is imported
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // Keep if using Hilt
import androidx.lifecycle.viewmodel.compose.viewModel // Keep if not using Hilt consistently or for CartViewModel
import androidx.navigation.NavController
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.ui.theme.TextColorDark
import java.text.NumberFormat
import java.util.*
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.PaymentBankDetails
import com.igdtuw.greenbasket.ProfileViewModel
import com.igdtuw.greenbasket.UpiDetails
import com.igdtuw.greenbasket.CardDetails
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.itextpdf.kernel.pdf.PdfName.Column


data class BankDetails(val accountNumber: String, val bankName: String, val ifscCode: String, val accountHolderName: String)

fun calculateTotalPayableWithCharges(cartAmount: Double): Triple<Double, Double, Double> {
    val platformFeePercent = 2.5
    val gstPercent = 18.0

    val platformFee = cartAmount * (platformFeePercent / 100)
    val gstOnPlatformFee = platformFee * (gstPercent / 100)
    val totalPayable = cartAmount + platformFee + gstOnPlatformFee

    return Triple(platformFee, gstOnPlatformFee, totalPayable)
}


@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PaymentScreen(
    navController: NavController,
    // Use hiltViewModel() if you have Hilt set up, otherwise viewModel().
    // If you're mixing, ensure your dependencies are clear.
    orderId: String,
    sharedViewModel: SharedViewModel = hiltViewModel(), // Using hiltViewModel as an example based on your code
    cartViewModel: CartViewModel = hiltViewModel() // Assuming CartViewModel might be separate or also Hilt
) {
    val context = LocalContext.current
    val orderCreationStatus by sharedViewModel.orderCreationStatus.collectAsState()

    val cartItems by cartViewModel.cartItems.collectAsState()
    val subtotal by cartViewModel.subtotal.collectAsState()
    var selectedPaymentMethod by remember { mutableStateOf("Cash on Delivery") }
    var upiDetails: UpiDetails? by remember { mutableStateOf(null) }
    var cardDetails: CardDetails? by remember { mutableStateOf(null) }
    var bankDetails: BankDetails? by remember { mutableStateOf(null) }

    val latestOrder by sharedViewModel.latestOrder.collectAsState()


    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // --- Retrieve userAddress from SharedViewModel ---
    val userAddress by sharedViewModel.userAddress.collectAsState()

    val chargeTriple by remember(selectedPaymentMethod, subtotal) {
        derivedStateOf {
            if (selectedPaymentMethod == "Pay Online") {
                calculateTotalPayableWithCharges(subtotal)
            } else {
                Triple(0.0, 0.0, subtotal)
            }
        }
    }

    val appliedDiscounts by sharedViewModel.appliedDiscounts.collectAsState()

    fun DiscountOffer.calculateDiscount(baseAmount: Double): Double {
        return (baseAmount * discountValue / 100.0)
    }

    fun calculateTotalDiscount(
        totalBeforeDiscount: Double,
        appliedDiscounts: List<DiscountOffer>
    ): Double {
        return appliedDiscounts.sumOf { offer ->
            offer.calculateDiscount(totalBeforeDiscount)
        }
    }

    val platformFee = chargeTriple.first
    val gstOnPlatformFee = chargeTriple.second
    val totalBeforeDiscount = chargeTriple.third

    val totalDiscount = calculateTotalDiscount(totalBeforeDiscount, appliedDiscounts)
    val totalPayableWithCharges = (totalBeforeDiscount - totalDiscount).coerceAtLeast(0.0)

    val deliveryOption by sharedViewModel.deliveryOption.collectAsState()


    // 🆕 Recover userAddress from SavedStateHandle
    val navBackStackEntry = navController.previousBackStackEntry
    val restoredAddress = remember {
        navBackStackEntry?.savedStateHandle?.get<UserAddress>("user_address")
    }

// 🆕 Write it into SharedViewModel if not already set
    LaunchedEffect(restoredAddress) {
        if (restoredAddress != null) {
            Log.d("PaymentScreenDebug", "Restoring user address from SavedStateHandle: $restoredAddress")
            sharedViewModel.setUserAddress(restoredAddress)
        }
    }



    // Logging for debugging retrieval
    LaunchedEffect(userAddress, deliveryOption) {
        Log.d("PaymentScreenDebug", "--- PaymentScreen Composable Initialized/Recomposed ---")
        Log.d("PaymentScreenDebug", "userAddress from SharedViewModel: $userAddress")
        Log.d("PaymentScreenDebug", "deliveryOption from SavedStateHandle: $deliveryOption")

        if (userAddress == null) {
            Log.e(
                "PaymentScreenDebug",
                "CRITICAL: userAddress is NULL. Data not passed correctly from AddressConfirmationScreen."
            )
            Toast.makeText(
                context,
                "Error: Delivery address missing. Please go back.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // ✅ Handle order status properly
    LaunchedEffect(orderCreationStatus) {
        when (orderCreationStatus) {
            SharedViewModel.OrderStatus.Success -> {
                Toast.makeText(context, "Order placed successfully!", Toast.LENGTH_LONG).show()
                cartViewModel.clearCart()
                sharedViewModel.resetOrderCreationStatus()
                sharedViewModel.clearAddressToConfirm() // Clear address from ViewModel after successful order
                // Pop back to the main consumer screen (or where appropriate)
                navController.popBackStack(
                    "consumer_main_screen_route",
                    inclusive = false
                ) // Adjust this route as per your nav graph
                navController.navigate("order_status_screen?isSuccess=true&deliveryOption=$deliveryOption")
            }

            is SharedViewModel.OrderStatus.Error -> {
                val errorMessage =
                    (orderCreationStatus as SharedViewModel.OrderStatus.Error).message
                Toast.makeText(context, "Failed to place order: $errorMessage", Toast.LENGTH_LONG)
                    .show()
                sharedViewModel.resetOrderCreationStatus()
            }

            SharedViewModel.OrderStatus.Loading -> {
                Toast.makeText(context, "Placing order...", Toast.LENGTH_SHORT).show()
            }

            SharedViewModel.OrderStatus.Idle -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Confirm Order & Payment",
                        color = Color.White, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Delivery Address Display ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Delivery/Pickup Address:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextColorDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (userAddress != null) {
                            Log.d(
                                "PaymentScreenDebug",
                                "Displaying userAddress in UI: ${userAddress!!.fullName}, ${userAddress!!.phone}, ${userAddress!!.address}"
                            )
                            Text(
                                "Option: ${if (deliveryOption == "home") "Home Delivery" else "Pickup from Farm"}",
                                color = TextColorDark
                            )
                            Text("Name: ${userAddress!!.fullName}", color = TextColorDark)
                            Text("Phone: ${userAddress!!.phone}", color = TextColorDark)
                            Text("Address: ${userAddress!!.address}", color = TextColorDark)
                        } else {
                            Log.e(
                                "PaymentScreenDebug",
                                "userAddress is NULL in UI block. Displaying 'No delivery address selected.'"
                            )
                            Text(
                                "No delivery/pickup address selected.",
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    navController.popBackStack() // Go back to AddressConfirmationScreen
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
                            ) {
                                Text("Select Address", color = Color.White)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }


            // --- Order Summary ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Order Summary:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${item.productName} (x${item.quantity})")
                                Text(currencyFormat.format(item.quantity * item.unitPrice))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:")
                            Text(currencyFormat.format(subtotal))
                        }

                        if (selectedPaymentMethod == "Pay Online") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Platform Fee (2.5%):")
                                Text(currencyFormat.format(platformFee))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("GST on Platform Fee (18%):")
                                Text(currencyFormat.format(gstOnPlatformFee))
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (totalDiscount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discounts Applied:")
                                Text("-${currencyFormat.format(totalDiscount)}")
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                        }


                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Payable:", style = MaterialTheme.typography.titleMedium)
                            Text(
                                currencyFormat.format(totalPayableWithCharges),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) } // Spacer is now an item in the outer LazyColumn


            item{if (!latestOrder?.producerId.isNullOrBlank()) {
                PaymentDiscounts(
                    producerId = latestOrder!!.producerId,
                    orderCropNames = latestOrder!!.items?.map { it.productName } ?: emptyList(),
                    onOffersApplied = { selectedOffers ->
                        sharedViewModel.setAppliedDiscounts(selectedOffers)
                    }
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
                Text("Loading discounts...", modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally))
            }}



            item { Spacer(modifier = Modifier.height(16.dp)) } // Spacer is now an item in the outer LazyColumn


            // --- Payment Method ---
            item {
                PaymentSection(
                    selectedPaymentMethod = selectedPaymentMethod,
                    onPaymentMethodSelected = { selectedPaymentMethod = it },
                    onUpiEntered = { upiDetails = it },
                    onCardEntered = { cardDetails = it },
                    onBankEntered = { bankDetails = it }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) } // Spacer is now an item in the outer LazyColumn

            // --- Place Order ---
            item {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                val paymentProcessStatus by profileViewModel.paymentProcessStatus.collectAsState()
                Button(
                    onClick = {
                        if (userAddress == null) {
                            Toast.makeText(context, "Delivery/Pickup address not found.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (cartItems.isEmpty()) {
                            Toast.makeText(context, "Your cart is empty!", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (totalPayableWithCharges <= 0.0 && selectedPaymentMethod != "Cash on Delivery") {
                            Toast.makeText(context, "Final amount must be positive.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val currentUserId = Firebase.auth.currentUser?.uid
                        if (currentUserId == null) {
                            Toast.makeText(context, "User not logged in.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val firestore = FirebaseFirestore.getInstance()

                        if (selectedPaymentMethod == "Cash on Delivery") {
                            firestore.collection("orders").document(orderId)
                                .update("paymentStatus", "COD")
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Order placed with Cash on Delivery.", Toast.LENGTH_SHORT).show()

                                    navController.navigate(
                                        "order_status_screen?orderId=$orderId&isSuccess=true&deliveryOption=$deliveryOption"
                                    )
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Failed to update COD status.", Toast.LENGTH_SHORT).show()
                                }
                        }
                        else if (selectedPaymentMethod == "Pay Online") {
                            firestore.collection("orders").document(orderId).get()
                                .addOnSuccessListener { orderSnapshot ->
                                    val producerId = orderSnapshot.getString("producerId")
                                    if (producerId.isNullOrEmpty()) {
                                        Toast.makeText(context, "Producer ID not found.", Toast.LENGTH_SHORT).show()
                                        return@addOnSuccessListener
                                    }

                                    firestore.collection("users").document(producerId).get()
                                        .addOnSuccessListener { userSnapshot ->
                                            val merchantId = userSnapshot.getString("linkedAccountId")
                                            if (merchantId.isNullOrEmpty()) {
                                                Toast.makeText(context, "Merchant ID not found.", Toast.LENGTH_SHORT).show()
                                                return@addOnSuccessListener
                                            }

                                            val paymentBankDetails = bankDetails?.let {
                                                PaymentBankDetails(
                                                    accountNumber = it.accountNumber,
                                                    ifscCode = it.ifscCode,
                                                    bankName = it.bankName,
                                                    accountHolderName = it.accountHolderName
                                                )
                                            }

                                            profileViewModel.initiateOrderAndPayment(
                                                producerMerchantAccountId = merchantId,
                                                orderTotal = totalPayableWithCharges,
                                                paymentMethod = selectedPaymentMethod,
                                                consumerUpiDetails = upiDetails,
                                                consumerCardDetails = cardDetails,
                                                consumerBankDetails = paymentBankDetails,
                                                consumerId = currentUserId,
                                                localOrderId = orderId,
                                                context = context
                                            )

                                            // 🔔 Observe the payment process status

                                            when (paymentProcessStatus) {
                                                is ProfileViewModel.ApiStatus.Success -> {
                                                    firestore.collection("orders").document(orderId)
                                                        .update(
                                                            mapOf(
                                                                "paymentStatus" to "online_success",
                                                                "status" to "payment_successful"
                                                            )
                                                        )
                                                        .addOnSuccessListener {
                                                            navController.navigate(
                                                                "order_status_screen?orderId=$orderId&isSuccess=true&deliveryOption=$deliveryOption"
                                                            )
                                                        }
                                                }

                                                is ProfileViewModel.ApiStatus.Error -> {
                                                    firestore.collection("orders").document(orderId)
                                                        .update("paymentStatus", "online_failure")
                                                        .addOnSuccessListener {
                                                            Toast.makeText(context, "Payment Failed. Try again.", Toast.LENGTH_SHORT).show()
                                                        }
                                                }

                                                is ProfileViewModel.ApiStatus.Loading, ProfileViewModel.ApiStatus.Idle -> {
                                                    // Optionally show progress or idle state
                                                }
                                            }


                                        }


                                        .addOnFailureListener {
                                            Toast.makeText(context, "Error fetching merchant.", Toast.LENGTH_SHORT).show()
                                        }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error fetching order.", Toast.LENGTH_SHORT).show()
                                }

                            navController.navigate(
                                "order_status_screen?orderId=$orderId&isSuccess=true&deliveryOption=$deliveryOption"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant),
                    enabled = orderCreationStatus != SharedViewModel.OrderStatus.Loading
                ) {
                    if (orderCreationStatus == SharedViewModel.OrderStatus.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Proceed", color = Color.White)
                    }
                }

            }

        }
    }
}


@Composable
fun <T : Parcelable> PaymentDetailsSection(
    title: String,
    savedDetails: List<T>,
    selectedDetailId: String?,
    onDetailSelected: (String?) -> Unit,
    onUseAnother: () -> Unit,
    newDetailsInput: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextColorDark)
        Spacer(modifier = Modifier.height(8.dp))

        if (savedDetails.isNotEmpty()) {
            savedDetails.forEach { detail ->
                val id = when (detail) {
                    is UserUpiDetails -> detail.id // Assuming UserUpiDetails has an 'id' for selection
                    is UserCardDetails -> detail.id
                    is UserBankDetails -> detail.id // Assuming UserBankDetails has an 'id' for selection
                    else -> ""
                }
                val displayText = when (detail) {
                    is UserUpiDetails -> detail.upiId
                    is UserCardDetails -> "${detail.cardType} **** ${detail.cardNumber.takeLast(4)}" // Display last 4
                    is UserBankDetails -> "${detail.bankName} - A/c: ****${detail.accountNumber.takeLast(4)}"
                    else -> "Unknown Detail"
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDetailSelected(id) }) {
                    RadioButton(
                        selected = selectedDetailId == id,
                        onClick = { onDetailSelected(id) },
                        colors = RadioButtonDefaults.colors(selectedColor = ConsumerPrimaryVariant)
                    )
                    Text(displayText, color = TextColorDark)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // "Use another" option
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .fillMaxWidth()
            .clickable { onUseAnother() }) {
            RadioButton(
                selected = selectedDetailId == null, // If no saved detail is selected, "Use another" is active
                onClick = { onUseAnother() },
                colors = RadioButtonDefaults.colors(selectedColor = ConsumerPrimaryVariant)
            )
            Text("Use another", color = TextColorDark)
        }

        // Show input fields only if "Use another" is selected
        if (selectedDetailId == null) {
            Spacer(modifier = Modifier.height(16.dp))
            newDetailsInput() // Composable lambda for input fields
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSection(
    selectedPaymentMethod: String,
    onPaymentMethodSelected: (String) -> Unit,
    onUpiEntered: (UpiDetails) -> Unit,
    onCardEntered: (CardDetails) -> Unit,
    onBankEntered: (BankDetails) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Select Payment Method:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedPaymentMethod == "Cash on Delivery",
                onClick = { onPaymentMethodSelected("Cash on Delivery") },
                colors = RadioButtonDefaults.colors(selectedColor = ConsumerPrimaryVariant)
            )
            Text("Cash on Delivery")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedPaymentMethod == "Pay Online",
                onClick = { onPaymentMethodSelected("Pay Online") },
                colors = RadioButtonDefaults.colors(selectedColor = ConsumerPrimaryVariant)
            )
            Text("Pay Online")
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedPaymentMethod == "Pay Online") {
            Text(
                text = "Pay Online Selected. Payment will proceed via Razorpay.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "Cash on Delivery selected. You can pay directly on delivery.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (selectedPaymentMethod == "Pay Online") {
            Text(
                text = "Note: Additional platform and GST charges apply.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}




fun getLinkedAccountIdFromOrder(
    orderId: String,
    onResult: (String?) -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    // Step 1: Get the order document from `orders/{orderId}`
    db.collection("orders")
        .document(orderId)
        .get()
        .addOnSuccessListener { orderDoc ->
            if (orderDoc.exists()) {
                val producerId = orderDoc.getString("producerId")

                if (!producerId.isNullOrBlank()) {
                    // Step 2: Fetch linkedAccountId from `users/{producerId}`
                    db.collection("users")
                        .document(producerId)
                        .get()
                        .addOnSuccessListener { producerDoc ->
                            if (producerDoc.exists()) {
                                val linkedAccountId = producerDoc.getString("linkedAccountId")
                                onResult(linkedAccountId)
                            } else {
                                Log.e("FetchLinkedID", "Producer not found: $producerId")
                                onResult(null)
                            }
                        }
                        .addOnFailureListener {
                            Log.e("FetchLinkedID", "Failed to fetch producer data", it)
                            onResult(null)
                        }
                } else {
                    Log.e("FetchLinkedID", "Order missing producerId")
                    onResult(null)
                }
            } else {
                Log.e("FetchLinkedID", "Order not found: $orderId")
                onResult(null)
            }
        }
        .addOnFailureListener {
            Log.e("FetchLinkedID", "Failed to fetch order", it)
            onResult(null)
        }
}