//LoginScreen
package com.igdtuw.greenbasket.ui.authentication

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.igdtuw.greenbasket.R


// Define your custom dark green color
val DarkGreen = Color(0xFF1B5E20)


@Composable
fun LoginScreen(
    navController: NavController,

    googleAuthUiClient: GoogleAuthUiClient,
    viewModel: AuthenticationViewModel = viewModel(
        factory = AuthenticationViewModelFactory(googleAuthUiClient)
    )
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }



    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null && account.idToken != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                isLoading = true
                googleAuthUiClient.signInWithGoogleCredential(
                    credential = credential,
                    onResult = { authResult ->
                        isLoading = false
                        when (authResult) {
                            is AuthResultStatus.Success -> {
                                viewModel.getUserType(
                                    uid = authResult.user.uid,
                                    onResult = { userType ->
                                        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                                        prefs.edit().putString("role", userType).apply()

                                        val currentUser = authResult.user

                                        if (userType == "Consumer") {
                                            navController.navigate("ConsumerHomeScreen")
                                        } else if (userType == "Producer") {
                                            navController.navigate("ProducerHomeScreen")
                                        } else {
                                            Toast.makeText(context, "Unknown user type", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onError = { errorMessage ->
                                        Toast.makeText(context, "Failed to get user type: $errorMessage", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            is AuthResultStatus.NavigateToSignUp -> {
                                navController.navigate("SignUpScreen")
                            }

                            is AuthResultStatus.Failure -> {
                                val msg = authResult.message ?: "Authentication failed"
                                if (msg.contains("exists with password", ignoreCase = true)) {
                                    // Google credential needs to be linked into existing email/password account
                                    // Pre-fill the email field so user knows which email to enter password for
                                    email = account.email ?: ""
                                } else {
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }

                        }
                    }
                )
            } else {
                Toast.makeText(context, "Google account or token is null", Toast.LENGTH_SHORT).show()
            }

        } catch (e: ApiException) {
            Toast.makeText(context, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }






    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkGreen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Icon(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Welcome Text
            Text(
                text = "Welcome to GreenBasket",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = image,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.White
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Login Button
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    viewModel.signInWithEmail(email, password, context) { userType ->
                        isLoading = false
                        if (userType == "Consumer") {
                            navController.navigate("ConsumerHomeScreen")
                        } else if (userType == "Producer") {
                            navController.navigate("ProducerHomeScreen")
                        } else {
                            Toast.makeText(context, "Unknown user type. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DarkGreen
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Logging in..." else "Login with Email")
            }


            Spacer(modifier = Modifier.height(8.dp))
            Text("or", color = Color.White)

            Spacer(modifier = Modifier.height(8.dp))

            // Google Login
            Button(
                onClick = {
                    googleAuthUiClient.signOut {
                        val signInIntent = googleAuthUiClient.getSignInIntent()
                        launcher.launch(signInIntent)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DarkGreen
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login with Google")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("or", color = Color.White)

            Spacer(modifier = Modifier.height(8.dp))

            // OTP Login
            Button(
                onClick = {
                    navController.navigate("OtpVerificationScreen")
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DarkGreen
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login with OTP")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign up prompt
            Text(
                "Don't have an account? Sign Up",
                color = Color.White,
                modifier = Modifier.clickable {
                    navController.navigate("SignUpScreen")
                }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Forgot Password?",
                color = Color.White,
                modifier = Modifier.clickable {
                    navController.navigate("forgot_password_screen")
                }
            )

        }
    }
}