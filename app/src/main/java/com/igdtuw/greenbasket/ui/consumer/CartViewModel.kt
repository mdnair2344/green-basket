//CartViewModel
package com.igdtuw.greenbasket.ui.consumer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class Coupon(
    val code: String,
    val discountPercentage: Double
)


@HiltViewModel
class CartViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun getUserId(): String? = auth.currentUser?.uid

    fun getUserData(callback: (Map<String, Any>?) -> Unit) {
        val userId = getUserId() ?: return callback(null)
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document -> callback(document.data) }
            .addOnFailureListener { callback(null) }
    }

    private val _cartItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val cartItems: StateFlow<List<OrderItem>> = _cartItems.asStateFlow()

    private val _cartMap = MutableStateFlow<Map<String, OrderItem>>(emptyMap())
    val cartMap: StateFlow<Map<String, OrderItem>> = _cartMap.asStateFlow()

    private val _subtotal = MutableStateFlow(0.0)
    val subtotal: StateFlow<Double> = _subtotal.asStateFlow()

    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _finalPayableAmount = MutableStateFlow(0.0)
    val finalPayableAmount: StateFlow<Double> = _finalPayableAmount.asStateFlow()

    init {
        loadCartItemsFromFirestore()

        viewModelScope.launch {
            cartItems.collect { recalculateTotals() }
        }

        viewModelScope.launch {
            appliedCoupon.collect { recalculateTotals() }
        }
    }

    fun isInCart(productId: String): Boolean {
        return _cartMap.value.containsKey(productId)
    }

    fun getCartQuantity(productId: String): Int {
        return _cartMap.value[productId]?.quantity ?: 0
    }

    fun loadCartItemsFromFirestore() {
        val userId = auth.currentUser?.uid ?: run {
            println("User not logged in. Cannot load cart.")
            _cartItems.value = emptyList()
            return
        }

        db.collection("consumers")
            .document(userId)
            .collection("cart")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    println("Listen failed: $e")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val items = mutableListOf<OrderItem>()
                    for (doc in snapshots.documents) {
                        val productMap = doc.get("product") as? Map<*, *>
                        val productId = productMap?.get("id") as? String ?: doc.id
                        val name = productMap?.get("name") as? String ?: ""
                        val price = (productMap?.get("price") as? Number)?.toDouble() ?: 0.0
                        val quantity = (doc.get("quantity") as? Number)?.toInt() ?: 0
                        val imageUrl = productMap?.get("imageUrl") as? String

                        items.add(
                            OrderItem(
                                productId = productId,
                                productName = name,
                                unitPrice = price,
                                quantity = quantity,
                                imageUrl = imageUrl
                            )
                        )
                    }
                    _cartItems.value = items
                    println("Cart items loaded: ${items.size}")
                } else {
                    println("Current data: null")
                    _cartItems.value = emptyList()
                }
            }
    }

    private fun recalculateTotals() {
        val currentSubtotal = _cartItems.value.sumOf { it.quantity * it.unitPrice }
        _subtotal.value = currentSubtotal

        val currentDiscount = _appliedCoupon.value?.let { coupon ->
            currentSubtotal * coupon.discountPercentage
        } ?: 0.0
        _discountAmount.value = currentDiscount

        val currentFinal = currentSubtotal - currentDiscount
        _finalPayableAmount.value = currentFinal
    }

    fun applyCoupon(code: String): Boolean {
        val validCoupons = mapOf(
            "SAVE10" to Coupon("SAVE10", 0.10),
            "FLAT20" to Coupon("FLAT20", 0.20),
            "FREE50" to Coupon("FREE50", 0.50)
        )

        val coupon = validCoupons[code.uppercase()]
        return if (coupon != null) {
            _appliedCoupon.value = coupon
            recalculateTotals()
            true
        } else {
            _appliedCoupon.value = null
            recalculateTotals()
            false
        }
    }

    fun clearCoupon() {
        _appliedCoupon.value = null
        recalculateTotals()
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _appliedCoupon.value = null
        recalculateTotals()
    }
}
