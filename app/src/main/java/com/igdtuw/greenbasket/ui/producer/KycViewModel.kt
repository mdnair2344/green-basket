package com.igdtuw.greenbasket.ui.producer

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

class KycViewModel : ViewModel() {

    companion object {
        private const val BASE_URL_CONSTANT = "http://192.168.1.107:5000/"
    }

    private val _kycCompleted = MutableStateFlow(false)
    val kycCompleted = _kycCompleted.asStateFlow()

    fun submitKycToBackend(
        producerId: String,
        merchantId: String,
        accountNumber: String,
        ifsc: String,
        beneficiaryName: String,
        context: Context
    ) {
        viewModelScope.launch {
            val backendUrl = "${BASE_URL_CONSTANT}complete-kyc"

            val jsonBody = JSONObject().apply {
                put("producerId", producerId)
                put("merchantId", merchantId)
                put("accountNumber", accountNumber)
                put("ifscCode", ifsc)
                put("beneficiaryName", beneficiaryName)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = RequestBody.create(mediaType, jsonBody.toString())

            val request = Request.Builder()
                .url(backendUrl)
                .post(requestBody)
                .build()

            val client = OkHttpClient()

            try {
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    _kycCompleted.value = true
                    Toast.makeText(context, "✅ KYC Submitted Successfully!", Toast.LENGTH_LONG).show()
                    Log.d("KYC", "KYC Submission Success")
                } else {
                    val errorBody = response.body?.string()
                    Log.e("KYC", "Backend error: $errorBody")
                    Toast.makeText(context, "❌ Failed to submit KYC", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("KYC", "Network error: ${e.message}")
                Toast.makeText(context, "❌ Network Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}