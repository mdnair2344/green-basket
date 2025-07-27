//AddressConfirmationScreen
package com.igdtuw.greenbasket.ui.consumer

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.ui.theme.TextColorDark
import android.util.Log // Import Log for debugging
import androidx.hilt.navigation.compose.hiltViewModel

@SuppressLint("StateFlowValueCalledInComposition")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressConfirmationScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // val coroutineScope = rememberCoroutineScope() // Not directly used here, can remove if not needed elsewhere

    val deliveryOption by sharedViewModel.deliveryOption.collectAsState()


    // Collect from the correct StateFlows
    val selectedConsumerAddress by sharedViewModel.selectedConsumerAddress.collectAsState()
    val selectedProducerAddress by sharedViewModel.selectedProducerAddress.collectAsState()

    var showEditFields by remember { mutableStateOf(false) }

    var fullNameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }

    val latestOrder by sharedViewModel.latestOrder.collectAsState()

    val orderId = sharedViewModel.latestOrder.value?.orderId




    LaunchedEffect(Unit) {
        sharedViewModel.loadUserAddress()
        sharedViewModel.loadProducerAddressFromCart()
    }

    LaunchedEffect(deliveryOption, selectedConsumerAddress, selectedProducerAddress) {
        // Reset showEditFields when deliveryOption changes or addresses update
        showEditFields = false
        val currentSelectedAddress = if (deliveryOption == "home") selectedConsumerAddress else selectedProducerAddress
        currentSelectedAddress?.let {
            fullNameInput = it.fullName
            phoneInput = it.phone
            addressInput = it.address
            Log.d("AddressConfirmDebug", "LaunchedEffect: Set inputs from loaded address: $it")
        } ?: run {
            // Clear fields if no address is available for the selected option
            fullNameInput = ""
            phoneInput = ""
            addressInput = ""
            Log.d("AddressConfirmDebug", "LaunchedEffect: Cleared inputs, no address available for $deliveryOption.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm Delivery/PickUp", color = Color.White, fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            // Only show FAB for home delivery and when not editing
            if (!showEditFields && deliveryOption == "home") {
                FloatingActionButton(
                    onClick = {
                        // Clear fields to prompt new address input
                        fullNameInput = ""
                        phoneInput = ""
                        addressInput = ""
                        showEditFields = true
                        Log.d("AddressConfirmDebug", "FAB clicked: Entering edit mode for new address.")
                    },
                    containerColor = ConsumerPrimaryVariant
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Deliver to Another Address", tint = Color.White)
                }
            }
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                DeliveryOptionButton(
                    label = "Home Delivery",
                    selected = deliveryOption == "home",
                    onClick = {
                        sharedViewModel.setDeliveryOption("home")
                    },
                    appBarDarkGreen = ConsumerPrimaryVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                DeliveryOptionButton(
                    label = "Pickup from Farm",
                    selected = deliveryOption == "pickup",
                    onClick = {
                        sharedViewModel.setDeliveryOption("pickup")
                    },
                    appBarDarkGreen = ConsumerPrimaryVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Get the currently relevant address based on deliveryOption
            val currentDisplayedAddress = if (deliveryOption == "home") selectedConsumerAddress else selectedProducerAddress

            if (showEditFields) {
                Text("Enter Address Details", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fullNameInput,
                    onValueChange = { fullNameInput = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    minLines = 3
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (fullNameInput.isNotBlank() && phoneInput.isNotBlank() && addressInput.isNotBlank()) {
                            val chosenAddress = UserAddress(
                                fullName = fullNameInput,
                                phone = phoneInput,
                                address = addressInput
                            )
                            // Save the address to Firestore (backend)
                            sharedViewModel.updateUserAddress(chosenAddress)

                            // Pass to next screen using SharedViewModel
                            sharedViewModel.setAddressToConfirm(chosenAddress)
                            // Pass delivery_option as a simple string via SavedStateHandle
                            navController.currentBackStackEntry?.savedStateHandle?.set("delivery_option", "home")

                            Log.d("AddressConfirmDebug", "Using new address. Navigating to payment_screen.")
                            navController.navigate("payment_screen")
                        } else {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            Log.d("AddressConfirmDebug", "Attempted to use new address but fields were empty.")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
                ) {
                    Text("Use This Address", color = Color.White)
                }

                // Show Cancel button only if there was a previously loaded home address
                if (deliveryOption == "home" && selectedConsumerAddress != null) {
                    TextButton(
                        onClick = {
                            showEditFields = false
                            // Revert to the loaded consumer address
                            selectedConsumerAddress?.let {
                                fullNameInput = it.fullName
                                phoneInput = it.phone
                                addressInput = it.address
                                Log.d("AddressConfirmDebug", "Cancelled edit, reverting to loaded consumer address: $it")
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel", color = ConsumerPrimaryVariant)
                    }
                }
            } else {
                if (currentDisplayedAddress != null) {
                    AddressSelectionCard(address = currentDisplayedAddress, isSelected = true) {
                        // Allow editing only for home delivery option
                        if (deliveryOption == "home") {
                            showEditFields = true
                            Log.d("AddressConfirmDebug", "Address card clicked. Entering edit mode for existing home address.")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            navController.currentBackStackEntry?.savedStateHandle?.set("delivery_option", deliveryOption)
                            navController.currentBackStackEntry?.savedStateHandle?.set("user_address", currentDisplayedAddress)

                            Log.d("AddressConfirmDebug", "Confirming selection for address: $currentDisplayedAddress. Navigating to payment_screen.")
                            val orderId = sharedViewModel.latestOrder.value?.orderId
                            if (!orderId.isNullOrEmpty()) {
                                navController.navigate("payment_screen?orderId=${Uri.encode(orderId)}")
                            } else {
                                Toast.makeText(context, "Order ID not found. Cannot proceed to payment.", Toast.LENGTH_SHORT).show()
                            }

                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
                    ) {
                        Text("Confirm Selection", color = Color.White)
                    }

                } else {
                    // Show message only if no address is available for the selected option
                    LaunchedEffect(Unit) {
                        Toast.makeText(context, "No address found for selected option", Toast.LENGTH_SHORT).show()
                        Log.w("AddressConfirmDebug", "No address available for selected option: $deliveryOption. Showing toast.")
                    }
                    EmptyAddressesContent()
                }
            }
        }
    }
}

@Composable
fun DeliveryOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    appBarDarkGreen: Color,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) appBarDarkGreen else Color.Transparent)
            .border(1.dp, appBarDarkGreen, RoundedCornerShape(8.dp)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (selected) Color.White else appBarDarkGreen,
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label)
    }
}

@Composable
fun AddressSelectionCard(
    address: UserAddress,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, ConsumerPrimaryVariant) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = ConsumerPrimaryVariant)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = address.fullName, fontWeight = FontWeight.SemiBold, color = TextColorDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = address.address, color = TextColorDark.copy(alpha = 0.7f))
                Text(text = "Phone: ${address.phone}", color = TextColorDark.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun EmptyAddressesContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "No Addresses",
            tint = Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("No address found!", style = MaterialTheme.typography.headlineSmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Click '+' to add an address.", style = MaterialTheme.typography.bodyLarge, color = Color.Gray.copy(alpha = 0.7f))
    }
}