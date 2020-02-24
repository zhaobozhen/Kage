package com.absinthe.kage.device;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.absinthe.kage.connect.Const;
import com.absinthe.kage.connect.IProxy;
import com.absinthe.kage.connect.protocol.Config;
import com.absinthe.kage.connect.tcp.KageSocket;
import com.absinthe.kage.device.cmd.PromptPhoneConnectedCommand;
import com.absinthe.kage.device.model.DeviceConfig;
import com.absinthe.kage.device.model.DeviceInfo;
import com.absinthe.kage.utils.NetUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DeviceManager extends KageObservable implements LifecycleObserver {
    private static final String TAG = DeviceManager.class.getSimpleName();
    private static final int DEFAULT_CONNECT_TIMEOUT = 10 * 1000;
    private final byte[] LOCK = new byte[0];

    public enum Singleton {
        INSTANCE;
        private DeviceManager instance;

        Singleton() {
            instance = new DeviceManager();
        }

        public DeviceManager getInstance() {
            return instance;
        }
    }

    public enum ConnectFailedReason {
        CONNECT_ERROR_CODE_CONNECT_UNKNOWN, //未知原因
        CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE,  //远端 IP 或 Port 不可到达
        CONNECT_ERROR_CODE_CONNECT_TIMEOUT, //连接超时
        CONNECT_ERROR_CODE_HAND_SHAKE_UNDONE    //协议握手未完成
    }

    public enum Result {
        RESULT_START_MONITOR_DEVICE_SUCCESS,
        RESULT_START_MONITOR_DEVICE_FAILED_NOT_INIT,
        RESULT_START_MONITOR_DEVICE_FAILED_UNAUTHORIZED,
        RESULT_START_MONITOR_DEVICE_FAILED_INTERNAL_ERROR
    }

    private DeviceConfig mConfig;
    private DeviceScanner mDeviceScanner;
    private List<IDeviceObserver> observers = new ArrayList<>();
    private List<IProxy> mProxyList = new ArrayList<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ConnectState mConnectState = null;
    private String mCurrentDeviceKey = null;
    private String delayToConnectDeviceInfoIp = null;

    private DeviceManager() {
        setConnectState(new StateIdle());
    }

    public void init() {
        if (mDeviceScanner == null) {
            mDeviceScanner = new DeviceScanner();
        }

        DeviceConfig config = new DeviceConfig();
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();

        if (defaultAdapter != null) {
            config.name = defaultAdapter.getName();
        } else {
            config.name = android.os.Build.MODEL;
        }

        config.uuid = UUID.randomUUID().toString();
        config.localHost = NetUtils.getLocalAddress();
        config.broadcastHostInWifi = Const.BROADCAST_IP_IN_WIFI;
        config.broadcastHostInAp = Const.BROADCAST_IP_IN_AP;
        config.broadcastMonitorPort = Config.PORT;
        config.broadcastPort = Config.PORT;
        mConfig = config;
        mDeviceScanner.setConfig(mConfig);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void release() {
        if (isConnected()) {
            disConnectDevice();
        }
        if (mDeviceScanner != null) {
            mDeviceScanner.stopScan();
            mDeviceScanner = null;
        }
        mHandler.removeCallbacksAndMessages(null);
    }

    public void startMonitorDevice() {
        startMonitorDevice(6000);
    }

    public int getScanPeriod() {
        if (!checkConfiguration()) {
            return -1;
        }
        return mDeviceScanner.getScanPeriod();
    }

    public DeviceConfig getConfig() {
        return mConfig;
    }

    /**
     * 开始监测设备
     *
     * @param period period
     * @return resultCode
     */
    public int startMonitorDevice(int period) {
        if (!checkConfiguration()) {
            return Result.RESULT_START_MONITOR_DEVICE_FAILED_NOT_INIT.ordinal();
        }
        boolean b = mDeviceScanner.startScan(period, new DeviceScanner.IScanCallback() {
            @Override
            public void onDeviceOnline(final Device device) {
                mHandler.post(() -> notifyFindDevice(device));
            }

            @Override
            public void onDeviceOffline(final Device device) {
                mHandler.post(() -> notifyLostDevice(device));
            }

            @Override
            public void onDeviceInfoChanged(final Device device) {
                mHandler.post(() -> notifyDeviceInfoChanged(device));
            }

            @Override
            public void onDeviceNotice(final Device device) {
                mHandler.post(() -> notifyDeviceNotice(device));
            }
        });
        if (b) {
            return Result.RESULT_START_MONITOR_DEVICE_SUCCESS.ordinal();
        } else {
            return Result.RESULT_START_MONITOR_DEVICE_FAILED_INTERNAL_ERROR.ordinal();
        }
    }

    /**
     * 停止监测设备
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void stopMonitorDevice() {
        if (!checkConfiguration()) {
            return;
        }
        mDeviceScanner.stopScan();
    }

    public List<DeviceInfo> getDeviceInfoList() {
        if (!checkConfiguration()) {
            Log.d(TAG, "checkConfiguration() return false");
            return new ArrayList<>();
        }
        List<DeviceInfo> deviceInfoList = new ArrayList<>();
        Map<String, Device> devices = mDeviceScanner.getDevices();
        for (Device device : devices.values()) {
            DeviceInfo deviceInfo = device.getDeviceInfo();
            if (device.getState() != DeviceInfo.STATE_IDLE) {
                deviceInfoList.add(0, deviceInfo);
            } else {
                deviceInfoList.add(deviceInfo);
            }
        }
        return deviceInfoList;
    }

    @Override
    public synchronized void register(IDeviceObserver observer) {
        if (observer == null) {
            return;
        }
        boolean hasRegistered = false;
        int size = observers.size();
        for (int i = 0; i < size; i++) {
            if (observer.equals(observers.get(i))) {
                hasRegistered = true;
                break;
            }
        }
        if (!hasRegistered) {
            observers.add(observer);
        }
    }

    @Override
    public synchronized void unregister(IDeviceObserver observer) {
        if (observer == null) {
            return;
        }
        observers.remove(observer);
    }

    @Override
    protected void notifyFindDevice(Device device) {
        IDeviceObserver[] localObservers;
        synchronized (this) {
            localObservers = observers.toArray(new IDeviceObserver[0]);
        }
        for (IDeviceObserver observer : localObservers) {
            DeviceInfo DeviceInfo = device.getDeviceInfo();
            observer.onFindDevice(DeviceInfo);
        }
    }

    @Override
    protected void notifyLostDevice(Device device) {
        IDeviceObserver[] localObservers;
        synchronized (this) {
            localObservers = observers.toArray(new IDeviceObserver[0]);
        }
        for (IDeviceObserver observer : localObservers) {
            DeviceInfo DeviceInfo = device.getDeviceInfo();
            observer.onLostDevice(DeviceInfo);
        }
    }

    @Override
    protected void notifyDeviceConnected(Device device) {
        IDeviceObserver[] localObservers;
        synchronized (this) {
            localObservers = observers.toArray(new IDeviceObserver[0]);
        }
        for (IDeviceObserver observer : localObservers) {
            DeviceInfo deviceInfo = device.getDeviceInfo();
            deviceInfo.setState(DeviceInfo.STATE_CONNECTED);
            observer.onDeviceConnected(deviceInfo);
        }
    }

    @Override
    protected void notifyDeviceDisconnect(Device device) {
        IDeviceObserver[] localObservers;
        synchronized (this) {
            localObservers = observers.toArray(new IDeviceObserver[0]);
        }
        for (IDeviceObserver observer : localObservers) {
            DeviceInfo deviceInfo = device.getDeviceInfo();
            deviceInfo.setState(DeviceInfo.STATE_IDLE);
            observer.onDeviceDisConnect(deviceInfo);
        }
    }

    @Override
    protected void notifyDeviceConnectFailed(Device device, int errorCode, String errorMessage) {
        IDeviceObserver[] localObservers;
        synchronized (this) {
            localObservers = observers.toArray(new IDeviceObserver[0]);
        }
        for (IDeviceObserver observer : localObservers) {
            DeviceInfo DeviceInfo = device.getDeviceInfo();
            observer.onDeviceConnectFailed(DeviceInfo, errorCode, errorMessage);
        }
    }

    private void notifyDeviceInfoChanged(Device device) {
        IDeviceObserver[] localObservers;
        synchronized (this) {
            localObservers = observers.toArray(new IDeviceObserver[0]);
        }
        for (IDeviceObserver observer : localObservers) {
            DeviceInfo DeviceInfo = device.getDeviceInfo();
            observer.onDeviceInfoChanged(DeviceInfo);
        }
    }

    private void notifyDeviceConnecting(Device device) {
        IDeviceObserver[] localObservers;
        synchronized (this) {
            localObservers = observers.toArray(new IDeviceObserver[0]);
        }
        for (IDeviceObserver observer : localObservers) {
            DeviceInfo DeviceInfo = device.getDeviceInfo();
            observer.onDeviceConnecting(DeviceInfo);
        }
    }

    private void notifyDeviceNotice(Device device) {
        IDeviceObserver[] localObservers;
        synchronized (this) {
            localObservers = observers.toArray(new IDeviceObserver[0]);
        }
        for (IDeviceObserver observer : localObservers) {
            DeviceInfo DeviceInfo = device.getDeviceInfo();
            observer.onDeviceNotice(DeviceInfo);
        }
    }

    /**
     * 连接指定的设备，已连接事件会通知Observer。
     */
    public void connectDevice(DeviceInfo deviceInfo) {
        synchronized (LOCK) {
            if (checkConfiguration()) {
                mConnectState.connect(deviceInfo);
            }
        }
    }

    /**
     * @param deviceInfo Device info
     * @param timeout    连接超时时间，单位为毫秒
     */
    public synchronized void connectDevice(DeviceInfo deviceInfo, int timeout) {
        if (checkConfiguration()) {
            mConnectState.connect(deviceInfo, timeout);
        }
    }

    /**
     * 如果已连接设备，断开当前连接。已断开事件会通知Observer
     */
    public void disConnectDevice() {
        mConnectState.disConnect();
    }

    private void setConnectState(ConnectState connectState) {
        mConnectState = connectState;
    }

    public DeviceInfo getCurrentDeviceInfo() {
        Device device = getCurrentDevice();
        if (device == null) return null;
        return device.getDeviceInfo();
    }

    @Nullable
    private Device getCurrentDevice() {
        if (!checkConfiguration()) {
            return null;
        }
        Device device;
        synchronized (LOCK) {
            if (null == mCurrentDeviceKey) {
                return null;
            }
            device = mDeviceScanner.queryDevice(mCurrentDeviceKey);
        }
        return device;
    }

    public boolean isConnected() {
        Device currentDevice = getCurrentDevice();
        if (null != currentDevice) {
            return currentDevice.isConnected();
        }
        return false;
    }

    public DeviceInfo onlineDevice(DeviceInfo deviceInfo) {
        if (!checkConfiguration()) {
            return null;
        }
        return mDeviceScanner.onlineDevice(deviceInfo);
    }

    public boolean offlineDevice(DeviceInfo deviceInfo) {
        if (!checkConfiguration()) {
            return false;
        }
        return mDeviceScanner.offlineDevice(deviceInfo);
    }

    private boolean checkConfiguration() {
        return this.mConfig != null && mDeviceScanner != null;
    }

    private class StateIdle implements ConnectState {

        @Override
        public void connect(DeviceInfo deviceInfo) {
            connect(deviceInfo, DEFAULT_CONNECT_TIMEOUT);
        }

        @Override
        public void connect(DeviceInfo deviceInfo, int timeout) {
            Log.d(TAG, "StateIdle connect");
            if (deviceInfo == null) {
                return;
            }
            if (timeout < 0) {
                timeout = DEFAULT_CONNECT_TIMEOUT;
            }
            String key = deviceInfo.getIp();
            final Device device = mDeviceScanner.queryDevice(key);
            if (device == null) {
                return;
            }
            synchronized (LOCK) {
                mCurrentDeviceKey = key;
            }
            setConnectState(new StateConnecting(device));
            device.setConnectCallback(new Device.IConnectCallback() {
                @Override
                public void onConnectedFailed(final int code, Exception e) {
                    Log.d(TAG, "onConnectedFailed");
                    synchronized (LOCK) {
                        mCurrentDeviceKey = null;
                        setConnectState(new StateIdle());
                        String errorMessage = "Unknown error";
                        if (null != e) {
                            errorMessage = e.getMessage();
                        }
                        int errorCode = ConnectFailedReason.CONNECT_ERROR_CODE_CONNECT_UNKNOWN.ordinal();
                        if (KageSocket.ISocketCallback.CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE == code) {
                            errorCode = ConnectFailedReason.CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE.ordinal();
                        } else if (KageSocket.ISocketCallback.CONNECT_ERROR_CODE_CONNECT_TIMEOUT == code) {
                            errorCode = ConnectFailedReason.CONNECT_ERROR_CODE_CONNECT_TIMEOUT.ordinal();
                        } else if (KageSocket.ISocketCallback.CONNECT_ERROR_CODE_HAND_SHAKE_NOT_COMPLETE == code) {
                            errorCode = ConnectFailedReason.CONNECT_ERROR_CODE_HAND_SHAKE_UNDONE.ordinal();
                        } else if (KageSocket.ISocketCallback.CONNECT_ERROR_CODE_CONNECT_UNKNOWN == code) {
                            errorCode = ConnectFailedReason.CONNECT_ERROR_CODE_CONNECT_UNKNOWN.ordinal();
                        }
                        final int finalErrorCode = errorCode;
                        final String finalErrorMessage = errorMessage;
                        mHandler.post(() -> notifyDeviceConnectFailed(device, finalErrorCode, finalErrorMessage));
                    }
                }

                @Override
                public void onConnected() {
                    Log.d(TAG, "onConnectedSuccess");
                    synchronized (LOCK) {
                        PromptPhoneConnectedCommand promptPhoneConnectedCommand = new PromptPhoneConnectedCommand();
                        promptPhoneConnectedCommand.localIp = mConfig.localHost;
                        promptPhoneConnectedCommand.phoneName = mConfig.name;
                        device.sendCommand(promptPhoneConnectedCommand);
                        setConnectState(new DeviceManager.StateConnected(device));
                        notifyDeviceChangedToProxy(device);
                        mHandler.post(() -> notifyDeviceConnected(device));
                    }
                }

                @Override
                public void onDisConnect() {
                    synchronized (LOCK) {
                        mCurrentDeviceKey = null;
                        setConnectState(new StateIdle());
                        notifyDeviceDisconnectToProxy(device);
                        mHandler.post(() -> notifyDeviceDisconnect(device));
                        if (null != delayToConnectDeviceInfoIp && null != mDeviceScanner) {
                            Device dev = mDeviceScanner.queryDevice(delayToConnectDeviceInfoIp);
                            if (dev != null) {
                                connectDevice(dev.getDeviceInfo());
                                delayToConnectDeviceInfoIp = null;
                            }
                        }
                    }
                }
            });
            device.connect(timeout);
            mHandler.post(() -> notifyDeviceConnecting(device));
        }

        @Override
        public void disConnect() {

        }
    }

    private void notifyDeviceChangedToProxy(Device device) {
        IProxy[] tempProxyList = null;
        synchronized (LOCK) {
            int size = mProxyList.size();
            if (0 < size) {
                tempProxyList = new IProxy[size];
                mProxyList.toArray(tempProxyList);
            }
        }
        if (null != tempProxyList) {
            for (IProxy iProxy : tempProxyList) {
                iProxy.onDeviceConnected(device);
            }
        }
    }

    private void notifyDeviceDisconnectToProxy(Device device) {
        IProxy[] tempProxyList = null;
        synchronized (LOCK) {
            int size = mProxyList.size();
            if (0 < size) {
                tempProxyList = new IProxy[size];
                mProxyList.toArray(tempProxyList);
            }
        }
        if (null != tempProxyList) {
            for (IProxy iProxy : tempProxyList) {
                iProxy.onDeviceDisconnected(device);
            }
        }
    }

    private class StateConnecting implements ConnectState {
        private Device mDevice;

        StateConnecting(Device device) {
            mDevice = device;
        }

        @Override
        public void connect(DeviceInfo deviceInfo) {

        }

        @Override
        public void connect(DeviceInfo deviceInfo, int timeout) {

        }

        @Override
        public void disConnect() {
            synchronized (LOCK) {
                mCurrentDeviceKey = null;
            }
        }
    }

    private class StateConnected implements ConnectState {
        private Device mDevice;

        StateConnected(Device device) {
            mDevice = device;
        }

        @Override
        public void connect(DeviceInfo deviceInfo) {
            String ip = deviceInfo.getIp();
            if (!mDevice.getIp().equals(ip)) {
                delayToConnectDeviceInfoIp = ip;
                disConnect();
            }
        }

        @Override
        public void connect(DeviceInfo deviceInfo, int timeout) {

        }

        @Override
        public void disConnect() {
            mDevice.disConnect();
        }
    }

    public void addProxy(IProxy proxy) {
        Device currentDevice = getCurrentDevice();
        if (null != currentDevice && currentDevice.isConnected()) {
            proxy.onDeviceConnected(currentDevice);
        }
        synchronized (LOCK) {
            if (!mProxyList.contains(proxy)) {
                mProxyList.add(proxy);
            }
        }
    }

    public void removeProxy(IProxy proxy) {
        synchronized (LOCK) {
            mProxyList.remove(proxy);
        }
    }

    public void sendCommandToCurrentDevice(Command command) {
        Device device = getCurrentDevice();
        if (device != null) {
            device.sendCommand(command);
        }
    }

    interface ConnectState {
        void connect(DeviceInfo deviceInfo);

        /**
         * @param deviceInfo device info
         * @param timeout    the timeout value in milliseconds
         */
        void connect(DeviceInfo deviceInfo, int timeout);

        void disConnect();
    }
}
