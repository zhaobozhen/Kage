package com.absinthe.kage.device

import com.absinthe.kage.device.model.DeviceInfo

interface IDeviceObserver {
    fun onFindDevice(deviceInfo: DeviceInfo)
    fun onLostDevice(deviceInfo: DeviceInfo)
    fun onDeviceConnected(deviceInfo: DeviceInfo)
    fun onDeviceDisConnect(deviceInfo: DeviceInfo)
    fun onDeviceConnectFailed(deviceInfo: DeviceInfo, errorCode: Int, errorMessage: String?)
    fun onDeviceInfoChanged(deviceInfo: DeviceInfo)
    fun onDeviceNotice(deviceInfo: DeviceInfo)
    fun onDeviceConnecting(deviceInfo: DeviceInfo)
}