package com.absinthe.kage.device

import com.absinthe.kage.device.model.DeviceInfo

open class DeviceObserverImpl : IDeviceObserver {
    override fun onFindDevice(deviceInfo: DeviceInfo) {}
    override fun onLostDevice(deviceInfo: DeviceInfo) {}
    override fun onDeviceConnected(deviceInfo: DeviceInfo) {}
    override fun onDeviceDisConnect(deviceInfo: DeviceInfo) {}
    override fun onDeviceConnectFailed(deviceInfo: DeviceInfo, errorCode: Int, errorMessage: String?) {}
    override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {}
    override fun onDeviceNotice(deviceInfo: DeviceInfo) {}
    override fun onDeviceConnecting(deviceInfo: DeviceInfo) {}
}