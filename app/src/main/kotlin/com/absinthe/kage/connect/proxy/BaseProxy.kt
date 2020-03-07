package com.absinthe.kage.connect.proxy

import com.absinthe.kage.connect.IProxy
import com.absinthe.kage.device.Device

open class BaseProxy : IProxy {
    @JvmField
    var mDevice: Device? = null

    override fun onDeviceConnected(device: Device) {
        mDevice = device
    }

    override fun onDeviceDisconnected(device: Device) {
        mDevice = null
    }
}