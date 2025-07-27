//TrackOrdersViewModel
package com.igdtuw.greenbasket.ui.producer

import android.os.Environment
import android.provider.MediaStore
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.igdtuw.greenbasket.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class DisplayOrder(
    val orderId: String,
    val consumerId: String,
    val consumerName: String,
    val consumerPhone: String = "",
    val consumerAddress: String = "",
    val mobileNumber: String = "",
    val deliveryDateTime: String = "",
    val status: String = "",
    val items: Map<String, Int>? = emptyMap(),
    val totalAmount: Any = 0.0,
    val orderDate: String = ""
)

@HiltViewModel
class TrackOrdersViewModel @Inject constructor() : ViewModel() {

    private val _pendingOrders = MutableStateFlow<List<DisplayOrder>>(emptyList())
    val pendingOrders: StateFlow<List<DisplayOrder>> = _pendingOrders

    private val _completedOrders = MutableStateFlow<List<DisplayOrder>>(emptyList())
    val completedOrders: StateFlow<List<DisplayOrder>> = _completedOrders

    private val db = Firebase.firestore
    private val currentProducerId = Firebase.auth.currentUser?.uid ?: ""

    fun fetchOrdersForProducer() {
        viewModelScope.launch {
            val pending = mutableListOf<DisplayOrder>()
            val completed = mutableListOf<DisplayOrder>()

            try {
                val ordersSnapshot = db.collection("orders").get().await()

                for (orderDoc in ordersSnapshot.documents) {
                    val producerId = orderDoc.getString("producerId") ?: continue
                    val status = orderDoc.getString("status") ?: continue
                    val userId = orderDoc.getString("userId") ?: continue
                    val delivery = orderDoc.getString("deliveryDateTime") ?: "TBD"
                    val totalAmount = orderDoc["totalAmount"]
                    val orderDate = orderDoc["orderDate"] as? String ?: "N/A"

                    if (producerId != currentProducerId) continue

                    val items = (orderDoc["items"] as? List<Map<String, Any>>)?.map {
                        val itemName = it["productName"] as? String ?: ""
                        val quantity = (it["quantity"] as? Long)?.toInt() ?: 0
                        itemName to quantity
                    }?.toMap()


                    val userSnapshot = db.collection("users").document(userId).get().await()
                    val name = userSnapshot.getString("name") ?: "Unknown"
                    val mobile = userSnapshot.getString("mobileNumber") ?: "N/A"
                    val phone = userSnapshot.getString("phone") ?: "N/A"
                    val address = userSnapshot.getString("address") ?: "N/A"

                    val displayOrder = DisplayOrder(
                        orderId = orderDoc.id,
                        consumerId = userId,
                        consumerName = name,
                        mobileNumber = mobile,
                        consumerPhone = phone,
                        consumerAddress = address,
                        items = items,
                        deliveryDateTime = delivery,
                        status = status,
                        totalAmount = totalAmount ?: 0.0,
                        orderDate = orderDate
                    )

                    when (status) {
                        "payment_successful" -> pending.add(displayOrder)
                        "delivered" -> completed.add(displayOrder)
                        // ignore others
                    }
                }

                _pendingOrders.value = pending
                _completedOrders.value = completed

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun markOrderAsDelivered(orderId: String) {
        viewModelScope.launch {
            try {
                val orderDoc = db.collection("orders").document(orderId).get().await()
                if (!orderDoc.exists()) return@launch

                val status = orderDoc.getString("status") ?: return@launch
                if (status == "delivered") return@launch // Already marked

                val producerId = orderDoc.getString("producerId") ?: return@launch

                // ✅ Correct casting: items is a List<Map<String, Any>>
                val items = orderDoc.get("items") as? List<Map<String, Any>> ?: return@launch

                for (item in items) {
                    val cropId = item["productId"]?.toString() ?: continue
                    val orderedQty = (item["quantity"] as? Number)?.toLong() ?: continue

                    val cropRef = db.collection("producers")
                        .document(producerId)
                        .collection("crops")
                        .document(cropId)

                    // Atomic transaction for safety
                    db.runTransaction { transaction ->
                        val cropSnapshot = transaction.get(cropRef)
                        val currentQty = cropSnapshot.getLong("quantity") ?: 0L
                        val newQty = (currentQty - orderedQty).coerceAtLeast(0L)
                        transaction.update(cropRef, "quantity", newQty)
                    }.await()
                }

                // ✅ Update status to 'delivered'
                db.collection("orders")
                    .document(orderId)
                    .update("status", "delivered")
                    .await()

                fetchOrdersForProducer()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }





    @RequiresApi(Build.VERSION_CODES.Q)
    fun generateCompletedOrdersPDF(
        context: Context,
        completedOrders: List<DisplayOrder>,
        pendingOrders: List<DisplayOrder>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()


        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
        val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 80, 80, true) // Resize if needed

        val logoX = pageInfo.pageWidth - scaledLogo.width - 40f
        val logoY = 40f
        canvas.drawBitmap(scaledLogo, logoX, logoY, null)

        // Colors
        val ConsumerCardBackground1 = Color.parseColor("#E0F2F1")
        val ConsumerPrimaryVariant = Color.parseColor("#00796B")
        val TextBlack = Color.BLACK

        var y = logoY + scaledLogo.height + 20f // Start text 20px below logo

        paint.textSize = 20f
        paint.isFakeBoldText = true
        paint.color = TextBlack
        canvas.drawText("Order Summary Report", 30f, y, paint)

        y += 40f
        paint.textSize = 14f
        paint.isFakeBoldText = false

        val sections = listOf(
            "Completed Orders" to completedOrders,
            "Pending Orders" to pendingOrders
        )

        var totalCompletedRevenue = 0.0
        var totalPendingRevenue = 0.0

        for ((sectionTitle, orders) in sections) {
            paint.textSize = 16f
            paint.isFakeBoldText = true
            paint.color = TextBlack
            canvas.drawText(sectionTitle, 30f, y, paint)
            y += 20f

            // Table setup
            paint.textSize = 12f
            paint.isFakeBoldText = true
            val headers = listOf("Name", "Phone", "Status", "Items", "Amount")
            val columnWidths = listOf(100f, 90f, 120f, 70f, 140f, 60f)  // total = ~580
            val rowHeight = 30f
            val startX = 30f

            // Header row
            var currentX = startX
            for ((i, header) in headers.withIndex()) {
                paint.color = ConsumerCardBackground1
                canvas.drawRect(currentX, y, currentX + columnWidths[i], y + rowHeight, paint)
                paint.color = TextBlack
                canvas.drawText(header, currentX + 5f, y + 20f, paint)
                currentX += columnWidths[i]
            }

            y += rowHeight
            paint.isFakeBoldText = false

            var sectionRevenue = 0.0

            for (order in orders) {
                if (y > 800f) {
                    pdfDocument.finishPage(page)
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }

                currentX = startX
                val amount = order.totalAmount.toString().toDoubleOrNull() ?: 0.0
                val statusText = if (order.status == "payment_successful" || order.status == "delivered") "Not Delivered" else order.status
                val itemDescription = order.items?.entries?.joinToString { "${it.key} x${it.value}" }

                val rowData = listOf(
                    order.consumerName,
                    order.consumerPhone,
                    statusText,
                    itemDescription,
                    "₹%.2f".format(amount)
                )

                for ((i, data) in rowData.withIndex()) {
                    paint.color = Color.LTGRAY
                    canvas.drawRect(currentX, y, currentX + columnWidths[i], y + rowHeight, paint)
                    paint.color = TextBlack
                    data?.let { canvas.drawText(it, currentX + 5f, y + 20f, paint) }
                    currentX += columnWidths[i]
                }

                sectionRevenue += amount
                y += rowHeight
            }

            // Section total
            y += 10f
            paint.isFakeBoldText = true
            paint.color = ConsumerPrimaryVariant
            canvas.drawText(
                "Total Revenue ($sectionTitle): ₹%.2f".format(sectionRevenue),
                30f, y, paint
            )
            y += 30f

            if (sectionTitle == "Completed Orders") totalCompletedRevenue = sectionRevenue
            if (sectionTitle == "Pending Orders") totalPendingRevenue = sectionRevenue
        }

        // Grand total
        val grandTotal = totalCompletedRevenue + totalPendingRevenue
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.color = ConsumerPrimaryVariant
        canvas.drawText("GRAND TOTAL REVENUE: ₹%.2f".format(grandTotal), 30f, y, paint)

        pdfDocument.finishPage(page)

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "order_summary.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        try {
            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    pdfDocument.writeTo(output)
                }
                Toast.makeText(context, "PDF saved to Downloads", Toast.LENGTH_LONG).show()
            } ?: throw Exception("File creation failed")
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }

        pdfDocument.close()

    }



}
