package com.example.android.araibtv03

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.android.araibtv03.entities.ItemDataHolder
import kotlinx.android.synthetic.main.card_item.view.*

class InventoryAdapter(private val exampleList: List<ItemDataHolder>) :

    RecyclerView.Adapter<InventoryAdapter.ItemViewHolder>() {
    lateinit var context: Context
    lateinit var dao: ItemDao

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        context = parent.context
        dao = ItemDatabase.getInstance(context).itemDao()
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.card_item,
            parent, false)
        return ItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = exampleList[position]
        holder.imageView.setImageResource(currentItem.imageResource)
        holder.textView.text = currentItem.itemName
        holder.checkBox.setTag(dao.isCheck(currentItem.itemName))
        holder.checkBox.setChecked(dao.isCheck(currentItem.itemName))

        holder.checkBox.setOnClickListener {
            val b: Boolean = holder.checkBox.isChecked
            dao.updateDone(currentItem.itemName, b)
            Log.e("onChecked", "Boom")
        }
    }

    override fun getItemCount() = exampleList.size

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.image_view
        val textView: TextView = itemView.text_view_1
        val checkBox: CheckBox = itemView.checkbox
    }
}
