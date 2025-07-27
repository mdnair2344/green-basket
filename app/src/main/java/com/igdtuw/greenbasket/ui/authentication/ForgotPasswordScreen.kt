// ForgotPasswordScreen.kt
package com.igdtuw.greenbasket.ui.authentication

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.igdtuw.greenbasket.R

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = DarkGreen) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "GreenBasket Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Forgot Password?",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White,
                cursorColor = Color.White,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Enter your registered email", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (email.isBlank()) {
                        Toast.makeText(context, "Please enter an email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            message = if (task.isSuccessful) {
                                "Reset link sent to your email."
                            } else {
                                "Failed: ${task.exception?.message}"
                            }
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DarkGreen
                )
            ) {
                Text(if (isLoading) "Sending..." else "Send Reset Link")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (message.isNotEmpty()) {
                Text(
                    message,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { navController.popBackStack() }) {
                Text("Back to Login", color = Color.White)
            }
        }
    }
}
