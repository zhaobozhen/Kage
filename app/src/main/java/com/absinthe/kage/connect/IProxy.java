package com.absinthe.kage.connect;

import com.absinthe.kage.device.Device;

public interface IProxy {

    void onDeviceConnected(Device device);
    void onDeviceDisconnected(Device device);

}
