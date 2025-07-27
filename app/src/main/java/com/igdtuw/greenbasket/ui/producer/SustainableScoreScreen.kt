package com.igdtuw.greenbasket.ui.producer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.igdtuw.greenbasket.R
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn


// Custom card colors
val ProducerCardBackground2 = Color(0xFFD0F8CE)
val ProducerCardBackground3 = Color(0xFF81C784)
val ProducerCardBackground4 = Color(0xFFB2DFDB)
val SoftYellow = Color(0xFFFFF9C4)
val SoftPurple = Color(0xFFD1C4E9)
val SoftGreen = Color(0xFFC8E6C9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SustainableScoreScreen(
    viewModel: SustainabilityViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val score = viewModel.sustainableScore.value.roundToInt()
    val animatedProgress by animateFloatAsState(targetValue = score / 100f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Sustainability Score",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1B5E20) // Dark Green
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    "🌿 Your efforts toward a greener future!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF1B5E20), // Dark Green
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_award),
                            contentDescription = "Sustainability Badge",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(150.dp)
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(180.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = animatedProgress,
                                strokeWidth = 12.dp,
                                color = Color(0xFF1B5E20), // Dark green
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "$score / 100",
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                ScoreBreakdownCard(
                    title = "Organic Crops",
                    value = viewModel.crops.size.toString(),
                    icon = Icons.Default.LocalOffer,
                    color = ProducerCardBackground2
                )
            }

            item {
                ScoreBreakdownCard(
                    title = "Completed Orders",
                    value = viewModel.completedOrders.value.toString(),
                    icon = Icons.Default.CheckCircle,
                    color = ProducerCardBackground4
                )
            }

            item {
                ScoreBreakdownCard(
                    title = "Average Rating",
                    value = "%.1f".format(viewModel.rating.value),
                    icon = Icons.Default.Star,
                    color = SoftYellow
                )
            }

            item {
                ScoreBreakdownCard(
                    title = "Reviews Count",
                    value = viewModel.reviewCount.value.toString(),
                    icon = Icons.Default.RateReview,
                    color = SoftPurple
                )
            }

            item {
                ScoreBreakdownCard(
                    title = "Valid Certificates",
                    value = viewModel.validCertificateCount.value.toString(),
                    icon = Icons.Default.Verified,
                    color = SoftGreen
                )
            }
        }
    }
}

@Composable
fun ScoreBreakdownCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    val isHeader = color == Color(0xFF1B5E20) // Used if needed later

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color(0xFF1B5E20).copy(alpha = 0.2f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
        }
    }
}
