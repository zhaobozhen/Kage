package com.absinthe.kage.device

abstract class KageObservable {
    abstract fun register(observer: IDeviceObserver?)
    abstract fun unregister(observer: IDeviceObserver?)
    protected abstract fun notifyFindDevice(device: Device)
    protected abstract fun notifyLostDevice(device: Device)
    protected abstract fun notifyDeviceConnected(device: Device)
    protected abstract fun notifyDeviceDisconnect(device: Device)
    protected abstract fun notifyDeviceConnectFailed(device: Device, errorCode: Int, errorMessage: String?)
}