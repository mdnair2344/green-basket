//RevenueViewModel
package com.igdtuw.greenbasket.ui.producer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.igdtuw.greenbasket.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RevenueViewModel : ViewModel() {

    private val _revenueData = mutableStateListOf<CropRevenue>()
    val revenueData: List<CropRevenue> get() = _revenueData

    fun loadRevenue(producerId: String) {
        viewModelScope.launch {
            val data = fetchRevenueData(producerId)
            _revenueData.clear()
            _revenueData.addAll(data)
        }
    }

    suspend fun fetchRevenueData(producerId: String): List<CropRevenue> {
        val db = Firebase.firestore
        val cropDocs = db.collection("producers").document(producerId)
            .collection("crops").get().await()

        val cropMetaMap = cropDocs.documents
            .mapNotNull { doc ->
                val name = doc.getString("name")?.trim() ?: return@mapNotNull null
                name to mapOf(
                    "emoji" to getEmojiForCrop(name),
                    "pricePerKg" to (doc.getDouble("pricePerKg") ?: 0.0)
                )
            }
            .associate { it }

        val cropRevenueMap = mutableMapOf<String, CropRevenue>()
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))

        val ordersSnapshot = db.collection("orders")
            .whereEqualTo("producerId", producerId)
            .whereIn("status", listOf("payment_successful", "delivered"))
            .get().await()

        for (doc in ordersSnapshot.documents) {
            val orderMonth = try {
                val orderDate = doc.get("orderDate")
                val localDate = when (orderDate) {
                    is Long -> java.time.Instant.ofEpochMilli(orderDate)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

                    is Double -> java.time.Instant.ofEpochMilli(orderDate.toLong())
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

                    is String -> {
                        try {
                            LocalDate.parse(orderDate, DateTimeFormatter.ISO_DATE)
                        } catch (e: Exception) {
                            continue
                        }
                    }

                    is com.google.firebase.Timestamp -> orderDate.toDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()

                    else -> continue
                }

                localDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            } catch (e: Exception) {
                continue
            }

            val items = when (val rawItems = doc.get("items")) {
                is List<*> -> rawItems.filterIsInstance<Map<String, Any>>()
                else -> listOf(
                    mapOf(
                        "productName" to (doc.getString("productName") ?: continue),
                        "quantity" to (doc.get("quantity") ?: continue),
                        "unitPrice" to (doc.get("unitPrice") ?: continue)
                    )
                )
            }

            for (item in items) {
                val cropName = (item["productName"] as? String)?.trim() ?: continue
                val quantity = when (val q = item["quantity"]) {
                    is Long -> q.toInt()
                    is Double -> q.toInt()
                    is String -> q.toIntOrNull() ?: continue
                    else -> continue
                }
                val unitPrice = when (val p = item["unitPrice"]) {
                    is Long -> p.toDouble()
                    is Double -> p
                    is String -> p.toDoubleOrNull() ?: continue
                    else -> continue
                }

                val revenue = quantity * unitPrice
                val cropMeta = cropMetaMap[cropName] ?: mapOf("emoji" to "🌱", "pricePerKg" to unitPrice)

                val current = cropRevenueMap[cropName]
                val updatedHistory = current?.revenueHistory?.toMutableMap() ?: mutableMapOf()
                updatedHistory[orderMonth] = (updatedHistory[orderMonth] ?: 0.0) + revenue

                val newRevenueThisMonth = if (orderMonth == currentMonth) {
                    (current?.revenueThisMonth ?: 0.0) + revenue
                } else {
                    current?.revenueThisMonth ?: 0.0
                }

                cropRevenueMap[cropName] = CropRevenue(
                    name = cropName,
                    emoji = cropMeta["emoji"] as String,
                    quantity = (current?.quantity ?: 0) + quantity,
                    pricePerKg = cropMeta["pricePerKg"] as Double,
                    revenueThisMonth = newRevenueThisMonth,
                    revenueHistory = updatedHistory
                )
            }
        }

        return cropRevenueMap.values.toList()
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun generateRevenueReportPdf(context: Context, selectedMonth: String) {
        viewModelScope.launch {
            try {
                val cropRevenueMap = mutableMapOf<String, MutableList<Float>>() // crop -> [qty, pricePerKg]

                for (record in revenueData) {
                    val monthlyRevenue = record.revenueHistory[selectedMonth] ?: continue
                    val pricePerKg = record.pricePerKg.toFloat()
                    val monthlyQuantity = (monthlyRevenue / pricePerKg).toFloat()

                    cropRevenueMap[record.name] = mutableListOf(monthlyQuantity, pricePerKg)
                }

                if (cropRevenueMap.isEmpty()) {
                    Toast.makeText(context, "No data found for $selectedMonth", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val pdfDocument = PdfDocument()
                val paint = Paint()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas


                val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
                val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 80, 80, true) // Resize if needed

                val logoX = pageInfo.pageWidth - scaledLogo.width - 40f
                val logoY = 40f
                canvas.drawBitmap(scaledLogo, logoX, logoY, null)

                // Title
                paint.textSize = 20f
                paint.isFakeBoldText = true
                paint.color = Color.BLACK
                val title = "${selectedMonth.replaceFirstChar { it.uppercase() }}'s Revenue Report"
                canvas.drawText(title, 160f, 50f, paint)

                // Table Setup
                val startX = 15f
                var startY = logoY + scaledLogo.height + 20f // Start text 20px below logo

                val rowHeight = 30f
                val columnSpacing = listOf(0f, 80f, 240f, 340f, 450f) // S.No, Crop, Qty, Price, Revenue
                val headers = listOf("S.No", "Crop", "Qty", "Price/kg", "Revenue")

                // Header Row Background - Dark Green
                paint.color = Color.rgb(0, 100, 0)
                canvas.drawRect(startX, startY, startX + 580f, startY + rowHeight, paint)

                // Header Text - White
                paint.color = Color.WHITE
                paint.textSize = 14f
                paint.isFakeBoldText = true
                headers.forEachIndexed { i, header ->
                    canvas.drawText(header, startX + columnSpacing[i] + 5f, startY + 20f, paint)
                }

                startY += rowHeight
                paint.isFakeBoldText = false

                var totalRevenue = 0f

                // Data Rows
                cropRevenueMap.entries.forEachIndexed { index, entry ->
                    val cropName = entry.key
                    val qty = entry.value[0].toInt()
                    val price = entry.value[1].toInt()
                    val revenue = qty * price
                    totalRevenue += revenue

                    // Light Green Row Background
                    paint.color = Color.rgb(200, 255, 200)
                    canvas.drawRect(startX, startY, startX + 580f, startY + rowHeight, paint)

                    // Row Text - Black
                    paint.color = Color.BLACK
                    canvas.drawText("${index + 1}", startX + columnSpacing[0] + 5f, startY + 20f, paint)
                    canvas.drawText(cropName, startX + columnSpacing[1] + 5f, startY + 20f, paint)
                    canvas.drawText("$qty", startX + columnSpacing[2] + 5f, startY + 20f, paint)
                    canvas.drawText("₹$price", startX + columnSpacing[3] + 5f, startY + 20f, paint)
                    canvas.drawText("₹$revenue", startX + columnSpacing[4] + 5f, startY + 20f, paint)

                    startY += rowHeight
                }

                // Total Row - Dark Gray Background
                paint.color = Color.DKGRAY
                canvas.drawRect(startX, startY, startX + 580f, startY + rowHeight, paint)
                paint.color = Color.WHITE
                paint.isFakeBoldText = true
                canvas.drawText("Total Revenue:", startX + columnSpacing[2] + 5f, startY + 20f, paint)
                canvas.drawText("₹${totalRevenue.toInt()}", startX + columnSpacing[4] + 5f, startY + 20f, paint)

                pdfDocument.finishPage(page)

                // Save PDF
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "${selectedMonth}_RevenueReport.pdf")
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/GreenBasketReports")
                }

                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
                )

                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { output ->
                        pdfDocument.writeTo(output)
                    }
                    Toast.makeText(context, "PDF saved to Downloads/GreenBasketReports", Toast.LENGTH_LONG).show()
                } ?: run {
                    Toast.makeText(context, "Failed to create file", Toast.LENGTH_SHORT).show()
                }

                pdfDocument.close()
            } catch (e: Exception) {
                Log.e("PDFGeneration", "Error: ${e.message}", e)
                Toast.makeText(context, "Error generating PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }


}

fun <K, V> Map<K, V?>.filterNotNullValues(): Map<K, V> {
    return this.mapNotNull { if (it.value != null) it.key to it.value!! else null }.toMap()
}

fun getEmojiForCrop(name: String): String {
    return when (name.lowercase()) {
        "wheat" -> "🌾"
        "tomato" -> "🍅"
        "potato" -> "🥔"
        "pineapple" -> "🍍"
        "mango", "mangoes" -> "🥭"
        "banana" -> "🍌"
        "carrot" -> "🥕"
        "apple" -> "🍎"
        else -> "🌱"
    }
}
