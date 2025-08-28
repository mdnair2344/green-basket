//ProfileScreen
package com.igdtuw.greenbasket

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.font.FontWeight
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant

@Composable
fun ReauthDialog(
    email: String,
    onDismiss: () -> Unit,
    onConfirm: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        title = { Text("Re-authentication Required") },
        text = {
            Column {
                Text("Enter your password to view/edit sensitive details:")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel(), onBack: () -> Unit = {}) {
    val userState = viewModel.userDetails.collectAsState()
    val savedCardsState = viewModel.savedCards.collectAsState()
    val savedBankDetailsState = viewModel.savedBankDetails.collectAsState()
    val isAuthenticatedState = viewModel.isAuthenticated.collectAsState()

    val user = userState.value
    val savedCards = savedCardsState.value
    val savedBankDetails = savedBankDetailsState.value
    val isAuthenticated = isAuthenticatedState.value

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Initialize editable fields with user data
    var editableName by remember(user.name) { mutableStateOf(user.name) }
    var editablePhone by remember(user.phone) { mutableStateOf(user.phone) }
    var editableAddress by remember(user.address) { mutableStateOf(user.address) }

    // State for bank detail input fields
    var newAccountNumber by remember { mutableStateOf("") }
    var newIfsc by remember { mutableStateOf("") }
    var newUpi by remember { mutableStateOf("") }

    // State for card detail input fields
    var newCardNumber by remember { mutableStateOf("") }
    var newCardType by remember { mutableStateOf("") }
    var newBankName by remember { mutableStateOf("") }
    var newCvv by remember { mutableStateOf("") }

    // State to manage which specific bank/card details are being viewed/edited
    var currentEditingBankDetails by remember { mutableStateOf<ProfileViewModel.BankDetails?>(null) }
    var currentEditingCard by remember { mutableStateOf<ProfileViewModel.Card?>(null) }
    var showDeleteImageDialog by remember { mutableStateOf(false) }




    LaunchedEffect(user) {
        // Update editable fields if user data changes, e.g., on initial fetch
        editableName = user.name
        editablePhone = user.phone
        editableAddress = user.address
        if (user.imageUri.isNotBlank()) {
            profileImageUri = Uri.parse(user.imageUri)
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            pendingAction?.invoke()
            pendingAction = null
        }else {
            profileImageUri = null
        }
    }


    if (showReauthDialog) {
        ReauthDialog(
            email = user.email,
            onDismiss = {
                showReauthDialog = false
                pendingAction = null // Clear pending action if dismissed
                viewModel.resetAuthenticationState() // Reset auth state on dismiss
            },
            onConfirm = { password ->
                viewModel.authenticateUser(password, context)
                showReauthDialog = false
            }
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            viewModel.uploadProfileImage(it, context)
        }
    }

    if (user.name.isBlank() && user.email.isBlank() && user.userType.isBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Your Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant
                )
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = profileImageUri ?: user.imageUri,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )

                Spacer(Modifier.width(12.dp))

                // Bin button beside the circle
                IconButton(
                    onClick = { showDeleteImageDialog = true },
                    enabled = (profileImageUri != null || user.imageUri.isNotBlank())
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Profile Picture",
                        tint = if (profileImageUri != null || user.imageUri.isNotBlank()) Color.Red else Color.Gray
                    )
                }
            }

            TextButton(
                onClick = { imagePickerLauncher.launch("image/*") }
            ) {
                Text("Change Profile Picture", color = ConsumerPrimaryVariant)
            }


            Spacer(modifier = Modifier.height(8.dp))

            Text("Basic Information", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = editableName, onValueChange = { editableName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = editablePhone, onValueChange = { editablePhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = editableAddress, onValueChange = { editableAddress = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    viewModel.updateBasicInfo(editableName, editablePhone, editableAddress, context)
                },
                modifier = Modifier.padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
            ) {
                Text("Save Basic Info", color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Email: ${user.email}", style = MaterialTheme.typography.bodyLarge)

            /*Spacer(modifier = Modifier.height(24.dp))
            Text("🔒 Bank Details", style = MaterialTheme.typography.titleLarge)

            if (savedBankDetails.isNotEmpty()) {
                savedBankDetails.forEach { bank ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (isAuthenticated && currentEditingBankDetails == bank) {
                                OutlinedTextField(value = newAccountNumber, onValueChange = { newAccountNumber = it }, label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = newIfsc, onValueChange = { newIfsc = it }, label = { Text("IFSC Code") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = newUpi, onValueChange = { newUpi = it }, label = { Text("UPI ID") }, modifier = Modifier.fillMaxWidth())

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = {
                                        viewModel.saveBankDetails(newAccountNumber, newIfsc, newUpi, context)
                                        currentEditingBankDetails = null
                                        viewModel.resetAuthenticationState()
                                        newAccountNumber = ""; newIfsc = ""; newUpi = ""
                                    }) { Text("Save", color = Color.White) }

                                    TextButton(onClick = {
                                        currentEditingBankDetails = null
                                        viewModel.resetAuthenticationState()
                                        newAccountNumber = ""; newIfsc = ""; newUpi = ""
                                    }) { Text("Cancel", color = Color.White) }
                                }
                            } else {
                                Text("Account Number: ******${bank.accountNumber.takeLast(4)}", color = Color.Black)
                                Text("IFSC Code: ******", color = Color.Black)
                                Text("UPI ID: ******", color = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        if (!isAuthenticated) {
                                            pendingAction = {
                                                currentEditingBankDetails = bank
                                                newAccountNumber = bank.accountNumber
                                                newIfsc = bank.ifscCode
                                                newUpi = bank.upi
                                            }
                                            showReauthDialog = true
                                        } else {
                                            currentEditingBankDetails = bank
                                            newAccountNumber = bank.accountNumber
                                            newIfsc = bank.ifscCode
                                            newUpi = bank.upi
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
                                ) {
                                    Text("View & Edit", color = Color.White)
                                }


                            }
                        }
                    }
                }
            } else {
                Text("No bank details saved. Add new ones below.")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Add New Bank Details", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(value = newAccountNumber, onValueChange = { newAccountNumber = it }, label = { Text("Account Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newIfsc, onValueChange = { newIfsc = it }, label = { Text("IFSC Code") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newUpi, onValueChange = { newUpi = it }, label = { Text("UPI ID") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val action = {
                        viewModel.saveBankDetails(newAccountNumber, newIfsc, newUpi, context)
                        newAccountNumber = ""; newIfsc = ""; newUpi = ""
                    }
                    if (!isAuthenticated) {
                        pendingAction = action
                        showReauthDialog = true
                    } else {
                        action()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
            ) {
                Text("Save New Bank Details", color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("💳 Saved Cards", style = MaterialTheme.typography.titleLarge)

            if (savedCards.isEmpty()) {
                Text("No cards saved. Add new ones below.")
            } else {
                savedCards.forEach { card ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = ConsumerCardBackground1) // MATCHED WITH BANK DETAILS
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (isAuthenticated && currentEditingCard == card) {
                                OutlinedTextField(value = newCardNumber, onValueChange = { newCardNumber = it }, label = { Text("Card Number") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = newCardType, onValueChange = { newCardType = it }, label = { Text("Card Type") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = newBankName, onValueChange = { newBankName = it }, label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = newCvv, onValueChange = { newCvv = it }, label = { Text("CVV") }, modifier = Modifier.fillMaxWidth())

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = {
                                        viewModel.saveCardDetails(card.id, newCardNumber, newCardType, newBankName, newCvv, context)
                                        currentEditingCard = null
                                        viewModel.resetAuthenticationState()
                                        newCardNumber = ""; newCardType = ""; newBankName = ""; newCvv = ""
                                    }) { Text("Save", color = Color.White) }

                                    TextButton(onClick = {
                                        currentEditingCard = null
                                        viewModel.resetAuthenticationState()
                                        newCardNumber = ""; newCardType = ""; newBankName = ""; newCvv = ""
                                    }) { Text("Cancel", color = Color.White) }
                                }
                            } else {
                                Text("Card Number: ******${card.cardNumber.takeLast(4)}", color = Color.Black)
                                Text("Type: ${card.cardType}", color = Color.Black)
                                Text("Bank: ${card.bankName}", color = Color.Black)
                                Text("CVV: ***", color = Color.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        if (!isAuthenticated) {
                                            pendingAction = {
                                                currentEditingCard = card
                                                newCardNumber = card.cardNumber
                                                newCardType = card.cardType
                                                newBankName = card.bankName
                                                newCvv = card.cvv
                                            }
                                            showReauthDialog = true
                                        } else {
                                            currentEditingCard = card
                                            newCardNumber = card.cardNumber
                                            newCardType = card.cardType
                                            newBankName = card.bankName
                                            newCvv = card.cvv
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
                                ) {
                                    Text("View & Edit", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))
            Text("Add New Card", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(value = newCardNumber, onValueChange = { newCardNumber = it }, label = { Text("Card Number") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newCardType, onValueChange = { newCardType = it }, label = { Text("Card Type (Visa, MasterCard, etc.)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newBankName, onValueChange = { newBankName = it }, label = { Text("Bank Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = newCvv, onValueChange = { newCvv = it }, label = { Text("CVV") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    val action = {
                        viewModel.saveCardDetails(null, newCardNumber, newCardType, newBankName, newCvv, context)
                        newCardNumber = ""; newCardType = ""; newBankName = ""; newCvv = ""
                    }
                    if (!isAuthenticated) {
                        pendingAction = action
                        showReauthDialog = true
                    } else {
                        action()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ConsumerPrimaryVariant)
            ) {
                Text("Save New Card", color = Color.White)
            }*/
        }
    }
}
