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
import com.example.elista.ApiClient

class MainActivity : AppCompatActivity() {

  private lateinit var recyclerView: RecyclerView
  private lateinit var adapter: ReceiptAdapter

  // UI elements for structured metadata (Vendor, Date, Total)
  private lateinit var tvVendor: TextView
  private lateinit var tvDate: TextView
  private lateinit var tvTotal: TextView

  // Constant for the image picker request
  private val PICK_IMAGE_REQUEST = 1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // 1. Initialize RecyclerView and Adapter
    recyclerView = findViewById(R.id.recyclerView)
    adapter = ReceiptAdapter(this, emptyList()) // Start with an empty list
    recyclerView.layoutManager = LinearLayoutManager(this)
    recyclerView.adapter = adapter

    // 2. Initialize Metadata TextViews
    // IMPORTANT: Ensure these IDs (tvVendor, tvDate, tvTotal) exist in your activity_main.xml
    tvVendor = findViewById(R.id.tvVendor)
    tvDate = findViewById(R.id.tvDate)
    tvTotal = findViewById(R.id.tvTotal)

    // Set initial state for metadata
    tvVendor.text = "Vendor: N/A"
    tvDate.text = "Date: N/A"
    tvTotal.text = "Total: ₱0.00"

    // 3. Set up Upload Button
    findViewById<Button>(R.id.btnUpload).setOnClickListener {
      val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
      startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
      val imageUri = data?.data ?: return

      // Step 1: Write the selected Uri content to a temporary local file
      val file = writeToFile(this, imageUri)
      if (file == null) {
        Toast.makeText(this, "Failed to prepare image file.", Toast.LENGTH_SHORT).show()
        return
      }

      // Step 2: Upload and Analyze the receipt
      ApiClient.uploadReceiptFile(file) { analysis ->
        // Must update UI components on the main thread
        runOnUiThread {
          if (analysis != null) {
            // A. Update Structured Metadata
            tvVendor.text = "Vendor: ${analysis.vendor}"
            tvDate.text = "Date: ${analysis.date}"
            // Use Philippine Peso symbol and format the total price
            tvTotal.text = "Total: ₱${String.format("%.2f", analysis.total)}"

            // B. Update Line Items in the RecyclerView
            // This now receives the clean list thanks to the backend exclusion logic
            adapter.updateItems(analysis.items)

            Toast.makeText(this, "Receipt analyzed successfully!", Toast.LENGTH_SHORT).show()
          } else {
            // Handle analysis failure (e.g., connection error or parse error)
            tvVendor.text = "Vendor: N/A"
            tvDate.text = "Date: N/A"
            tvTotal.text = "Total: ₱0.00"
            adapter.updateItems(emptyList()) // Clear old items
            Toast.makeText(this, "Failed to upload or analyze receipt.", Toast.LENGTH_LONG).show()
          }
        }
      }
    }
  }

  /**
   * Helper function to convert a content URI to a temporary file required by OkHttp.
   */
  private fun writeToFile(context: Context, uri: Uri): File? {
    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
    if (inputStream == null) return null

    val tempFile = File(context.cacheDir, "receipt_upload.jpg")

    try {
      val outputStream = FileOutputStream(tempFile)
      inputStream.copyTo(outputStream)

      outputStream.flush()
      outputStream.close()
      inputStream.close()
      return tempFile
    } catch (e: Exception) {
      e.printStackTrace()
      return null
    }
  }
}