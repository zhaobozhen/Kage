package com.absinthe.kage.device;

public abstract class KageObservable {

    abstract public void register(IDeviceObserver observer);

    abstract protected void notifyFindDevice(Device device);

    abstract protected void notifyLostDevice(Device device);

    abstract protected void notifyDeviceConnected(Device device);

    abstract protected void notifyDeviceDisConnect(Device device);

    public abstract void unRegister(IDeviceObserver observer);

    protected abstract void notifyDeviceConnectFailed(Device device, int errorCode, String errorMessage);

}
