//ConsumerApp
package com.igdtuw.greenbasket.ui.consumer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings.Global.getString
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.firebase.firestore.FirebaseFirestore
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme
import kotlinx.coroutines.launch
import com.igdtuw.greenbasket.ProfileScreen
import com.igdtuw.greenbasket.ProfileViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.firebase.auth.FirebaseAuth
import com.igdtuw.greenbasket.UnderDevelopmentDialog
import com.igdtuw.greenbasket.ui.authentication.GoogleAuthUiClient
import com.igdtuw.greenbasket.ui.authentication.LoginScreen
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.igdtuw.greenbasket.R
import com.igdtuw.greenbasket.navigation.SplashScreen
import kotlinx.coroutines.delay



@SuppressLint("StateFlowValueCalledInComposition")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ConsumerApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val navController = rememberNavController()
    val sharedViewModel: SharedViewModel = hiltViewModel()
    val orderViewModel: OrderViewModel = viewModel()
    val cartViewModel: CartViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val productDetailViewModel: ProductDetailViewModel = viewModel()
    val reviewViewModel: MyReviewsViewModel = hiltViewModel()

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


    LaunchedEffect(Unit) {
        Log.d("NAV_DEBUG", "ConsumerApp Launched")
        Toast.makeText(context, "Inside ConsumerApp", Toast.LENGTH_SHORT).show()
    }

    GreenBasketTheme(isProducer = false) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ConsumerSideDrawer(
                    navController = navController,
                    closeDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            NavHost(navController = navController, startDestination = "consumer_dashboard") {

                composable("consumer_dashboard") {
                    ConsumerDashboardScreen(
                        navController = navController,
                        openDrawer = { scope.launch { drawerState.open() } }
                    )
                }
                composable(Routes.Profile) {
                    ProfileScreen(profileViewModel, onBack = { navController.popBackStack() })
                }

                composable("HomeScreen") {
                    ConsumerDashboardScreen(
                        navController = navController,
                        openDrawer = { scope.launch { drawerState.open() } }
                    )
                }

                composable(
                    "shop_category_screen/{category}",
                    arguments = listOf(navArgument("category") { type = NavType.StringType })
                ) { backStackEntry ->
                    val category = backStackEntry.arguments?.getString("category") ?: ""
                    ShopCategoryScreen(category = category, navController = navController)
                }




                composable(Routes.Help) { HelpScreen(navController) }
                composable(Routes.Settings) { SettingsScreen(navController) }

                composable("terms_screen") { PolicyScreen("Terms & Conditions", termsContent, navController) }
                composable("privacy_screen") { PolicyScreen("Privacy Policy", privacyContent, navController) }
                composable("cancellation_screen") { PolicyScreen("Cancellation Policy", cancellationContent, navController) }
                composable("shipping_screen") { PolicyScreen("Shipping Policy", shippingContent, navController) }
                composable("contact_screen") { PolicyScreen("Contact Us", contactContent, navController) }
                composable("about_us_screen") { PolicyScreen("About Us", aboutUsContent,navController) }


                composable(Routes.Chat) { UnderDevelopmentDialog(navController) }
                composable(
                    route = "my_reviews_screen/{consumerId}",
                    arguments = listOf(navArgument("consumerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val consumerId = backStackEntry.arguments?.getString("consumerId") ?: ""
                    MyReviewsScreen(
                        navController = navController,
                        sharedViewModel = sharedViewModel,
                        consumerId = consumerId
                    )
                }




                composable("deals_screen") { DealsScreen(navController) }
                composable("refunds_screen") { UnderDevelopmentDialog(navController)  }
                composable("shop_products") {
                    ShopProductsScreen(navController)
                }
                composable("bulk_event_order") { UnderDevelopmentDialog(navController) }
                composable("my_orders") {
                    MyOrdersScreen(navController, sharedViewModel, orderViewModel)
                }
                composable("live_cctv_list") { UnderDevelopmentDialog(navController) }

                /*composable(
                    "cctv_feed/{producerId}",
                    arguments = listOf(navArgument("producerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    CCTVFeedScreen(
                        navController = navController,
                        producerId = backStackEntry.arguments?.getString("producerId")
                    )
                }*

                composable("add_cctv_producer") { AddProducerCCTVScreen(navController) }*/
                composable("certifications_list") { ConsumerCertificatesScreen(navController) }


                composable(
                    "cctv_feed/{producerId}",
                    arguments = listOf(navArgument("producerId") { type = NavType.StringType })
                ) { backStackEntry ->
                    /*CCTVFeedScreen(
                        navController = navController,
                        producerId = backStackEntry.arguments?.getString("producerId")
                    ) */
                    UnderDevelopmentDialog(navController) }



                composable("my_cart") { MyCartScreen(navController, sharedViewModel) }
                composable("wishlist") { WishlistScreen(navController, sharedViewModel) }
                composable(
                    "product_details/{producerId}/{productId}",
                    arguments = listOf(
                        navArgument("producerId") { type = NavType.StringType },
                        navArgument("productId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val producerId = backStackEntry.arguments?.getString("producerId")
                    val productId = backStackEntry.arguments?.getString("productId")
                    val consumerId by sharedViewModel.userId.collectAsState()

                    if (consumerId.isNullOrBlank()) {
                        // Show a loading spinner while consumerId is null
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val producerId = backStackEntry.arguments?.getString("producerId")
                        val productId = backStackEntry.arguments?.getString("productId")

                        if (!producerId.isNullOrBlank() && !productId.isNullOrBlank()) {
                            ProductDetailScreen(
                                navController = navController,
                                producerId = producerId,
                                cropId = productId,
                                consumerId = consumerId!!,
                                viewModel = productDetailViewModel
                            )
                        } else {
                            Text("Invalid or missing product details", modifier = Modifier.padding(16.dp))
                        }
                    }


                }



                composable("chat_with_producers") {
                    UnderDevelopmentDialog(navController)
                }

                composable("select_address") {
                    AddressConfirmationScreen(navController, sharedViewModel)
                }

                composable(
                    route = "payment_screen?orderId={orderId}",
                    arguments = listOf(
                        navArgument("orderId") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        }
                    )
                )
                { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                    PaymentScreen(navController = navController, orderId = orderId, sharedViewModel, cartViewModel)
                }




                composable(
                    route = "order_status_screen?orderId={orderId}&isSuccess={isSuccess}&deliveryOption={deliveryOption}",
                    arguments = listOf(
                        navArgument("orderId") { type = NavType.StringType },
                        navArgument("isSuccess") { type = NavType.BoolType; defaultValue = false },
                        navArgument("deliveryOption") { type = NavType.StringType; defaultValue = "home" }
                    )
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId")
                        ?: throw IllegalArgumentException("Missing orderId")
                    val isSuccess = backStackEntry.arguments?.getBoolean("isSuccess") ?: false
                    val deliveryOption = backStackEntry.arguments?.getString("deliveryOption") ?: "home"

                    OrderStatusScreen(
                        navController = navController,
                        orderId = orderId,
                        isSuccess = isSuccess,
                        deliveryOption = deliveryOption,
                        orderViewModel = orderViewModel,
                        sharedViewModel = sharedViewModel
                    )
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






                composable(
                    route = "waitingForApproval/{orderId}",
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                    WaitingForApprovalScreen(navController, orderId, FirebaseFirestore.getInstance())
                }



            }


        }
    }
}

@Composable
fun TextPlaceholderScreen(screenName: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = screenName, style = MaterialTheme.typography.headlineMedium)
    }
}
