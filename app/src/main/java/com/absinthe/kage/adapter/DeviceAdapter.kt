package com.absinthe.kage.adapter

import com.absinthe.kage.R
import com.absinthe.kage.device.model.DeviceInfo
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

open class DeviceAdapter : BaseQuickAdapter<DeviceInfo, BaseViewHolder>(R.layout.item_device) {

    override fun convert(helper: BaseViewHolder, item: DeviceInfo) {
        helper.setText(R.id.tv_device_name, item.name)
        helper.setText(R.id.tv_device_ip, item.ip)

        when (item.state) {
            DeviceInfo.STATE_IDLE -> helper.setText(R.id.btn_connect, R.string.connect_state_connect)
            DeviceInfo.STATE_CONNECTING -> helper.setText(R.id.btn_connect, R.string.connect_state_connecting)
            DeviceInfo.STATE_CONNECTED -> helper.setText(R.id.btn_connect, R.string.connect_state_connected)
        }
    }

    init {
        addChildClickViewIds(R.id.btn_connect)
    }
}