package com.igdtuw.greenbasket.ui.producer

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme
import com.igdtuw.greenbasket.ProfileViewModel
import com.igdtuw.greenbasket.R
import com.igdtuw.greenbasket.UnderDevelopmentDialog
import com.igdtuw.greenbasket.navigation.SplashScreen
import com.igdtuw.greenbasket.ui.authentication.GoogleAuthUiClient
import com.igdtuw.greenbasket.ui.authentication.LoginScreen
import com.igdtuw.greenbasket.ui.consumer.PolicyScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProducerApp() {
    val navController = rememberNavController()

    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val profileViewModel: ProfileViewModel = hiltViewModel()
    val trackOrdersViewModel: TrackOrdersViewModel = hiltViewModel()
    val orderViewModel: ProducerOrdersViewModel = hiltViewModel()
    val revenueViewModel: RevenueViewModel = viewModel()

    // Get current Firebase user id
    val producerId = remember {
        FirebaseAuth.getInstance().currentUser?.uid
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id)) // ✅ use context
        .requestEmail()
        .build()


    // Step 2: Create GoogleAuthUiClient
    val googleAuthUiClient = GoogleAuthUiClient(
        context = context, // ✅ not 'this' if you're not in an Activity
        auth = FirebaseAuth.getInstance(),
        firestore = FirebaseFirestore.getInstance(),
        gso = gso
    )

    GreenBasketTheme(isProducer = true) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ProducerSideDrawer(
                    onHomeClick  = {
                        scope.launch { drawerState.close() }
                        navController.navigate("HomeScreen")
                    },

                    onInventoryClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("InventoryScreen")
                    },
                    onOrdersClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("TrackOrdersScreen")
                    },
                    onEarningsClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("RevenueScreen")
                    },
                    onSettingsClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("SettingsScreen")
                    },
                    onCropCalendarClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("CropCalendarScreen")
                    },
                    onBulkOrderNotificationClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("BulkOrderScreen")
                    },
                    onAcceptRejectOrdersClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("PendingOrdersScreen")
                    },
                    onLiveChatClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("LiveChatScreen")
                    },
                    onRestockClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("RestockScreen")
                    },
                    onNearbyProducersClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("NearbyProducersScreen")
                    },
                    onUploadMediaClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("UploadMediaScreen")
                    },
                    onDiscountFeaturesClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("DiscountFeaturesScreen")
                    },
                    onDemoGuidanceClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("DemoGuidanceVideoScreen")
                    },
                    onReviewsClick = {
                        scope.launch { drawerState.close() }
                        if (producerId != null) {
                            navController.navigate("my_reviews_screen/$producerId")
                        }
                    },
                    onLogoutClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("logout")
                    },
                    onProfileClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate("ProfileScreen")
                    }
                )
            }
        ) {
            NavHost(navController = navController, startDestination = "HomeScreen") {
                composable("HomeScreen") {
                    ProducerHomeScreen(navController, drawerState, scope)
                }


                composable("InventoryScreen") {
                    InventoryScreen(navController)
                }

                composable("TrackOrdersScreen") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        TrackOrdersScreen(trackOrdersViewModel, onBack = { navController.popBackStack() })
                    }
                }

                composable("ProfileScreen") {
                    ProducerProfileScreen(profileViewModel, onBack = { navController.popBackStack() })
                }

                composable("PendingOrdersScreen") {
                    PendingOrdersScreenForProducer(navController, orderViewModel)
                }

                composable("RevenueScreen") {
                    if (producerId != null) {
                        RevenueScreen(
                            producerId = producerId,
                            viewModel = revenueViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    } else {
                        Text("Please log in to view your revenue.")
                    }
                }

                composable("ManageCropsScreen") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ManageCropsScreen(navController)
                    }
                }

                composable("CropCalendarScreen") { CropCalendarScreen(onBack = { navController.popBackStack() }) }

                composable("BulkOrderScreen") {
                    UnderDevelopmentDialog(navController)
                }

                composable("LiveChatScreen") {
                    UnderDevelopmentDialog(navController)
                }

                composable("RestockScreen") {
                    RestockScreen(navController, onBack = { navController.popBackStack() })
                }

                composable("NearbyProducersScreen") {
                    UnderDevelopmentDialog(navController)
                }

                composable("UploadMediaScreen") {
                    UploadMediaScreen(onBack = { navController.popBackStack() })
                }

                composable("DiscountFeaturesScreen") {
                    DiscountFeaturesScreen(onBack = { navController.popBackStack() })
                }


                composable("terms_screen") {
                    PolicyPage(
                        "Terms & Conditions",
                        termsContent,
                        navController
                    )
                }
                composable("privacy_screen") { PolicyPage("Privacy Policy", privacyContent, navController) }
                composable("cancellation_screen") { PolicyPage("Cancellation Policy", cancellationContent, navController) }
                composable("shipping_screen") { PolicyPage("Shipping Policy", shippingContent, navController) }
                composable("contact_screen") { PolicyPage("Contact Us", contactContent, navController) }
                composable("about_us_screen") { PolicyPage("About Us", aboutUsContent,navController) }


                composable("SettingsScreen") {
                    SettingsPage(navController)
                }

                composable("DemoGuidanceVideoScreen") {
                    DemoGuidanceVideoScreen(onBack = { navController.popBackStack() })
                }

                composable("SustainableScoreScreen") {
                    SustainableScoreScreen(onBack = { navController.popBackStack() })
                }

                composable(
                    route = "my_reviews_screen/{producerId}",
                    arguments = listOf(navArgument("producerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val pid = backStackEntry.arguments?.getString("producerId") ?: ""
                    ReviewsAndRatingsScreen(producerId = pid, onBack = { navController.popBackStack() })
                }

                composable("splash") {
                    SplashScreen(navController = navController)
                }


                composable("LoginScreen") {
                    LoginScreen(
                        navController = navController,
                        viewModel = hiltViewModel(), // or your preferred ViewModel provider
                        googleAuthUiClient = googleAuthUiClient // Inject or pass this
                    )
                }


                composable("logout") {
                    val context = LocalContext.current

                    LaunchedEffect(Unit) {
                        FirebaseAuth.getInstance().signOut()  // ✅ Firebase logout
                        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        prefs.edit().clear().apply()          // ✅ Clear prefs

                        delay(200) // ⏳ Give FirebaseAuth time to reflect logout
                        navController.navigate("LoginScreen") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            launchSingleTop = true
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

            }
        }
    }
}


