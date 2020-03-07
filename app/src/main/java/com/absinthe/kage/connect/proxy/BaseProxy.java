package com.absinthe.kage.connect.proxy;

import androidx.annotation.NonNull;

import com.absinthe.kage.connect.IProxy;
import com.absinthe.kage.device.Device;

public class BaseProxy implements IProxy {

    Device mDevice;

    @Override
    public void onDeviceConnected(@NonNull Device device) {
        mDevice = device;
    }

    @Override
    public void onDeviceDisconnected(@NonNull Device device) {

    }
}

