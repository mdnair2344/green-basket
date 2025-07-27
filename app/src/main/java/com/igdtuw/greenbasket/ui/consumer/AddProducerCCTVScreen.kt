//AddProducerCCTVScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme // Assuming your theme is here
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant // Your custom green color

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProducerCCTVScreen(navController: NavController) {
    var producerName by remember { mutableStateOf("") }
    var producerDescription by remember { mutableStateOf("") }
    var cctvUrl by remember { mutableStateOf("") }
    val sharedViewModel: SharedViewModel = hiltViewModel()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New CCTV Feed", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = producerName,
                onValueChange = { producerName = it },
                label = { Text("Producer Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = producerDescription,
                onValueChange = { producerDescription = it },
                label = { Text("Product Details") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // In a real app, you would save this data (e.g., to a database)
                    // For now, just print and navigate back
                    println("Adding Producer: Name=$producerName, Desc=$producerDescription, URL=$cctvUrl")
                    navController.popBackStack() // Go back to the list screen
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
            ) {
                Text("Add Producer", color = Color.White)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showSystemUi = true, showBackground = true)
@Composable
fun AddProducerCCTVScreenPreview() {
    GreenBasketTheme(isProducer = false) {
        AddProducerCCTVScreen(navController = rememberNavController())
    }
}