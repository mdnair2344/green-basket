//SignUpScreen
package com.igdtuw.greenbasket.ui.authentication

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.igdtuw.greenbasket.R
import androidx.compose.ui.res.painterResource
@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: AuthenticationViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("Consumer") }
    var farmName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var signUppasswordVisible by remember { mutableStateOf(false) }
    var confirmpasswordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkGreen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(25.dp))
            // Logo
            Icon(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "GreenBasket Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(100.dp)
            )

            //Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to GreenBasket",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            fun Modifier.fieldModifier() = this.fillMaxWidth()

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
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name", color = Color.White) },
                modifier = Modifier.fieldModifier(),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address", color = Color.White) },
                modifier = Modifier.fieldModifier(),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fieldModifier(),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Select User Type", color = Color.White)

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = userType == "Consumer",
                    onClick = { userType = "Consumer" },
                    colors = RadioButtonDefaults.colors(selectedColor = Color.White)
                )
                Text("Consumer", color = Color.White)
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = userType == "Producer",
                    onClick = { userType = "Producer" },
                    colors = RadioButtonDefaults.colors(selectedColor = Color.White)
                )
                Text("Producer", color = Color.White)
            }

            if (userType == "Producer") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = { Text("Farm Name", color = Color.White) },
                    modifier = Modifier.fieldModifier(),
                    colors = textFieldColors
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fieldModifier(),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (signUppasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (signUppasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { signUppasswordVisible = !signUppasswordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = Color.White)
                    }
                },
                modifier = Modifier.fieldModifier(),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (confirmpasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (confirmpasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { confirmpasswordVisible = !confirmpasswordVisible }) {
                        Icon(imageVector = image, contentDescription = null, tint = Color.White)
                    }
                },
                modifier = Modifier.fieldModifier(),
                colors = textFieldColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            var isLoading by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    if (isLoading) return@Button

                    // Name validation
                    if (name.isBlank() || !name.matches(Regex("^[a-zA-Z ]+\$"))) {
                        Toast.makeText(context, "Please enter a valid name (alphabets only)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (address.isBlank()) {
                        Toast.makeText(context, "Please enter address", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Phone number check
                    if (phone.length != 10 || !phone.all { it.isDigit() }) {
                        Toast.makeText(context, "Phone number must be 10 digits", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Email validation
                    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // Password strength check
                    val passwordRegex = Regex("^(?=.*[0-9])(?=.*[!@#\$%^&*])[A-Za-z0-9!@#\$%^&*]{6,}\$")
                    if (!password.matches(passwordRegex)) {
                        Toast.makeText(
                            context,
                            "Password must be at least 6 characters, include a number and a special character",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }

                    if (password != confirmPassword) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (userType == "Producer" && farmName.isBlank()) {
                        Toast.makeText(context, "Please enter farm name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    viewModel.signUpWithEmail(
                        name = name,
                        address = address,
                        phone = phone,
                        email = email,
                        password = password,
                        userType = userType,
                        farmName = farmName,
                        context = context
                    ) { success ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Signup successful!", Toast.LENGTH_SHORT).show()
                            navController.navigate(
                                if (userType == "Producer") "ProducerHomeScreen"
                                else "ConsumerHomeScreen"
                            )
                        } else {
                            Toast.makeText(context, "Signup failed. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DarkGreen
                )
            ) {
                Text(if (isLoading) "Signing up..." else "Sign Up")
            }


            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Already have an account? Login here",
                color = Color.White,
                modifier = Modifier
                    .clickable { navController.navigate("LoginScreen") }
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

