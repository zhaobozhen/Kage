package com.absinthe.kage.device;

public abstract class KageObservable {

    abstract public void register(IDeviceObserver observer);
    public abstract void unregister(IDeviceObserver observer);
    abstract protected void notifyFindDevice(Device device);
    abstract protected void notifyLostDevice(Device device);
    abstract protected void notifyDeviceConnected(Device device);
    abstract protected void notifyDeviceDisConnect(Device device);
    protected abstract void notifyDeviceConnectFailed(Device device, int errorCode, String errorMessage);

}
