//ProducerOrdersViewModel

package com.igdtuw.greenbasket.ui.producer

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks // Import Tasks for Tasks.whenAllSuccess
import com.google.firebase.firestore.QuerySnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

// --- Data Classes ---
data class OrderItem(
    val name: String = "",
    val quantity: Int = 0,
    val pricePerUnit: Double = 0.0
)

data class Order(
    val orderId: String = "",
    val consumerId: String = "", // Added consumerId to the data class
    val producerId: String = "",
    val items: List<OrderItem> = emptyList(),
    val status: String = "",
    val totalAmount: Double = 0.0,
    var date: String = ""
)

// --- ViewModel ---
@HiltViewModel
class ProducerOrdersViewModel @Inject constructor() : ViewModel() {

    private val _ordersToApprove = MutableStateFlow<List<Order>>(emptyList())
    val ordersToApprove: StateFlow<List<Order>> = _ordersToApprove

    private val db = Firebase.firestore
    private val currentProducerId = Firebase.auth.currentUser?.uid ?: ""

    /**
     * Fetches the current state of pending orders for the logged-in producer.
     * This function performs a one-time fetch when called, gathering orders
     * from all consumers that match the producer's ID and "waiting_for_approval" status.
     * The StateFlow `_ordersToApprove` is updated once all data is collected.
     */
    fun listenToPendingOrdersRealtime() {
        val currentProducerId = Firebase.auth.currentUser?.uid ?: ""

        if (currentProducerId.isEmpty()) {
            _ordersToApprove.value = emptyList()
            println("Producer not logged in. Cannot fetch orders.")
            return
        }

        Firebase.firestore.collection("orders")
            .whereEqualTo("producerId", currentProducerId)
            .whereEqualTo("status", "waiting_for_approval")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Error listening to orders: $e")
                    _ordersToApprove.value = emptyList()
                    return@addSnapshotListener
                }

                val newOrders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val items = (doc["items"] as? List<Map<String, Any>>)?.map { itemMap ->
                            OrderItem(
                                name = itemMap["productName"] as? String ?: "",
                                quantity = (itemMap["quantity"] as? Long)?.toInt() ?: 0,
                                pricePerUnit = (itemMap["unitPrice"] as? Number)?.toDouble() ?: 0.0
                            )
                        } ?: emptyList()

                        Order(
                            orderId = doc.id,
                            consumerId = doc.getString("userId") ?: "",
                            producerId = doc.getString("producerId") ?: "",
                            items = items,
                            totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                            status = doc.getString("status") ?: ""
                        )
                    } catch (e: Exception) {
                        println("Failed to parse order: ${doc.id} due to $e")
                        null
                    }
                } ?: emptyList()

                _ordersToApprove.value = newOrders
                println("Fetched ${newOrders.size} global pending orders.")
            }
    }




    /**
     * Updates the status of a specific order in Firestore.
     * After a successful update, it re-fetches the pending orders to update the UI.
     *
     * @param consumerId The ID of the consumer who placed the order.
     * @param orderId The ID of the order to update.
     * @param newStatus The new status to set for the order (e.g., "Approved", "Rejected").
     * @param onComplete A callback function to execute upon successful update.
     */
    fun updateOrderStatus(
        orderId: String,
        newStatus: String,
        onComplete: () -> Unit
    ) {
        Firebase.firestore.collection("orders").document(orderId)
            .update("status", newStatus)
            .addOnSuccessListener {
                println("Order $orderId status updated to $newStatus")
                onComplete()
            }
            .addOnFailureListener { e ->
                println("Error updating order status for $orderId: $e")
            }
    }


}