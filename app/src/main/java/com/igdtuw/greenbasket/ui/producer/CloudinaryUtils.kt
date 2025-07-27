package com.igdtuw.greenbasket.ui.producer // Adjust package name as per your project

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response // <--- THIS IS THE MISSING IMPORT!
import okio.source
import org.json.JSONObject
import java.io.IOException

private const val CLOUDINARY_CLOUD_NAME = "dgbtfbkk3"
private const val CLOUDINARY_UPLOAD_PRESET = "GreenBasket" // The name of your unsigned upload preset

// Common utility function for Cloudinary upload

fun uploadToCloudinary(
    context: Context,
    fileUri: Uri,
    folder: String, // Dynamic folder for specific crop or producer certificates
    onSuccess: (String, String) -> Unit, // Returns imageUrl and publicId
    onFailure: (String) -> Unit
) {
    val cloudinaryUploadUrl = "https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/auto/upload"

    val inputStream = context.contentResolver.openInputStream(fileUri)
        ?: return onFailure("Failed to read file")
    val mediaType = context.contentResolver.getType(fileUri)?.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()

    val requestBody = object : RequestBody() {
        override fun contentType() = mediaType
        override fun writeTo(sink: okio.BufferedSink) {
            inputStream.source().use { sink.writeAll(it) }
        }
    }

    val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("file", "file", requestBody)
        .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
        .addFormDataPart("folder", folder) // Use the provided dynamic folder
        .build()

    val request = Request.Builder()
        .url(cloudinaryUploadUrl)
        .post(multipart)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post {
                onFailure(e.message ?: "Upload error")
            }
        }

        override fun onResponse(call: Call, response: Response) { // 'Response' now correctly imported
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val url = json.getString("secure_url")
                val publicId = json.getString("public_id") // Extract public_id
                Handler(Looper.getMainLooper()).post {
                    onSuccess(url, publicId) // Pass both url and publicId
                }
            } else {
                Handler(Looper.getMainLooper()).post {
                    val errorBody = response.body?.string()

                    onFailure("Upload failed: ${response.code} - ${response.message} - ${errorBody}")
                }
            }
        }
    })
}

// TODO: IMPORTANT! Replace this client-side simulation with a secure backend call for production.
fun simulateCloudinaryDeletion(imageUrl: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
    println("SIMULATING Cloudinary deletion for URL: $imageUrl")
    Handler(Looper.getMainLooper()).postDelayed({
// Simulate success or failure
        if (Math.random() > 0.1) { // 90% chance of success
            onSuccess()
        } else {
            onFailure("Simulated network error during deletion")
        }
    }, 500) // Simulate a network delay
}

// Helper function to extract public_id from Cloudinary URL (if not storing it directly)
fun extractPublicIdFromCloudinaryUrl(url: String): String? {
// Example: https://res.cloudinary.com/cloud_name/image/upload/v12345/folder/subfolder/filename.ext
// We want: folder/subfolder/filenamev
    val parts = url.split("/upload/")
    if (parts.size < 2) return null
    val pathWithVersion = parts[1] // v12345/folder/subfolder/filename.ext
// Remove the version number (if present, usually after '/upload/') and then the extension
    val publicIdWithPathAndVersion = pathWithVersion.substringAfter("/") // folder/subfolder/filename.ext
    val finalPublicId = publicIdWithPathAndVersion.substringBeforeLast(".") // Remove extension
    return finalPublicId
}