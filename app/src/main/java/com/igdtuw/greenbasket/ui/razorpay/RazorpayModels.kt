package com.igdtuw.greenbasket.ui.razorpay

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// data/model/RazorpayModels.kt (or similar)


@Parcelize
data class UserAddress(
    val fullName: String = "",
    val phone: String = "",
    val address: String = ""
)
    : Parcelable {
    val fullAddress: String
        get() = "$address"
}


// Data class for each item being transferred to a producer
// This should match what your backend expects
data class OrderItemForTransfer(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double, // Assuming this is needed for backend calculation
    val producerId: String // Crucial for identifying the producer for transfer
)

// Request body your Android app sends to your backend to create a Razorpay Order
data class CreateRazorpayOrderRequest(
    val totalAmount: Double,
    val deliveryAddress: UserAddress, // Pass the full UserAddress object
    val items: List<OrderItemForTransfer>, // List of items for transfer calculation
    val paymentMethod: String // "UPI" or "Card"
    // Add any other necessary details like userId, timestamp, etc.
)

// Response body your backend sends back to Android after creating Razorpay Order
data class OrderResponseFromBackend(
    val razorpayOrderId: String, // The order_id received from Razorpay
    val internalOrderId: String, // Your own internal order ID for this transaction
    val message: String = "Razorpay order created successfully"
)

// Generic Resource class for API states (Loading, Success, Error, Idle)
sealed class Resource<out T> {
    object Idle : Resource<Nothing>()
    object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : Resource<Nothing>()
}