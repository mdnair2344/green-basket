//ProducerProfileScreen
package com.igdtuw.greenbasket.ui.producer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.igdtuw.greenbasket.ProfileViewModel.ApiStatus
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.igdtuw.greenbasket.ProfileViewModel
import com.igdtuw.greenbasket.ReauthDialog
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import coil.compose.rememberAsyncImagePainter
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProducerProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    kycViewModel: KycViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val razorpayAccountInfo by viewModel.razorpayAccountInfo.collectAsState() // Observe the new StateFlow


    val user by viewModel.userDetails.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val acceptOnly5To10 by viewModel.acceptOnly5To10
    val isAuthenticatedState = viewModel.isAuthenticated.collectAsState()

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var showKycForm by remember { mutableStateOf(false) }
    val kycCompleted by kycViewModel.kycCompleted.collectAsState()

    val merchantStatus by viewModel.razorpayMerchantCreationStatus.collectAsState()
    var showDeleteImageDialog by remember { mutableStateOf(false) }




    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            profileImageUri = it

            uploadToCloudinary(
                context = context,
                fileUri = it,
                folder = "Medias/$uid/Profile",
                onSuccess = { imageUrl, _ ->
                    viewModel.uploadProfileImage(profileImageUri!!, context)
                    Toast.makeText(context, "Upload successful", Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Toast.makeText(context, "Upload failed: $error", Toast.LENGTH_LONG).show()
                }
            )
        }
    }



    LaunchedEffect(user) {
        if (user.imageUri.isNotBlank()) {
            profileImageUri = Uri.parse(user.imageUri)
        }else {
            profileImageUri = null
        }
        viewModel.loadAccept5to10Status()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Producer Profile",style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",tint = Color.White)

                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        },containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .background(Color.White)
                .verticalScroll(scrollState)
        ) {
            val imageUrl = profileImageUri?.toString() ?: user.imageUri

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        tint = ConsumerCardBackground1
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Bin (delete) button beside the circle
                IconButton(
                    onClick = { showDeleteImageDialog = true },
                    enabled = (!imageUrl.isNullOrBlank())
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Profile Picture",
                        tint = if (!imageUrl.isNullOrBlank()) Color.Red else Color.Gray
                    )
                }
            }



            TextButton(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                }
            ) {
                Text("Update Profile Picture", color = ConsumerPrimaryVariant)
            }

            if (showDeleteImageDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteImageDialog = false },
                    title = { Text("Delete profile picture?") },
                    text = { Text("This will remove your photo from Firestore and from this screen.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.removeProfileImage(context) // your VM function
                                profileImageUri = null                // clear local state so UI updates immediately
                                showDeleteImageDialog = false
                                Toast.makeText(context, "Profile picture removed", Toast.LENGTH_SHORT).show()
                            }
                        ) { Text("Delete", color = Color.Red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteImageDialog = false }) { Text("Cancel") }
                    }
                )
            }




            Spacer(modifier = Modifier.height(12.dp))

            Text("Name: ${user.name}")
            Text("Email: ${user.email}")
            Text("Razorpay Linked Account ID: ${user.linkedAccountId.ifBlank { "N/A" }}") // From userDetails
            // Display information from the new RazorpayAccountInfo StateFlow
            Text("Razorpay KYC Status: ${razorpayAccountInfo.razorpayKycStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}")
            Text("Razorpay Account Status: ${razorpayAccountInfo.razorpayAccountStatus.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}")


            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Checkbox(
                    checked = acceptOnly5To10,
                    onCheckedChange = {
                        viewModel.updateAccept5to10Status(it)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ConsumerPrimaryVariant,
                        uncheckedColor = Color.Gray,
                        checkmarkColor = Color.White
                    )
                )
                Text("Accept orders only b/w 5:00 a.m. & 10:00 p.m.")
            }


            Spacer(modifier = Modifier.height(16.dp))

            val isAccountCreated = user.linkedAccountId.isNotBlank()

            Button(
                onClick = {
                    coroutineScope.launch {
                        viewModel.createRazorpayMerchantAccount(context)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isAccountCreated,
                colors = if (!isAccountCreated) {
                    // Account not created: use ConsumerPrimaryVariant background and white text
                    ButtonDefaults.buttonColors(
                        containerColor = ConsumerPrimaryVariant,
                        contentColor = Color.White
                    )
                } else {
                    // Account created: make it look "flat"
                    ButtonDefaults.buttonColors(
                        containerColor = ConsumerCardBackground1,
                        contentColor = ConsumerPrimaryVariant
                    )
                }
            ) {
                Text(
                    text = if (isAccountCreated) "Razorpay Account Created" else "Create Razorpay Merchant Account",
                    color = if (isAccountCreated) ConsumerPrimaryVariant else Color.White
                )
            }

            merchantStatus?.let { status ->
                when (status) {
                    is ApiStatus.Loading -> Text("Creating merchant account...", color = Color.Gray)
                    is ApiStatus.Success -> Text(status.message, color = ConsumerPrimaryVariant)
                    is ApiStatus.Error -> Text("Error: ${status.message}", color = Color.Red)
                    is ApiStatus.Idle -> {}
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Complete your KYC",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            /*Text(
                text = "Click here to complete the KYC form",
                color = ConsumerPrimaryVariant,
                modifier = Modifier
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com/forms/d/e/1FAIpQLSfUYvMNzqkGBEZ6aiJfh-dZ_W40gXjAh57n6NybopM4qMzaxw/viewform?usp=header"))
                        context.startActivity(intent)
                    }
                    .padding(4.dp),
                style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline)
            )*/

            /* Button(
                onClick = {
                    showKycForm = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConsumerPrimaryVariant, // consumer primary
                    contentColor = Color.White          // white text
                ),
                enabled = !kycCompleted
            ) {
                Text(if (kycCompleted) "KYC Completed" else "Complete KYC")
            }

            Button(
                onClick = {
                    showKycForm = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !kycCompleted
            ) {
                Text(if (kycCompleted) "KYC Completed" else "Complete KYC")
            }

            if (showKycForm) {
                KycFormScreen(
                    onSubmit = { accountNumber, confirmAccountNumber, ifscCode, beneficiaryName ->
                        val producerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val merchantId = user.linkedAccountId

                        kycViewModel.submitKycToBackend(
                            producerId = producerId,
                            merchantId = merchantId,
                            accountNumber = accountNumber,
                            ifsc = ifscCode,
                            beneficiaryName = beneficiaryName,
                            context = context
                        )
                        showKycForm = false
                    }
                )
            }*/

            // Button to initiate Razorpay Hosted KYC Onboarding
            Button(
                onClick = {
                    if (user.linkedAccountId.isNotBlank()) {
                        coroutineScope.launch {
                            // Call the new function in ViewModel
                            viewModel.generateAndOpenRazorpayOnboardingLink(user.linkedAccountId, context)
                        }
                    } else {
                        Toast.makeText(context, "Please create a Razorpay Merchant Account first.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConsumerPrimaryVariant, // consumer primary
                    contentColor = Color.White          // white text
                ),
                modifier = Modifier.fillMaxWidth(),
                // Enabled if linked account exists AND Razorpay KYC is NOT 'verified'
                enabled = user.linkedAccountId.isNotBlank() && razorpayAccountInfo.razorpayKycStatus != "verified"
            ) {
                Text(if (razorpayAccountInfo.razorpayKycStatus == "verified") "KYC Completed on Razorpay" else "Complete KYC on Razorpay")
            }
        }
    }
}