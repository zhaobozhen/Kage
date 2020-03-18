package com.absinthe.kage.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.kage.R
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.model.DeviceInfo
import com.drakeet.multitype.ItemViewBinder

class DeviceInfoItemViewBinder : ItemViewBinder<DeviceInfo, DeviceInfoItemViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root = inflater.inflate(R.layout.item_device, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: DeviceInfo) {
        holder.deviceName.text = item.name
        holder.deviceIp.text = item.ip

        when (item.state) {
            DeviceInfo.STATE_IDLE -> holder.btnConnect.text = holder.itemView.context.getString(R.string.connect_state_connect)
            DeviceInfo.STATE_CONNECTING -> holder.btnConnect.text = holder.itemView.context.getString(R.string.connect_state_connecting)
            DeviceInfo.STATE_CONNECTED -> holder.btnConnect.text = holder.itemView.context.getString(R.string.connect_state_connected)
        }

        holder.btnConnect.setOnClickListener {
            if (!item.isConnected) {
                DeviceManager.onlineDevice(item)
                DeviceManager.connectDevice(item)
            } else {
                DeviceManager.disConnectDevice()
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.tv_device_name)
        val deviceIp: TextView = itemView.findViewById(R.id.tv_device_ip)
        val btnConnect: Button = itemView.findViewById(R.id.btn_connect)
    }
}