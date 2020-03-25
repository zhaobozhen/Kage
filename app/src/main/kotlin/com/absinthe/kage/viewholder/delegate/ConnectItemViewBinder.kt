package com.absinthe.kage.viewholder.delegate

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.kage.R
import com.absinthe.kage.ui.connect.ConnectActivity
import com.absinthe.kage.viewholder.HolderConstant
import com.absinthe.kage.viewholder.model.ConnectItem
import com.drakeet.multitype.ItemViewBinder

class ConnectItemViewBinder : ItemViewBinder<ConnectItem, ConnectItemViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root = inflater.inflate(R.layout.item_connect_card, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: ConnectItem) {
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ConnectActivity::class.java)
            holder.itemView.context.startActivity(intent)
        }

        holder.itemView.setOnTouchListener(HolderConstant.onTouchListener)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}