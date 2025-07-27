//CommonDataClasses

package com.igdtuw.greenbasket.ui.consumer


import java.time.LocalDate

import com.google.firebase.Timestamp


data class Review(
    var id: String = "",
    val consumerId: String = "",
    val producerId: String = "",
    val cropId: String = "",
    val comment: String = "",
    val rating: Int = 0,
    val timestamp: Timestamp? = null,
    val date: String = "",

    var cropName: String? = null,
    var farmName: String? = null,
    var producerName: String? = null
)


data class ProducerDetails(
    val farmName: String = "",
    val certificateURI: String = "",
    val imageUri: String = "",
    val name: String = "",
    val phone: String = "",
    val category: String = "",
    val uid: String = "",
    val address: String = "",
    val linkedAccountId: String = "",
    val userType: String = "",
    val email: String = ""
)





data class ReviewDisplay(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImageUrl: String = "",
    val producerName: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val date: String = "" // formatted yyyy-MM-dd
)




// Data class for UserProducerInfo (to fetch name and farmName from the 'users' collection)
//
data class UserProducerInfo(
    val name: String = "",
    val farmName: String = "",
    val userType: String = ""
)

// Data class for Crop (as used in ShopProductsScreen)

data class CropWithProducerInfo(
    val crop: Crop,
    val producerName: String,
    val farmName: String
)

data class Crop(
    var id: String = "",
    val name: String = "",
    val category: String = "",
    val variety: String = "",
    val type: String = "",
    val quantity: Int = 0,
    val pricePerKg: Double = 0.0,
    val description: String = "",
    val imageUri: String = "",
    val cloudinaryPublicId: String = "",
    val producer: String = ""
)

// Data class for Certificate (as used in ProducerCertificatesScreen)
data class Certificate(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val issueDate: String = "",
    val expiryDate: String = "",
    val authority: String = "",
    val status: String = "",
    val certificateUrl: String = "",
    val cloudinaryPublicId: String = ""
)

// Combined data class for display in ShopProductsScreen
data class ShopProductDisplay(
    val crop: Crop,
    val producerName: String,
    val farmName: String
)

// Data class to combine Certificate with Producer info for display
data class CertificateWithProducer(
    val certificate: Certificate,
    val producerName: String,
    val farmName: String
)