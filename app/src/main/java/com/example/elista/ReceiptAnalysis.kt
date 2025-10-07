package com.example.elista

data class ReceiptAnalysis(
    val items: List<ReceiptItem>,
    val total: Double,
    val vendor: String,    // Renamed from vendorName, set to non-nullable String (using "N/A" fallback in ApiClient)
    val date: String,      // Set to non-nullable String (using "N/A" fallback in ApiClient)
    val fullText: String   // Updated for clarity, corresponds to the 'text' field in the backend response
)