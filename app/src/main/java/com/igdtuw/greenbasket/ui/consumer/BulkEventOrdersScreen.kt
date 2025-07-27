//BulkEventOrdersScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.input.KeyboardOptions // ADD THIS IMPORT
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.ui.theme.TextColorDark

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkEventOrdersScreen(navController: NavController) {
    val sharedViewModel: SharedViewModel = hiltViewModel()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bulk Event Order", color = Color.White, fontWeight = FontWeight.Bold) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Request a Bulk Order for Your Event",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextColorDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            item { EventOrderForm() }
        }
    }
}

@Composable
fun EventOrderForm() {
    var eventName by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var itemsNeeded by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var specialInstructions by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }

    val textFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = eventName,
            onValueChange = { eventName = it },
            label = { Text("Event Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors
        )

        OutlinedTextField(
            value = eventDate,
            onValueChange = { eventDate = it },
            label = { Text("Event Date (DD/MM/YYYY)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors
        )

        OutlinedTextField(
            value = itemsNeeded,
            onValueChange = { itemsNeeded = it },
            label = { Text("Items and Quantity Needed (e.g., 50kg Apples, 20 Dozen Eggs)") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            colors = textFieldColors
        )

        OutlinedTextField(
            value = deliveryAddress,
            onValueChange = { deliveryAddress = it },
            label = { Text("Delivery Address") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            colors = textFieldColors
        )

        OutlinedTextField(
            value = specialInstructions,
            onValueChange = { specialInstructions = it },
            label = { Text("Special Instructions (e.g., specific ripeness, delivery time)") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp),
            colors = textFieldColors
        )

        OutlinedTextField(
            value = contactName,
            onValueChange = { contactName = it },
            label = { Text("Contact Person Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = textFieldColors
        )

        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Contact Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            //keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = textFieldColors
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                println("Bulk Order Request Submitted:")
                println("Event Name: $eventName")
                println("Event Date: $eventDate")
                println("Items Needed: $itemsNeeded")
                println("Delivery Address: $deliveryAddress")
                println("Special Instructions: $specialInstructions")
                println("Contact Name: $contactName")
                println("Contact Phone: $contactPhone")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
        ) {
            Text(
                text = "Submit Bulk Order Request",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun BulkEventOrdersScreenPreview() {
    GreenBasketTheme(isProducer = false) {
        BulkEventOrdersScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun EventOrderFormPreview() {
    GreenBasketTheme(isProducer = false) {
        EventOrderForm()
    }
}
