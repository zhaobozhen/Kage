package com.absinthe.kage.device;

import com.absinthe.kage.device.model.DeviceInfo;

public class DeviceObserverImpl implements IDeviceObserver {

    @Override
    public void onFindDevice(DeviceInfo deviceInfo) { }

    @Override
    public void onLostDevice(DeviceInfo deviceInfo) { }

    @Override
    public void onDeviceConnected(DeviceInfo deviceInfo) { }

    @Override
    public void onDeviceDisConnect(DeviceInfo deviceInfo) { }

    @Override
    public void onDeviceConnectFailed(DeviceInfo deviceInfo, int errorCode, String errorMessage) { }

    @Override
    public void onDeviceInfoChanged(DeviceInfo deviceInfo) { }

    @Override
    public void onDeviceNotice(DeviceInfo deviceInfo) { }

    @Override
    public void onDeviceConnecting(DeviceInfo deviceInfo) { }
}
