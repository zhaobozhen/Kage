package com.absinthe.kage.device

import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.absinthe.kage.connect.IProxy
import com.absinthe.kage.connect.tcp.KageSocket
import com.absinthe.kage.device.Device.IConnectCallback
import com.absinthe.kage.device.DeviceScanner.IScanCallback
import com.absinthe.kage.device.cmd.PromptPhoneConnectedCommand
import com.absinthe.kage.device.model.DeviceConfig
import com.absinthe.kage.device.model.DeviceInfo
import java.util.*

object DeviceManager : KageObservable(), LifecycleObserver {

    private const val TAG = "DeviceManager"
    private const val DEFAULT_CONNECT_TIMEOUT = 10 * 1000

    private val LOCK = ByteArray(0)
    private val observers: MutableList<IDeviceObserver> = ArrayList()
    private val mProxyList: MutableList<IProxy> = ArrayList()
    private val mHandler = Handler(Looper.getMainLooper())

    private var mDeviceScanner: DeviceScanner = DeviceScanner()
    private var mConnectState: ConnectState = StateIdle()
    private var mCurrentDeviceKey: String? = null
    private var delayToConnectDeviceInfoIp: String? = null

    var config: DeviceConfig = DeviceConfig()
        private set

    var scanPeriod: Int
        get() = mDeviceScanner.scanPeriod
        set(value) {
            mDeviceScanner.scanPeriod = value
        }

