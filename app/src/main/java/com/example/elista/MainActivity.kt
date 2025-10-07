package com.example.elista

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

  private lateinit var recyclerView: RecyclerView
  private lateinit var adapter: ReceiptAdapter

  // UI elements for structured metadata
  private lateinit var tvVendor: TextView
  private lateinit var tvDate: TextView
  private lateinit var tvTotal: TextView

  // Request code for image picker
  private val PICK_IMAGE_REQUEST = 100

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Initialize RecyclerView and Adapter
    recyclerView = findViewById(R.id.recyclerView)
    adapter = ReceiptAdapter(emptyList())
    recyclerView.layoutManager = LinearLayoutManager(this)
    recyclerView.adapter = adapter

    // Initialize TextViews
    tvVendor = findViewById(R.id.tvVendor)
    tvDate = findViewById(R.id.tvDate)
    tvTotal = findViewById(R.id.tvTotal)

    // Default values
    tvVendor.text = "Vendor: N/A"
    tvDate.text = "Date: N/A"
    tvTotal.text = "Total: ₱0.00"

    // Upload button logic
    findViewById<Button>(R.id.btnUpload).setOnClickListener {
      val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
      startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
      val imageUri = data?.data ?: return

      val file = writeToFile(this, imageUri)
      if (file == null) {
        Toast.makeText(this, "Failed to prepare image file.", Toast.LENGTH_SHORT).show()
        return
      }

      // Show quick feedback
      Toast.makeText(this, "Uploading receipt...", Toast.LENGTH_SHORT).show()

      // Call ApiClient to analyze the receipt
      ApiClient.uploadReceiptFile(file) { analysis ->
        runOnUiThread {
          if (analysis != null) {
            updateUI(analysis)
            Toast.makeText(this, "Receipt scanned successfully!", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(this, "Failed to analyze receipt.", Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  private fun updateUI(receipt: ReceiptAnalysis) {
    tvVendor.text = "Vendor: ${receipt.vendor}"
    tvDate.text = "Date: ${receipt.date}"

    // receipt.total is a string; try to format as numeric, fallback to raw
    tvTotal.text = try {
      "Total: ₱${String.format("%.2f", receipt.total.toDouble())}"
    } catch (e: Exception) {
      "Total: ₱${receipt.total}"
    }

    adapter.updateItems(receipt.items)
  }

  /**
   * Converts a URI to a temporary file for upload.
   */
  private fun writeToFile(context: Context, uri: Uri): File? {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    if (inputStream == null) return null

    val tempFile = File(context.cacheDir, "receipt_upload.jpg")

    try {
      inputStream.use { input ->
        FileOutputStream(tempFile).use { output ->
          input.copyTo(output)
        }
      }
      return tempFile
    } catch (e: Exception) {
      e.printStackTrace()
      return null
    }
  }
}
