//UnderDevelopmentDialog
package com.igdtuw.greenbasket

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.R

@Composable
fun UnderDevelopmentDialog(navController: NavController) {
    AlertDialog(
        onDismissRequest = {
            navController.navigate("HomeScreen") {
                popUpTo("HomeScreen") { inclusive = true }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    navController.navigate("HomeScreen") {
                        popUpTo("HomeScreen") { inclusive = true }
                    }
                }
            ) {
                Text("OK", color = ConsumerPrimaryVariant)
            }
        },
        title = null,
        containerColor = Color.White,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color.White)
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // Watermark Icon
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .alpha(0.1f),
                    tint = Color.DarkGray
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    // Circular Logo
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "This feature is under development.",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Please check back soon!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    )
}


