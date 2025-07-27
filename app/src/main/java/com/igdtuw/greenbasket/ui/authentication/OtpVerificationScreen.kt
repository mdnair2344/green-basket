//OtpVerificationScreen
package com.igdtuw.greenbasket.ui.authentication

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.igdtuw.greenbasket.R
import androidx.compose.ui.res.painterResource

@Composable
fun OtpVerificationScreen(
    navController: NavController,
    viewModel: AuthenticationViewModel
) {
    val context = LocalContext.current
    val activity = context as Activity

    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var isOtpSent by remember { mutableStateOf(false) }

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
                contentDescription = "GreenBasket Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to GreenBasket",
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

            if (!isOtpSent) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number", color = Color.White) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (phoneNumber.length == 10 && phoneNumber.all { it.isDigit() }){
                            val fullPhone = "+91$phoneNumber"
                            viewModel.sendOtp(
                                phoneNumber = fullPhone,
                                activity = activity,
                                onCodeSent = { verId ->
                                    verificationId = verId
                                    isOtpSent = true
                                    Toast.makeText(context, "OTP sent successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(context, "Failed to send OTP: $error", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DarkGreen
                    )
                ) {
                    Text("Send OTP")
                }
            } else {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it },
                    label = { Text("Enter OTP", color = Color.White) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (otp.length == 6 && otp.all { it.isDigit() } && verificationId != null){
                            val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                            val currentUser = FirebaseAuth.getInstance().currentUser
                            if (currentUser != null) {
                                currentUser.linkWithCredential(credential)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(context, "Phone linked successfully!", Toast.LENGTH_SHORT).show()
                                            viewModel.getUserType(
                                                uid = currentUser.uid,
                                                onResult = { userType ->
                                                    navController.navigate(
                                                        if (userType == "Producer") "ProducerHomeScreen"
                                                        else "ConsumerHomeScreen"
                                                    )
                                                },
                                                onError = {
                                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                        } else {
                                            fallbackSignInWithPhone(credential, navController, viewModel, context)
                                        }
                                    }
                            } else {
                                fallbackSignInWithPhone(credential, navController, viewModel, context)
                            }
                        } else {
                            Toast.makeText(context, "Please enter valid OTP", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = DarkGreen
                    )
                ) {
                    Text("Verify OTP")
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = {
                    isOtpSent = false
                    otp = ""
                    verificationId = null
                }) {
                    Text("Resend OTP?", color = Color.White)
                }
            }
        }
    }
}

private fun fallbackSignInWithPhone(
    credential: PhoneAuthCredential,
    navController: NavController,
    viewModel: AuthenticationViewModel,
    context: android.content.Context
) {
    FirebaseAuth.getInstance().signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = FirebaseAuth.getInstance().currentUser
                user?.let {
                    viewModel.getUserType(
                        uid = user.uid,
                        onResult = { userType ->
                            navController.navigate(
                                if (userType == "Producer") "ProducerHomeScreen"
                                else "ConsumerHomeScreen"
                            )
                        },
                        onError = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                Toast.makeText(
                    context,
                    "Sign-in failed: ${task.exception?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
}