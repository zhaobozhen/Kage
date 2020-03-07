package com.absinthe.kage.connect

import com.absinthe.kage.device.Device

interface IProxy {
    fun onDeviceConnected(device: Device)
    fun onDeviceDisconnected(device: Device)
}