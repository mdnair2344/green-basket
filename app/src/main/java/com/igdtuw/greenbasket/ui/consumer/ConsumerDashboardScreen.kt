//ConsumerDashboardScreen
package com.igdtuw.greenbasket.ui.consumer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerDashboardScreen(
    navController: NavController,
    openDrawer: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    val suggestions = listOf("Mango", "Apple", "Banana", "Papaya", "Organic Mango")

    val backgroundColor = Color.White

    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.White,
            darkIcons = true
        )
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = openDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open drawer")
                    }
                },
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            showSuggestions = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        placeholder = {
                            Text(
                                text = "Search in GreenBasket",
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )
                }
            )
        },
        containerColor = Color.White // Scaffold background forced to white
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(backgroundColor) // Ensure column is white
                .padding(horizontal = 16.dp)
        ) {
            if (showSuggestions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE0F2F1))
                        .padding(8.dp)
                ) {
                    suggestions.filter {
                        it.contains(searchQuery, ignoreCase = true) || searchQuery.isEmpty()
                    }.forEach { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    searchQuery = suggestion
                                    showSuggestions = false
                                    navController.navigate("shop_products")
                                }
                                .padding(8.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Explore products, check your orders, and more.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConsumerHomeScreen(navController = navController)
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ConsumerDashboardPreview() {
    GreenBasketTheme(isProducer = false) {
        ConsumerDashboardScreen(
            navController = rememberNavController(),
            openDrawer = {}
        )
    }
}

