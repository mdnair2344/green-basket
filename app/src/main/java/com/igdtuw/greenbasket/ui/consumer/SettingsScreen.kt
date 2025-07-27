//SettingsScreen
package com.igdtuw.greenbasket.ui.consumer

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.igdtuw.greenbasket.NavControllerHolder.navController
import com.igdtuw.greenbasket.ui.theme.ConsumerPrimaryVariant
import com.igdtuw.greenbasket.ui.theme.GreenBasketTheme // Assuming your theme is here




@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController? = null, // MAKE NAVCONTROLLER NULLABLE HERE
    onBackClick: () -> Unit = { navController?.popBackStack() } // Use safe call for popBackStack
) {
    val context = LocalContext.current

    var isDarkThemeEnabled by remember { mutableStateOf(false) }
    var areNotificationsEnabled by remember { mutableStateOf(true) }
    val sharedViewModel: SharedViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel) // Pass sharedViewModel
                }
            )
        }, containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(Color.White) ,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingItem(
                title = "Dark Theme",
                description = "Enable or disable dark mode.",
                trailingContent = {
                    Switch(
                        checked = isDarkThemeEnabled,
                        onCheckedChange = {
                            isDarkThemeEnabled = it
                            Toast.makeText(context, "Dark Theme: $it", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF66BB6A),
                            checkedTrackColor = Color(0xFF81C784)
                        )
                    )
                }
            )

            HorizontalDivider()

            SettingItem(
                title = "Notifications",
                description = "Receive updates about your orders and offers.",
                trailingContent = {
                    Switch(
                        checked = areNotificationsEnabled,
                        onCheckedChange = {
                            areNotificationsEnabled = it
                            Toast.makeText(context, "Notifications: $it", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF66BB6A),
                            checkedTrackColor = Color(0xFF81C784)
                        )
                    )
                }
            )

            HorizontalDivider()

            SettingItem(
                title = "About Us",
                description = "Learn more about Green Basket.",
                trailingContent = {
                    // No trailing content for a clickable item, but you could add an arrow icon
                },
                onClick = {

                    navController?.navigate("about_us_screen")
                }
            )

            HorizontalDivider()

            SettingItem(
                title = "App Version",
                description = "1.0.0",
                trailingContent = {}
            )

            HorizontalDivider()

            SettingItem("Terms & Conditions", "Read our terms of service.", trailingContent = {}, onClick = {
                navController?.navigate("terms_screen")
            })

            HorizontalDivider()

            SettingItem("Privacy Policy", "Understand how we use your data.", trailingContent = {}, onClick = {
                navController?.navigate("privacy_screen")
            })

            HorizontalDivider()

            SettingItem("Cancellation Policy", "Refunds and cancellation guidelines.", trailingContent = {}, onClick = {
                navController?.navigate("cancellation_screen")
            })

            HorizontalDivider()

            SettingItem("Shipping Policy", "Delivery and shipping process.", trailingContent = {}, onClick = {
                navController?.navigate("shipping_screen")
            })

            HorizontalDivider()

            SettingItem("Contact Us", "Reach out to our support team.", trailingContent = {}, onClick = {
                navController?.navigate("contact_screen")
            })


        }
    }
}

