package com.absinthe.kage.device;

import com.absinthe.kage.device.model.DeviceInfo;

public interface IDeviceObserver {

    void onFindDevice(DeviceInfo deviceInfo);
    void onLostDevice(DeviceInfo deviceInfo);
    void onDeviceConnected(DeviceInfo deviceInfo);
    void onDeviceDisConnect(DeviceInfo deviceInfo);
    void onDeviceConnectFailed(DeviceInfo deviceInfo, int errorCode, String errorMessage);
    void onDeviceInfoChanged(DeviceInfo deviceInfo);
    void onDeviceNotice(DeviceInfo deviceInfo);
    void onDeviceConnecting(DeviceInfo deviceInfo);

}
