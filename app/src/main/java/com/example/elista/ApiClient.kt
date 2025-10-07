package com.example.elista

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

object ApiClient {

    private val client = OkHttpClient()
    private const val BACKEND_URL = "https://elista-406642774905.asia-southeast1.run.app/analyze"

    fun uploadReceiptFile(file: File, callback: (ReceiptAnalysis?) -> Unit) {
        try {
            val fileSize = file.length()
            Log.d("ApiClient", "Uploading file: ${file.name}, size: $fileSize bytes")

            if (fileSize == 0L) {
                Log.e("ApiClient", "File is empty or not saved properly.")
                callback(null)
                return
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(BACKEND_URL)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("ApiClient", "❌ Upload failed: ${e.message}")
                    callback(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            Log.e("ApiClient", "❌ Server error: ${response.code}")
                            callback(null)
                            return
                        }

                        val json = response.body?.string() ?: run {
                            Log.e("ApiClient", "Empty response from backend")
                            callback(null)
                            return
                        }

                        Log.d("ApiClient", "Raw JSON response: $json")

                        val root = JSONObject(json)
                        val data = root.optJSONObject("extracted_data") ?: root

                        val vendor = data.optString("store_name", "Unknown Store")
                        val date = data.optString("date_of_purchase", "Unknown Date")
                        val totalStr = data.optString("total_amount", "0.00")

                        val itemsArray = data.optJSONArray("items")
                        val items = mutableListOf<ReceiptItem>()

                        if (itemsArray != null) {
                            for (i in 0 until itemsArray.length()) {
                                val itemObj = itemsArray.getJSONObject(i)
                                val name = itemObj.optString("name", "Unknown Item")
                                val price = itemObj.optString("price", "0.00")
                                items.add(ReceiptItem(name, "1", price))
                            }
                        }

                        val receiptAnalysis = ReceiptAnalysis(
                            vendor = vendor,
                            date = date,
                            total = totalStr,
                            items = items
                        )

                        callback(receiptAnalysis)

                    } catch (e: Exception) {
                        Log.e("ApiClient", "❌ JSON parse error: ${e.message}")
                        callback(null)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("ApiClient", "❌ Unexpected error: ${e.message}")
            callback(null)
        }
    }
}
