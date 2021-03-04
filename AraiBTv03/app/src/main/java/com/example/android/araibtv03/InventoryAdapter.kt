package com.example.android.araibtv03

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.card_item.view.*

class InventoryAdapter(private val exampleList: List<ItemDataHolder>) :

    RecyclerView.Adapter<InventoryAdapter.ItemViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.card_item,
            parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = exampleList[position]
        holder.imageView.setImageResource(currentItem.imageResource)
        holder.textView.text = currentItem.text
//        holder.checkBox.isChecked = currentItem.checkStatus
        holder.checkBox.setChecked(currentItem.checkStatus)
    }

    override fun getItemCount() = exampleList.size

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.image_view
        val textView: TextView = itemView.text_view_1
        val checkBox: CheckBox = itemView.checkbox
    }
}
