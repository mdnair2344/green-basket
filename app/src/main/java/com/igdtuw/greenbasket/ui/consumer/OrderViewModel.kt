// OrderViewModel.kt
package com.igdtuw.greenbasket.ui.consumer

import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.Timestamp
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.Query
import com.igdtuw.greenbasket.R


class OrderViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private var ordersListener: ListenerRegistration? = null

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()


    private val _fetchedOrder = MutableStateFlow<Order?>(null)
    val fetchedOrder: StateFlow<Order?> = _fetchedOrder

    fun fetchOrderById(orderId: String) {
        Firebase.firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val order = document.toObject(Order::class.java)
                    _fetchedOrder.value = order
                } else {
                    Log.e("OrderFetch", "No such order document for $orderId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("OrderFetch", "Error fetching order: ", e)
            }
    }


    fun fetchOrdersForUser(userId: String?) {
        // Clean up any previous listener
        ordersListener?.remove()
        ordersListener = null

        if (userId.isNullOrBlank()) {
            _orders.value = emptyList()
            println("User ID is null or blank. Cannot fetch orders.")
            return
        }

        // Start listening to Firestore
        ordersListener = db.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("orderDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    println("Listen failed for orders for user $userId: ${e.message}")
                    _orders.value = emptyList()
                    return@addSnapshotListener
                }

                val fetchedOrders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val itemsList = (doc["items"] as? List<*>)?.mapNotNull { item ->
                            val map = item as? Map<*, *> ?: return@mapNotNull null
                            OrderItem(
                                productId = map["productId"] as? String ?: "",
                                productName = map["productName"] as? String ?: "",
                                imageUrl = map["imageUrl"] as? String,
                                quantity = (map["quantity"] as? Long)?.toInt() ?: 0,
                                unitPrice = (map["unitPrice"] as? Number)?.toDouble() ?: 0.0
                            )
                        } ?: emptyList()

                        Order(
                            orderId = doc.id,
                            userId = doc.getString("userId") ?: "",
                            userEmail = doc.getString("userEmail") ?: "",
                            orderDate = doc.getLong("orderDate") ?: 0L,
                            deliveryAddress = doc.getString("deliveryAddress") ?: "",
                            deliveryFullName = doc.getString("deliveryFullName") ?: "",
                            deliveryPhone = doc.getString("deliveryPhone") ?: "",
                            totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                            status = doc.getString("status") ?: "Pending",
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            producerId = doc.getString("producerId") ?: "",
                            items = itemsList
                        )
                    } catch (ex: Exception) {
                        println("Error parsing order ${doc.id}: ${ex.message}")
                        null
                    }
                }

                _orders.value = fetchedOrders ?: emptyList()
            }
    }


    fun clearOrders() {
        _orders.value = emptyList()
        ordersListener?.remove()
        ordersListener = null
    }

    override fun onCleared() {
        super.onCleared()
        ordersListener?.remove()
        ordersListener = null
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun generateBillPdf(context: Context, order: Order) {
    val db = FirebaseFirestore.getInstance()

    val buyerRef = order.userId.takeIf { it.isNotEmpty() }?.let { db.collection("users").document(it) }
    val sellerRef = order.producerId.takeIf { it.isNotEmpty() }?.let { db.collection("users").document(it) }

    if (buyerRef == null || sellerRef == null) {
        Toast.makeText(context, "Invalid user or producer ID.", Toast.LENGTH_SHORT).show()
        return
    }

    buyerRef.get().addOnSuccessListener { buyerDoc ->
        val buyerName = buyerDoc.getString("name") ?: ""
        val buyerPhone = buyerDoc.getString("phone") ?: ""
        val buyerAddress = buyerDoc.getString("address") ?: ""

        sellerRef.get().addOnSuccessListener { sellerDoc ->
            val sellerName = sellerDoc.getString("name") ?: ""
            val sellerPhone = sellerDoc.getString("phone") ?: ""
            val farmName = sellerDoc.getString("farmName") ?: ""

            val fileName = "OrderBill_${order.orderId}.pdf"
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas


            // Load logo from drawable
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 80, 80, true) // Resize if needed

            val logoX = pageInfo.pageWidth - scaledLogo.width - 40f
            val logoY = 40f
            canvas.drawBitmap(scaledLogo, logoX, logoY, null)


            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
            }

            val boldPaint = Paint().apply {
                isFakeBoldText = true
                color = Color.BLACK
                textSize = 16f
            }

            val headerPaint = Paint().apply {
                color = Color.WHITE
                textSize = 14f
                isFakeBoldText = true
            }

            val lightGreen = Color.parseColor("#D0F0C0")
            val darkGreen = Color.parseColor("#2E7D32")

            val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(order.orderDate))

            var y = logoY + scaledLogo.height + 20f // Start text 20px below logo
            canvas.drawText("GreenBasket - Order Invoice", 200f, y, boldPaint)
            y += 30f
            canvas.drawText("Order ID: ${order.orderId}", 40f, y, paint)
            y += 20f
            canvas.drawText("Date: $dateStr", 40f, y, paint)
            y += 30f

            // Buyer Info
            canvas.drawText("Buyer: $buyerName", 40f, y, paint)
            y += 20f
            canvas.drawText("Phone: $buyerPhone", 40f, y, paint)
            y += 20f
            canvas.drawText("Address: $buyerAddress", 40f, y, paint)
            y += 30f

            // Seller Info
            canvas.drawText("Seller: $sellerName", 40f, y, paint)
            y += 20f
            canvas.drawText("Phone: $sellerPhone", 40f, y, paint)
            y += 20f
            canvas.drawText("Farm: $farmName", 40f, y, paint)
            y += 30f

            // Table Header
            val startX = 40f
            val colWidths = listOf(200f, 80f, 100f, 100f)
            val rowHeight = 30f

            // Header Background
            canvas.drawRect(startX, y, startX + colWidths.sum(), y + rowHeight, Paint().apply { color = darkGreen })
            canvas.drawText("Product", startX + 10f, y + 20f, headerPaint)
            canvas.drawText("Qty", startX + colWidths[0] + 10f, y + 20f, headerPaint)
            canvas.drawText("Unit ₹", startX + colWidths[0] + colWidths[1] + 10f, y + 20f, headerPaint)
            canvas.drawText("Total ₹", startX + colWidths[0] + colWidths[1] + colWidths[2] + 10f, y + 20f, headerPaint)
            y += rowHeight

            // Item Rows
            order.items.forEach { item ->
                val rowPaint = Paint().apply { color = lightGreen }
                canvas.drawRect(startX, y, startX + colWidths.sum(), y + rowHeight, rowPaint)

                canvas.drawText(item.productName, startX + 10f, y + 20f, paint)
                canvas.drawText("${item.quantity}", startX + colWidths[0] + 10f, y + 20f, paint)
                canvas.drawText("₹${String.format("%.2f", item.unitPrice)}", startX + colWidths[0] + colWidths[1] + 10f, y + 20f, paint)
                canvas.drawText("₹${String.format("%.2f", item.unitPrice * item.quantity)}", startX + colWidths[0] + colWidths[1] + colWidths[2] + 10f, y + 20f, paint)
                y += rowHeight
            }

            // Total
            y += 10f
            val totalPaint = Paint().apply {
                color = lightGreen
            }
            canvas.drawRect(startX, y, startX + colWidths.sum(), y + rowHeight, totalPaint)
            canvas.drawText("Total Paid: ₹${String.format("%.2f", order.totalAmount)}", startX + 10f, y + 20f, boldPaint)
            y += rowHeight

            canvas.drawText("Payment Mode: ${order.paymentMethod}", startX + 10f, y + 20f, paint)
            y += 40f

            canvas.drawText("Thank you for shopping with GreenBasket!", 140f, y, paint)

            pdfDocument.finishPage(page)

            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    Toast.makeText(context, "Bill saved to Downloads/$fileName", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Failed to create PDF file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Toast.makeText(context, "Error saving bill: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                pdfDocument.close()
            }

        }.addOnFailureListener {
            Toast.makeText(context, "Failed to fetch producer details.", Toast.LENGTH_SHORT).show()
        }
    }.addOnFailureListener {
        Toast.makeText(context, "Failed to fetch buyer details.", Toast.LENGTH_SHORT).show()
    }
}


