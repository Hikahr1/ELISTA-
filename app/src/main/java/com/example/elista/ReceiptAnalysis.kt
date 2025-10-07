package com.example.elista

data class ReceiptAnalysis(
    val vendor: String,
    val date: String,
    val total: String,
    val items: List<ReceiptItem>
)
