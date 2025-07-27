//ProducerHomeScreen
package com.igdtuw.greenbasket.ui.producer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.igdtuw.greenbasket.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProducerHomeScreen(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {
    Scaffold(
        containerColor = Color.White, // 👈 Force white background
        topBar = {
            TopAppBar(
                title = {
                    Text("Welcome Producer!", color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ProducerFeatureCard(
                icon = Icons.Default.Inventory,
                text = "Manage Inventory",
                backgroundColor = ProducerCardBackground1,
                iconColor = TextColorDark,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                onClick = { navController.navigate("InventoryScreen") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProducerFeatureCard(
                icon = Icons.Default.LocalShipping,
                text = "Track Orders",
                backgroundColor = ProducerCardBackground2,
                iconColor = TextColorDark,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                onClick = { navController.navigate("TrackOrdersScreen") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProducerFeatureCard(
                icon = Icons.Default.Eco,
                text = "Sustainable Score",
                backgroundColor = ProducerCardBackground3,
                iconColor = TextColorDark,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                onClick = { navController.navigate("SustainableScoreScreen") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProducerFeatureCard(
                icon = Icons.Default.Money,
                text = "Revenue Earned",
                backgroundColor = ProducerCardBackground4,
                iconColor = TextColorDark,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                onClick = { navController.navigate("RevenueScreen") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProducerFeatureCard(
                icon = Icons.Default.Agriculture,
                text = "Manage Crops",
                backgroundColor = ProducerCardBackground1,
                iconColor = TextColorDark,
                modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                onClick = { navController.navigate("ManageCropsScreen") }
            )
        }
    }
}

@Composable
fun ProducerFeatureCard(
    icon: ImageVector,
    text: String,
    backgroundColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    modifier = Modifier.size(28.dp),
                    tint = iconColor
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextColorDark
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
fun PreviewProducerHomeScreenWithDrawer() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    GreenBasketTheme(content = {
        ProducerHomeScreen(
            navController = navController,
            drawerState = drawerState,
            scope = scope
        )
    }, isProducer = true)
}