    fun init() {
        val defaultAdapter = BluetoothAdapter.getDefaultAdapter()

        config.name = if (defaultAdapter != null) defaultAdapter.name else Build.MODEL
        mDeviceScanner.setConfig(config)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun release() {
        if (isConnected) {
            disConnectDevice()
        }
        mDeviceScanner.stopScan()
        mHandler.removeCallbacksAndMessages(null)
    }

    fun startMonitorDevice() {
        startMonitorDevice(scanPeriod)
    }

    /**
     * 开始监测设备
     *
     * @param period period
     * @return resultCode
     */
    private fun startMonitorDevice(period: Int): Int {
        val b = mDeviceScanner.startScan(period, object : IScanCallback {
            override fun onDeviceOnline(device: Device?) {
                mHandler.post { notifyFindDevice(device) }
            }

            override fun onDeviceOffline(device: Device?) {
                mHandler.post { notifyLostDevice(device) }
            }

            override fun onDeviceInfoChanged(device: Device?) {
                mHandler.post { notifyDeviceInfoChanged(device) }
            }

            override fun onDeviceNotice(device: Device?) {
                mHandler.post { notifyDeviceNotice(device) }
            }
        })
        return if (b) {
            Result.RESULT_START_MONITOR_DEVICE_SUCCESS.ordinal
        } else {
            Result.RESULT_START_MONITOR_DEVICE_FAILED_INTERNAL_ERROR.ordinal
        }
    }

    /**
     * 停止监测设备
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun stopMonitorDevice() {
        mDeviceScanner.stopScan()
    }

    val deviceInfoList: List<DeviceInfo>
        get() {
            val deviceInfoList: MutableList<DeviceInfo> = ArrayList()
            val devices = mDeviceScanner.devices
            for (device in devices.values) {
                val deviceInfo = device.deviceInfo
                if (device.state != DeviceInfo.STATE_IDLE) {
                    deviceInfoList.add(0, deviceInfo)
                } else {
                    deviceInfoList.add(deviceInfo)
                }
            }
            return deviceInfoList
        }

    @Synchronized
    override fun register(observer: IDeviceObserver?) {
        if (observer == null) {
            return
        }
        var hasRegistered = false
        val size = observers.size
        for (i in 0 until size) {
            if (observer == observers[i]) {
                hasRegistered = true
                break
            }
        }
        if (!hasRegistered) {
            observers.add(observer)
        }
    }

    @Synchronized
    override fun unregister(observer: IDeviceObserver?) {
        if (observer == null) {
            return
        }
        observers.remove(observer)
    }

    override fun notifyFindDevice(device: Device?) {
        var localObservers: Array<IDeviceObserver>
        synchronized(this) { localObservers = observers.toTypedArray() }
        for (observer in localObservers) {
            val deviceInfo = device!!.deviceInfo
            observer.onFindDevice(deviceInfo)
        }
    }

    override fun notifyLostDevice(device: Device?) {
        var localObservers: Array<IDeviceObserver>
        synchronized(this) { localObservers = observers.toTypedArray() }
        for (observer in localObservers) {
            val deviceInfo = device!!.deviceInfo
            observer.onLostDevice(deviceInfo)
        }
    }

    override fun notifyDeviceConnected(device: Device?) {
        var localObservers: Array<IDeviceObserver>
        synchronized(this) { localObservers = observers.toTypedArray() }
        for (observer in localObservers) {
            val deviceInfo = device!!.deviceInfo
            deviceInfo.state = DeviceInfo.STATE_CONNECTED
            observer.onDeviceConnected(deviceInfo)
        }
    }

    override fun notifyDeviceDisconnect(device: Device?) {
        var localObservers: Array<IDeviceObserver>
        synchronized(this) { localObservers = observers.toTypedArray() }
        for (observer in localObservers) {
            val deviceInfo = device!!.deviceInfo
            deviceInfo.state = DeviceInfo.STATE_IDLE
            observer.onDeviceDisConnect(deviceInfo)
        }
    }

    override fun notifyDeviceConnectFailed(device: Device?, errorCode: Int, errorMessage: String?) {
        var localObservers: Array<IDeviceObserver>
        synchronized(this) { localObservers = observers.toTypedArray() }
        for (observer in localObservers) {
            val deviceInfo = device!!.deviceInfo
            observer.onDeviceConnectFailed(deviceInfo, errorCode, errorMessage)
        }
    }

    private fun notifyDeviceInfoChanged(device: Device?) {
        var localObservers: Array<IDeviceObserver>
        synchronized(this) { localObservers = observers.toTypedArray() }
        for (observer in localObservers) {
            val deviceInfo = device!!.deviceInfo
            observer.onDeviceInfoChanged(deviceInfo)
        }
    }

    private fun notifyDeviceConnecting(device: Device) {
        var localObservers: Array<IDeviceObserver>
        synchronized(this) { localObservers = observers.toTypedArray() }
        for (observer in localObservers) {
            val deviceInfo = device.deviceInfo
            observer.onDeviceConnecting(deviceInfo)
        }
    }

    private fun notifyDeviceNotice(device: Device?) {
        var localObservers: Array<IDeviceObserver>
        synchronized(this) { localObservers = observers.toTypedArray() }
        for (observer in localObservers) {
            val deviceInfo = device!!.deviceInfo
            observer.onDeviceNotice(deviceInfo)
        }
    }

    /**
     * 连接指定的设备，已连接事件会通知Observer。
     */
    fun connectDevice(deviceInfo: DeviceInfo) {
        synchronized(LOCK) {
            mConnectState.connect(deviceInfo)
        }
    }

    /**
     * @param deviceInfo Device info
     * @param timeout    连接超时时间，单位为毫秒
     */
    @Synchronized
    fun connectDevice(deviceInfo: DeviceInfo?, timeout: Int) {
        mConnectState.connect(deviceInfo, timeout)
    }

    /**
     * 如果已连接设备，断开当前连接。已断开事件会通知Observer
     */
    fun disConnectDevice() {
        mConnectState.disConnect()
    }

    private fun setConnectState(connectState: ConnectState) {
        mConnectState = connectState
    }

    val currentDeviceInfo: DeviceInfo?
        get() {
            val device = currentDevice ?: return null
            return device.deviceInfo
        }

    private val currentDevice: Device?
        get() {
            var device: Device?
            synchronized(LOCK) {
                if (null == mCurrentDeviceKey) {
                    return null
                }
                device = mDeviceScanner.queryDevice(mCurrentDeviceKey)
            }
            return device
        }

    val isConnected: Boolean
        get() {
            val currentDevice = currentDevice
            return currentDevice?.isConnected ?: false
        }

    fun onlineDevice(deviceInfo: DeviceInfo?): DeviceInfo? {
        return mDeviceScanner.onlineDevice(deviceInfo!!)
    }

    fun offlineDevice(deviceInfo: DeviceInfo?): Boolean {
        return mDeviceScanner.offlineDevice(deviceInfo)
    }

    private class StateIdle : ConnectState {
        override fun connect(deviceInfo: DeviceInfo) {
            connect(deviceInfo, DEFAULT_CONNECT_TIMEOUT)
        }

        override fun connect(deviceInfo: DeviceInfo?, timeout: Int) {
            var t = timeout
            Log.d(TAG, "StateIdle connect")
            if (deviceInfo == null) {
                return
            }
            if (timeout < 0) {
                t = DEFAULT_CONNECT_TIMEOUT
            }
            val key = deviceInfo.ip
            val device = mDeviceScanner.queryDevice(key) ?: return

            synchronized(LOCK) { mCurrentDeviceKey = key }
            setConnectState(StateConnecting())
            device.setConnectCallback(object : IConnectCallback {

                override fun onConnectedFailed(errorCode: Int, e: Exception?) {
                    Log.d(TAG, "onConnectedFailed")
                    synchronized(LOCK) {
                        mCurrentDeviceKey = null
                        setConnectState(StateIdle())
                        var errorMessage: String? = "Unknown error"
                        if (null != e) {
                            errorMessage = e.message
                        }
                        var code = ConnectFailedReason.CONNECT_ERROR_CODE_CONNECT_UNKNOWN.ordinal

                        when (errorCode) {
                            KageSocket.ISocketCallback.CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE -> {
                                code = ConnectFailedReason.CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE.ordinal
                            }
                            KageSocket.ISocketCallback.CONNECT_ERROR_CODE_CONNECT_TIMEOUT -> {
                                code = ConnectFailedReason.CONNECT_ERROR_CODE_CONNECT_TIMEOUT.ordinal
                            }
                            KageSocket.ISocketCallback.CONNECT_ERROR_CODE_HAND_SHAKE_NOT_COMPLETE -> {
                                code = ConnectFailedReason.CONNECT_ERROR_CODE_HAND_SHAKE_UNDONE.ordinal
                            }
                            KageSocket.ISocketCallback.CONNECT_ERROR_CODE_CONNECT_UNKNOWN -> {
                                code = ConnectFailedReason.CONNECT_ERROR_CODE_CONNECT_UNKNOWN.ordinal
                            }
                        }
                        mHandler.post { notifyDeviceConnectFailed(device, code, errorMessage) }
                    }
                }

                override fun onConnected() {
                    Log.d(TAG, "onConnectedSuccess")
                    synchronized(LOCK) {
                        val promptPhoneConnectedCommand = PromptPhoneConnectedCommand()
                        promptPhoneConnectedCommand.localIp = config.localHost
                        promptPhoneConnectedCommand.phoneName = config.name
                        device.sendCommand(promptPhoneConnectedCommand)
                        setConnectState(StateConnected(device))
                        notifyDeviceChangedToProxy(device)
                        mHandler.post { notifyDeviceConnected(device) }
                    }
                }

                override fun onDisConnect() {
                    synchronized(LOCK) {
                        mCurrentDeviceKey = null
                        setConnectState(StateIdle())
                        notifyDeviceDisconnectToProxy(device)
                        mHandler.post { notifyDeviceDisconnect(device) }

                        if (null != delayToConnectDeviceInfoIp) {
                            val dev = mDeviceScanner.queryDevice(delayToConnectDeviceInfoIp)

                            if (dev != null) {
                                connectDevice(dev.deviceInfo)
                                delayToConnectDeviceInfoIp = null
                            }
                        }
                    }
                }
            })
            device.connect(t)
            mHandler.post { notifyDeviceConnecting(device) }
        }

        override fun disConnect() {}
    }

    private fun notifyDeviceChangedToProxy(device: Device) {
        for (iProxy in mProxyList) {
            iProxy.onDeviceConnected(device)
        }
    }

    private fun notifyDeviceDisconnectToProxy(device: Device) {
        for (iProxy in mProxyList) {
            iProxy.onDeviceDisconnected(device)
        }
    }

    private class StateConnecting internal constructor() : ConnectState {
        override fun connect(deviceInfo: DeviceInfo) {}
        override fun connect(deviceInfo: DeviceInfo?, timeout: Int) {}
        override fun disConnect() {
            synchronized(LOCK) { mCurrentDeviceKey = null }
        }
    }

    private class StateConnected internal constructor(private val mDevice: Device) : ConnectState {
        override fun connect(deviceInfo: DeviceInfo) {
            val ip = deviceInfo.ip
            if (mDevice.ip != ip) {
                delayToConnectDeviceInfoIp = ip
                disConnect()
            }
        }

        override fun connect(deviceInfo: DeviceInfo?, timeout: Int) {}
        override fun disConnect() {
            mDevice.disConnect()
        }

    }

    fun addProxy(proxy: IProxy) {
        val currentDevice = currentDevice
        if (null != currentDevice && currentDevice.isConnected) {
            proxy.onDeviceConnected(currentDevice)
        }
        synchronized(LOCK) {
            if (!mProxyList.contains(proxy)) {
                mProxyList.add(proxy)
            }
        }
    }

    fun removeProxy(proxy: IProxy) {
        synchronized(LOCK) { mProxyList.remove(proxy) }
    }

    fun sendCommandToCurrentDevice(command: Command?) {
        val device = currentDevice
        device?.sendCommand(command!!)
    }

    enum class ConnectFailedReason {
        CONNECT_ERROR_CODE_CONNECT_UNKNOWN,  //未知原因
        CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE,  //远端 IP 或 Port 不可到达
        CONNECT_ERROR_CODE_CONNECT_TIMEOUT,  //连接超时
        CONNECT_ERROR_CODE_HAND_SHAKE_UNDONE //协议握手未完成
    }

    enum class Result {
        RESULT_START_MONITOR_DEVICE_SUCCESS,
        RESULT_START_MONITOR_DEVICE_FAILED_NOT_INIT,
        RESULT_START_MONITOR_DEVICE_FAILED_UNAUTHORIZED,
        RESULT_START_MONITOR_DEVICE_FAILED_INTERNAL_ERROR
    }

    internal interface ConnectState {
        fun connect(deviceInfo: DeviceInfo)

        /**
         * @param deviceInfo device info
         * @param timeout    the timeout value in milliseconds
         */
        fun connect(deviceInfo: DeviceInfo?, timeout: Int)
        fun disConnect()
    }
}