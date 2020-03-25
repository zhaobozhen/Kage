package com.absinthe.kage.viewholder.delegate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.kage.R
import com.absinthe.kage.viewholder.model.DeviceItem
import com.drakeet.multitype.ItemViewBinder

class DeviceItemViewBinder : ItemViewBinder<DeviceItem, DeviceItemViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root = inflater.inflate(R.layout.item_device_card, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: DeviceItem) {
        holder.deviceName.text = item.deviceName
        holder.deviceIp.text = item.deviceIp
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.tv_device_name)
        val deviceIp: TextView = itemView.findViewById(R.id.tv_device_ip)
    }
}