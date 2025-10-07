package com.example.elista

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// MODIFIED: Make items mutable (var) so it can be updated
class ReceiptAdapter(
    private val context: Context,
    private var items: List<ReceiptItem>
) : RecyclerView.Adapter<ReceiptAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtPrice: TextView = itemView.findViewById(R.id.txtPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.txtName.text = item.name

        // Use the Philippine Peso symbol (₱) and format the price to two decimal places
        holder.txtPrice.text = "₱%.2f".format(item.price)
    }

    override fun getItemCount(): Int = items.size

    // NEW FUNCTION: Allows MainActivity to update the item list when analysis is complete
    fun updateItems(newItems: List<ReceiptItem>) {
        items = newItems
        notifyDataSetChanged() // Notify the RecyclerView to redraw itself with the new data
    }
}
