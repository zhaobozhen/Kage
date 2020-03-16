package com.absinthe.kage.connect.proxy

import com.absinthe.kage.connect.IProxy
import com.absinthe.kage.device.Device

const val MODE_IMAGE = 0
const val MODE_AUDIO = 1
const val MODE_VIDEO = 2

open class BaseProxy : IProxy {
    var mDevice: Device? = null

    override fun onDeviceConnected(device: Device) {
        mDevice = device
    }

    override fun onDeviceDisconnected(device: Device) {
        mDevice = null
    }

    companion object {
        var CURRENT_MODE = MODE_IMAGE
    }
}