@Composable
fun SettingItem(
    title: String,
    description: String,
    trailingContent: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF1B5E20)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        trailingContent()
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyScreen(title: String, content: String, navController: NavController? = null,) {
    val sharedViewModel: SharedViewModel = hiltViewModel()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${title}", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConsumerPrimaryVariant,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    CartWishlistActions(navController, sharedViewModel) // Pass sharedViewModel
                }
            )
        }, containerColor = Color.White
    ){ padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

val termsContent = """
For the purpose of these Terms and Conditions, The term "we", "us", "our" used anywhere on this page shall mean Jaismeen & Devika, whose registered/operational office is Indira Gandhi Delhi Technical University, Kashmere Gate, New Delhi North Delhi DELHI 110006 . 
"you", “your”, "user", “visitor” shall mean any natural or legal person who is visiting our website and/or agreed to purchase from us. 
Your use of the application and/or purchase from us are governed by following Terms and Conditions: 
The content of the pages of this website is subject to change without notice. 
Neither we nor any third parties provide any warranty or guarantee as to the accuracy, timeliness, performance, completeness or suitability of the information and materials found or offered on this website for any particular purpose. 
You acknowledge that such information and materials may contain inaccuracies or errors and we expressly exclude liability for any such inaccuracies or errors to the fullest extent permitted by law. 
Your use of any information or materials on our website and/or product pages is entirely at your own risk, for which we shall not be liable. 
It shall be your own responsibility to ensure that any products, services or information available through our website and/or product pages meet your specific requirements. 
Our website contains material which is owned by or licensed to us. This material includes, but are not limited to, the design, layout, look, appearance and graphics. 
Reproduction is prohibited other than in accordance with the copyright notice, which forms part of these terms and conditions. 
All trademarks reproduced in our website which are not the property of, or licensed to, the operator are acknowledged on the website. 
Unauthorized use of information provided by us shall give rise to a claim for damages and/or be a criminal offense. 
From time to time our website may also include links to other websites. These links are provided for your convenience to provide further information. 
You may not create a link to our website from another website or document without Jaismeen or Devika's prior written consent. 
Any dispute arising out of use of our website and/or purchase with us and/or any engagement with us is subject to the laws of India . 
We, shall be under no liability whatsoever in respect of any loss or damage arising directly or indirectly out of the decline of authorization for any Transaction, on Account of the Cardholder having exceeded the preset limit mutually agreed by us with our acquiring bank from time to time.
""".trimIndent()

val privacyContent = """
This privacy policy sets out how Devika & Jaismeen use and protect any information that you give Devika & Jaismeen when you visit their website and/or agree to purchase from them. 
Devika & Jaismeen are committed to ensuring that your privacy is protected. Should we ask you to provide certain information by which you can be identified when using this website, and then you can be assured that it will only be used in accordance with this privacy statement. 
Devika & Jaismeen may change this policy from time to time by updating this page. You should check this page from time to time to ensure that you adhere to these changes. 
We may collect the following information: Name Contact information including email address Demographic information such as postcode, preferences and interests, if required Other information relevant to customer surveys and/or offers 
What we do with the information we gather We require this information to understand your needs and provide you with a better service, and in particular for the following reasons: Internal record keeping. We may use the information to improve our products and services. 
We may periodically send promotional emails about new products, special offers or other information which we think you may find interesting using the email address which you have provided. 
From time to time, we may also use your information to contact you for market research purposes. We may contact you by email, phone, fax or mail. 
We may use the information to customise the website according to your interests. We are committed to ensuring that your information is secure. In order to prevent unauthorised access or disclosure we have put in suitable measures. 
Controlling your personal information You may choose to restrict the collection or use of your personal information in the following ways: 
whenever you are asked to fill in a form on the website, look for the box that you can click to indicate that you do not want the information to be used by anybody for direct marketing purposes 
if you have previously agreed to us using your personal information for direct marketing purposes, you may change your mind at any time by writing to or emailing us at mdnairigdtuw@gmail.com/jaissawhney123@gmail.com 
We will not sell, distribute or lease your personal information to third parties unless we have your permission or are required by law to do so. 
We may use your personal information to send you promotional information about third parties which we think you may find interesting if you tell us that you wish this to happen. 
If you believe that any information we are holding on you is incorrect or incomplete, 
please write to Indira Gandhi Delhi Technical University, Kashmere Gate, New Delhi North Delhi DELHI 110006 . 
or contact us at 9878880454/8588840228 or write to us at mdnairigdtuw@gmail.com/jaissawhney123@gmail.com as soon as possible. We will promptly correct any information found to be incorrect.
""".trimIndent()

val cancellationContent = """
Devika and Jaismeen believe in helping its customers as far as possible, and has therefore a liberal cancellation policy. 
Under this policy: 
Cancellations will be considered only if the request is made within 1-2 days of placing the order. 
However, the cancellation request may not be entertained if the orders have been communicated to the vendors/merchants and they have initiated the process of shipping them. 
Devika & Jaismeen accept refund/replacement requests if the customer establishes that the quality of product delivered is not good with pictures/videos. 
In case of receipt of damaged or defective items please report the same to our Customer Service team. 
The request will, however, be entertained once the merchant has checked and determined the same at his own end. 
This should be reported within 1-2 days of receipt of the products. 
In case of complaints regarding products that come with a warranty from manufacturers, please refer the issue to them. 
In case of any Refunds approved by the Jaismeen and Devika, it’ll take 3-5 days for the refund to be processed to the end customer.
""".trimIndent()

val shippingContent = """
For International buyers, orders are shipped and delivered through registered international courier companies and/or International speed post only. 
For domestic buyers, orders are shipped through registered domestic courier companies and /or speed post only. 
Orders are shipped within 3-5 days or as per the delivery date agreed at the time of order confirmation and delivering of the shipment subject to Courier Company / post office norms. 
Jaismeen and Devika are not liable for any delay in delivery by the courier company / postal authorities and only guarantees to hand over the consignment to the courier company or postal authorities within 3-5 days rom the date of the order and payment or as per the delivery date agreed at the time of order confirmation. 
Delivery of all orders will be to the address provided by the buyer. Delivery of our services will be confirmed on your mail ID as specified during registration. 
For any issues in utilizing our services you may contact us on 9878880454/8588840228 or write to us jaissawhney123@gmail.com/mdnairigdtuw@gmail.com
""".trimIndent()

val contactContent = """
📧 mdnairigdtuw@gmail.com
📧 jaissawhney123@gmail.com 
📞 +91-9876543210  
📞 +91-8588840228
🕒 Support Hours: 9 AM – 5 PM IST
""".trimIndent()

val aboutUsContent = """
Welcome to GreenBasket, a revolutionary platform that connects local organic producers directly with urban consumers.
In today’s fast-changing world, where health, sustainability, and digital inclusion are more important than ever, GreenBasket was created to bridge the widening gap between rural farmers and city dwellers. While urban consumers increasingly seek fresh, authentic, and organic food, small and marginal farmers struggle with access to fair markets and face exploitation by middlemen.
GreenBasket aims to change that.
We’re a mobile-first, cloud-backed app built using modern Android technologies that empowers verified local producers—farmers, home-based food makers, and artisans—to showcase and sell their products directly to consumers. Unlike traditional grocery delivery apps, GreenBasket decentralizes the supply chain. Here, producers retain control over pricing, availability, delivery, and whether to approve each order in real time.

💡 What makes us unique?
- Direct farm-to-consumer model
- Real-time order approval by producers
- Multiple secure payment options (UPI, card, bank transfer)
- AES-encrypted financial data for maximum security
- Farm videos and certification uploads to build trust
- Role-specific features for both producers and consumers
- Razorpay integration for seamless digital payments

We built GreenBasket not just as a shopping app—but as a movement. A movement to promote transparency in food sourcing, empower rural producers, and support healthier, more sustainable urban living.
Backed by Firebase for real-time data and authentication, Jetpack Compose for a modern user interface, and a scalable MVVM architecture, GreenBasket ensures a seamless and trustworthy experience for all users.
Whether you're a farmer looking to sell directly to your community or a consumer looking for genuine organic produce, GreenBasket is your digital marketplace for sustainable, secure, and direct trade.
Let’s build a fairer food future—together.
""".trimIndent()


@RequiresApi(Build.VERSION_CODES.O)
@Preview(showSystemUi = true, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    GreenBasketTheme(isProducer = false) {
        // Pass a mock NavController for the preview if CartWishlistActions requires one,
        // or ensure CartWishlistActions also handles a nullable NavController.
        // For simplicity and avoiding crashes in preview if CartWishlistActions is not robust,
        // we'll pass a rememberNavController() here, or null if CartWishlistActions handles it.
        SettingsScreen(navController = rememberNavController()) // Pass a real navController for the preview to work properly
    }
}