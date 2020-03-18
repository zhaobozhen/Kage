package com.absinthe.kage.viewholder

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.kage.R
import com.absinthe.kage.ui.sender.SenderActivity
import com.drakeet.multitype.ItemViewBinder

class CastItemViewBinder : ItemViewBinder<CastItem, CastItemViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root = inflater.inflate(R.layout.item_cast_card, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: CastItem) {
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, SenderActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }

        holder.itemView.setOnTouchListener(HolderConstant.onTouchListener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}