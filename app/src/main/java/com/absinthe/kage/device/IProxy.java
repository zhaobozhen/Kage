package com.absinthe.kage.device;

public interface IProxy {

    void onDeviceConnected(Device device);
    void onDeviceDisconnected(Device device);

}
