package com.example.elista

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException

// Ensure there is only ONE definition of the ApiClient object
object ApiClient {

    private val client = OkHttpClient()
    private const val BACKEND_URL = "https://elista-406642774905.asia-southeast1.run.app/analyze"
    fun uploadReceiptFile(file: File, callback: (ReceiptAnalysis?) -> Unit) {
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
                e.printStackTrace()
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        // Handle server errors (e.g., 500)
                        callback(null)
                        return
                    }

                    val json = response.body?.string() ?: return

                    // Parse the JSON response
                    val jobj = JSONObject(json)

                    // --- PARSING INCLUDING VENDOR, DATE, AND TOTAL ---
                    val vendor = jobj.optString("vendor", "N/A")
                    val date = jobj.optString("date", "N/A")
                    val total = jobj.optDouble("total", 0.0)

                    val itemsArray = jobj.optJSONArray("items")
                    val items = mutableListOf<ReceiptItem>()

                    if (itemsArray != null) {
                        for (i in 0 until itemsArray.length()) {
                            val itemObj = itemsArray.getJSONObject(i)
                            val name = itemObj.optString("name", "Unknown Item")
                            val price = itemObj.optDouble("price", 0.0)

                            items.add(ReceiptItem(name, price))
                        }
                    }
                    val receiptAnalysis = ReceiptAnalysis(
                        items = items,
                        total = total,
                        vendor = vendor,
                        date = date,
                        fullText = jobj.optString("text", "")
                    )

                    callback(receiptAnalysis)

                } catch (e: Exception) {
                    e.printStackTrace()
                    callback(null)
                }
            }
        })
    }
}