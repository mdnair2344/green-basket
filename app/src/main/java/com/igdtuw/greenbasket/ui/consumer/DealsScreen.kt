//DealsScreen
package com.igdtuw.greenbasket.ui.consumer

import android.content.*
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.R
import com.igdtuw.greenbasket.ui.theme.ConsumerCardBackground1
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class DiscountOffer(
    val id: String = "",
    val title: String = "",
    val discountValue: Double = 0.0,
    val validTill: String = "",
    val promoCode: String = "",
    val appliesToAllCrops: Boolean = false,
    val cropNames: List<String> = emptyList()
)

data class DisplayOffer(
    val offer: DiscountOffer,
    val producerName: String,
    val farmName: String
)

val DarkGreen = Color(0xFF1B5E20)
val TranslucentRed = Color(0xAAE57373)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealsScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val sharedViewModel: SharedViewModel = hiltViewModel()

    var displayOffers by remember { mutableStateOf<List<DisplayOffer>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null

        scope.launch(Dispatchers.IO) {
            try {
                val producersSnapshot = db.collection("users")
                    .whereEqualTo("userType", "Producer")
                    .get().await()

                val offerList = mutableListOf<DisplayOffer>()

                for (producerDoc in producersSnapshot.documents) {
                    val producerId = producerDoc.id
                    val userDoc = db.collection("users").document(producerId).get().await()
                    val producerName = userDoc.getString("name") ?: "Unknown Producer"
                    val farmName = userDoc.getString("farmName") ?: "Unknown Farm"

                    val discountSnapshot = db.collection("producers")
                        .document(producerId)
                        .collection("discount_offers")
                        .get().await()

                    for (doc in discountSnapshot.documents) {
                        val offer = doc.toObject(DiscountOffer::class.java)?.copy(id = doc.id)
                        if (offer != null) {
                            offerList.add(DisplayOffer(offer, producerName, farmName))
                        }
                    }
                }

                displayOffers = offerList
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Error loading offers: ${e.message}"
                isLoading = false
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deals & Offers", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel)
                }
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                errorMessage != null -> Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                displayOffers.isEmpty() -> EmptyDealsState("Offers")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(displayOffers.sortedBy { it.offer.validTill }) { displayOffer ->
                        OfferItemCard(displayOffer)
                    }
                }
            }
        }
    }
}

@Composable
fun OfferItemCard(displayOffer: DisplayOffer) {
    val offer = displayOffer.offer
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val context = LocalContext.current

    val (bgColor, daysLeftText, isValid) = remember(offer.validTill) {
        try {
            val endDate = sdf.parse(offer.validTill)
            val today = Calendar.getInstance().time
            val diff = endDate.time - today.time
            val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt()
            val text = when {
                daysLeft < 0 -> "Expired"
                daysLeft == 0 -> "Ends today"
                daysLeft == 1 -> "Ends in 1 day"
                else -> "Ends in $daysLeft days"
            }
            Triple(if (daysLeft >= 0) ConsumerCardBackground1 else TranslucentRed, text, daysLeft >= 0)
        } catch (e: Exception) {
            Triple(TranslucentRed, "Invalid date", false)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(offer.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("By ${displayOffer.producerName} from ${displayOffer.farmName}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(6.dp))

            if (!offer.appliesToAllCrops && offer.cropNames.isNotEmpty()) {
                Text("Applies to: ${offer.cropNames.joinToString()}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Use code: ${offer.promoCode}", fontWeight = FontWeight.SemiBold, color = DarkGreen)

                if (isValid) {
                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Promo Code", offer.promoCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Copy", fontSize = 12.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Use promo code ${offer.promoCode} to get ${offer.discountValue}% off! Valid till ${offer.validTill}."
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Promo Code"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Share", fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text("${offer.discountValue}% off", fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = daysLeftText,
                color = Color.Red.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun EmptyDealsState(dealType: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_no_deals_placeholder),
            contentDescription = "No $dealType",
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No $dealType available right now!",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Check back soon for exciting offers!",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
