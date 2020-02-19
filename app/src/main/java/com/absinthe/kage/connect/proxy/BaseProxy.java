package com.absinthe.kage.connect.proxy;

import com.absinthe.kage.connect.IProxy;
import com.absinthe.kage.device.Device;

public class BaseProxy implements IProxy {
    protected Device mDevice;

    @Override
    public void onDeviceConnected(Device device) {
        mDevice = device;
    }

    @Override
    public void onDeviceDisconnected(Device device) {

    }
}

