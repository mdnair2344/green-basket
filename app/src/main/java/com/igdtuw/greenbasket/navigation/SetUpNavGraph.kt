//SetUpNavGraph
package com.igdtuw.greenbasket.navigation

import androidx.compose.foundation.background
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.igdtuw.greenbasket.ui.authentication.*
import com.igdtuw.greenbasket.ui.consumer.ConsumerApp
import com.igdtuw.greenbasket.ui.producer.ProducerApp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon


// Firebase
import com.google.firebase.auth.FirebaseAuth

// Context
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween

// Navigation
import androidx.navigation.NavController
import androidx.navigation.compose.*

// Compose
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// Coroutine delay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SetupNavGraph(
    navController: NavHostController,
    googleAuthUiClient: GoogleAuthUiClient,
    startDestination: String
) {
    val authViewModel: AuthenticationViewModel = viewModel(factory = AuthenticationViewModelFactory(googleAuthUiClient))
    NavHost(navController = navController, startDestination = startDestination) {

        composable("splash") {
            SplashScreen(navController = navController)
        }


        composable("SignUpScreen") {
            SignUpScreen(
                navController = navController,
                googleAuthUiClient = googleAuthUiClient,
                viewModel = authViewModel
            )
        }

        composable("LoginScreen") {
            LoginScreen(
                navController = navController,
                viewModel = authViewModel,
                googleAuthUiClient = googleAuthUiClient
            )
        }

        composable("OtpVerificationScreen") {
            OtpVerificationScreen(
                navController = navController,
                googleAuthUiClient = googleAuthUiClient,
                viewModel = authViewModel
            )
        }

        composable("ConsumerHomeScreen") {
            ConsumerApp()
        }


        composable("ProducerHomeScreen") {
            ProducerApp()
        }

        composable("forgot_password_screen") {
            ForgotPasswordScreen(navController)
        }

    }
}


/*@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()


    LaunchedEffect(Unit) {
        delay(500) // Optional splash delay

        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val role = prefs.getString("role", null)
        val user = auth.currentUser

        if (user != null && role != null) {
            when (role.lowercase()) {
                "consumer" -> navController.navigate("ConsumerHomeScreen") {
                    popUpTo("splash") { inclusive = true }
                }
                "producer" -> navController.navigate("ProducerHomeScreen") {
                    popUpTo("splash") { inclusive = true }
                }
                else -> navController.navigate("LoginScreen") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        } else {
            navController.navigate("LoginScreen") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Optional: Add your animated cart or loading UI
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}*/


@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    val cartOffsetX = remember { Animatable(-200f) } // Start from off-screen left
    val rotation = remember { Animatable(0f) }

    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val role = prefs.getString("role", null)
    val user = auth.currentUser

    LaunchedEffect(Unit) {
        // Animate cart moving from left to right with rotating wheels
        launch {
            cartOffsetX.animateTo(
                targetValue = 800f, // Change based on screen width
                animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
            )
        }
        launch {
            rotation.animateTo(
                targetValue = 1080f, // 3 full rotations (360*3)
                animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
            )
        }

        delay(1600) // Wait for animation to finish

        if (user != null && role != null) {
            when (role.lowercase()) {
                "consumer" -> navController.navigate("ConsumerHomeScreen") {
                    popUpTo("splash") { inclusive = true }
                }
                "producer" -> navController.navigate("ProducerHomeScreen") {
                    popUpTo("splash") { inclusive = true }
                }
                else -> navController.navigate("LoginScreen") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        } else {
            navController.navigate("LoginScreen") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF388E3C)), // Consumer primary variant green
        contentAlignment = Alignment.CenterStart
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = "Cart",
            tint = Color.White,
            modifier = Modifier
                .offset { IntOffset(cartOffsetX.value.toInt(), 0) }
                .graphicsLayer(rotationZ = rotation.value)
                .size(72.dp)
        )
    }
}
