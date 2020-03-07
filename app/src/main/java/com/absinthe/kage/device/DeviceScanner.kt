package com.absinthe.kage.device

import android.text.TextUtils
import android.util.Log
import com.absinthe.kage.connect.UDP
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.model.DeviceConfig
import com.absinthe.kage.device.model.DeviceInfo
import java.util.concurrent.ConcurrentHashMap

class DeviceScanner {

    private val LOCK = ByteArray(0)
    private var mUDP: UDP? = null
    private var mScanCallback: IScanCallback? = null
    private var mNoticeOnlineThread: NoticeOnlineThread? = null
    private val mDevices: MutableMap<String?, Device> = ConcurrentHashMap()
    private var mConfig: DeviceConfig = DeviceConfig()

    val devices: Map<String?, Device>
        get() = mDevices

    var scanPeriod: Int
        get() {
            synchronized(LOCK) {
                return if (mNoticeOnlineThread != null && !mNoticeOnlineThread!!.isStopped) {
                    mNoticeOnlineThread!!.period
                } else -1
            }
        }
        set(value) {
            synchronized(LOCK) {
                if (mNoticeOnlineThread != null && !mNoticeOnlineThread!!.isStopped) {
                    mNoticeOnlineThread!!.period = value
                }
            }
        }

    fun setConfig(config: DeviceConfig) {
        synchronized(LOCK) { mConfig = config }
    }

    fun queryDevice(key: String?): Device? {
        return mDevices[key]
    }

    fun startScan(period: Int, scanCallback: IScanCallback): Boolean {
        mScanCallback = scanCallback

        synchronized(LOCK) {
            mUDP?.stopReceive()
            mUDP = UDP(mConfig.localHost, mConfig.broadcastMonitorPort)
        }

        mUDP?.startReceive(object : UDP.IUDPCallback {

            override fun onReceive(ip: String, port: Int, data: String) {
                val ipMessage: IpMessageProtocol
                ipMessage = try {
                    IpMessageProtocol(data)
                } catch (e: NumberFormatException) {
                    Log.e(TAG, "Parse UDP data error: $e")
                    return
                }

                val cmd = 0x000000FF and ipMessage.cmd
                var device = mDevices[ip]
                when (cmd) {
                    IpMessageConst.IP_MSG_BR_EXIT -> if (device != null && !device.isConnected) {
                        mDevices.remove(ip)
                        mScanCallback?.onDeviceOffline(device)
                    }
                    IpMessageConst.IP_MSG_BR_ENTRY -> {
                        Log.d(TAG, "IP_MSG_BR_ENTRY")
                        val ipMsgSend = IpMessageProtocol()
                        ipMsgSend.version = IpMessageConst.VERSION.toString()
                        ipMsgSend.senderName = mConfig.name
                        ipMsgSend.cmd = IpMessageConst.IP_MSG_ANS_ENTRY // 回送报文命令
                        ipMsgSend.additionalSection = mConfig.uuid
                        mUDP?.notify(ipMsgSend, ip, port)
                    }
                    IpMessageConst.IP_MSG_ANS_ENTRY -> {
                        Log.d(TAG, "IP_MSG_ANS_ENTRY")
                        if (device == null) {
                            val protocolVersion = ipMessage.version
                            device = Device(mConfig, protocolVersion)
                            device.ip = ip
                            device.name = ipMessage.senderName
                            val additionalSection = ipMessage.additionalSection
                            val userInfo = additionalSection?.split(IpMessageProtocol.DELIMITER)?.toTypedArray()

                            if (userInfo != null) {
                                if (userInfo.isEmpty()) {
                                    device.name = ipMessage.senderName
                                }
                            }
                            if (userInfo != null) {
                                if (userInfo.size >= 2) {
                                    device.functionCode = userInfo[1]
                                }
                            }
                            mDevices[ip] = device
                            mScanCallback?.onDeviceOnline(device)
                        } else if (isDeviceInfoChanged(ipMessage, device)) {
                            device.name = ipMessage.senderName
                            val additionalSection = ipMessage.additionalSection
                            val userInfo = additionalSection?.split(IpMessageProtocol.DELIMITER)?.toTypedArray()
                            if (userInfo != null) {
                                if (userInfo.isEmpty()) {
                                    device.name = ipMessage.senderName
                                }
                            }
                            if (userInfo != null) {
                                if (userInfo.size >= 2) {
                                    device.functionCode = userInfo[1]
                                }
                            }
                            mScanCallback?.onDeviceInfoChanged(device)
                        }
                        mScanCallback?.onDeviceNotice(device)
                        updateOnlineTime(device)
                    }
                    else -> {
                    }
                }
            }

        })
        synchronized(LOCK) {
            mNoticeOnlineThread?.isStopped = true
            mNoticeOnlineThread?.interrupt()
            mNoticeOnlineThread = NoticeOnlineThread(mUDP)
            mNoticeOnlineThread?.period = period
            mNoticeOnlineThread?.start()
        }
        return true
    }

    private fun isDeviceInfoChanged(ipMessage: IpMessageProtocol, device: Device): Boolean {
        return if (device.name.isNotEmpty()) {
            device.name != ipMessage.senderName
        } else {
            ipMessage.senderName.isNotEmpty()
        }
    }

    fun stopScan() {
        synchronized(LOCK) {
            mNoticeOnlineThread?.isStopped = true
            mNoticeOnlineThread?.interrupt()
            mNoticeOnlineThread = null
            mUDP?.stopReceive()
            mUDP = null
            offlineALlDevices()
            mScanCallback = null
        }
    }

    fun onlineDevice(deviceInfo: DeviceInfo): DeviceInfo? {
        return if (!mDevices.containsKey(deviceInfo.ip)) {
            val ip = deviceInfo.ip
            val name = deviceInfo.name
            val protocolVersion = deviceInfo.protocolVersion
            val functionCode = deviceInfo.functionCode
            if (TextUtils.isEmpty(protocolVersion)
                    or TextUtils.isEmpty(functionCode)) {
                return null
            }
            val device = Device(mConfig, protocolVersion)
            device.ip = ip
            device.name = name
            device.functionCode = functionCode
            mDevices[ip] = device
            if (mScanCallback != null) {
                mScanCallback!!.onDeviceOnline(device)
            }
            updateOnlineTime(device)
            device.deviceInfo
        } else {
            val device = mDevices[deviceInfo.ip]
            device?.deviceInfo
        }
    }

    fun offlineDevice(deviceInfo: DeviceInfo?): Boolean {
        if (deviceInfo == null) {
            return false
        }
        val ip = deviceInfo.ip
        return if (mDevices.containsKey(ip)) {
            val device = mDevices.remove(ip)
            if (null != device) {
                if (mScanCallback != null) {
                    mScanCallback!!.onDeviceOffline(device)
                }
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    internal inner class NoticeOnlineThread(private val mUDP: UDP?) : Thread() {

        val MIN_PERIOD = 1000 //间隔至少1秒
        val DEFAULT_PERIOD = 6000

        @Transient
        var isStopped = false
        private var mPeriod = DEFAULT_PERIOD

        var period: Int
            get() = mPeriod
            set(period) {
                mPeriod = period.coerceAtLeast(MIN_PERIOD)
            }

        override fun run() {
            val ipMsgSend = IpMessageProtocol()
            ipMsgSend.version = IpMessageConst.VERSION.toString()
            ipMsgSend.senderName = mConfig.name
            ipMsgSend.cmd = IpMessageConst.IP_MSG_BR_ENTRY // 上线命令
            ipMsgSend.additionalSection = mConfig.uuid

            val broadCastHost = mConfig.broadcastHostInWifi
            val broadcastPort = mConfig.broadcastPort
            val broadCastHostInAp = mConfig.broadcastHostInAp

            while (!isInterrupted && !isStopped) {
                checkOffline()
                if (mUDP == null) {
                    break
                }
                mUDP.notify(ipMsgSend, broadCastHost, broadcastPort)
                mUDP.notify(ipMsgSend, broadCastHostInAp, broadcastPort)
                try {
                    sleep(mPeriod.toLong())
                } catch (e: InterruptedException) {
                    Log.e(TAG, "NoticeOnlineThread InterruptedException,")
                }
                if (isStopped) {
                    break
                }
            }
        }
    }

    private fun checkOffline() {
        val iterator: MutableIterator<Map.Entry<String?, Device>> = mDevices.entries.iterator()
        var spaceTime: Long
        while (iterator.hasNext()) {
            val device = iterator.next().value
            spaceTime = System.currentTimeMillis() - device.onlineTime
            Log.d(TAG, "Check Offline IP = ${device.ip}, State = ${device.state}, SpaceTime = $spaceTime")
            if (spaceTime > TIMEOUT && device.state == DeviceInfo.STATE_IDLE) {
                iterator.remove()
                if (mScanCallback != null) {
                    mScanCallback!!.onDeviceOffline(device)
                }
            }
        }
    }

    private fun updateOnlineTime(device: Device) {
        device.onlineTime = System.currentTimeMillis()
    }

    private fun offlineALlDevices() {
        val iterator: MutableIterator<Map.Entry<String?, Device>> = mDevices.entries.iterator()
        while (iterator.hasNext()) {
            val device = iterator.next().value
            iterator.remove()
            if (mScanCallback != null) {
                mScanCallback!!.onDeviceOffline(device)
            }
        }
    }

    interface IScanCallback {
        fun onDeviceOnline(device: Device?)
        fun onDeviceOffline(device: Device?)
        fun onDeviceInfoChanged(device: Device?)
        fun onDeviceNotice(device: Device?)
    }

    companion object {
        private val TAG = DeviceScanner::class.java.simpleName
        private const val TIMEOUT = 5000 //5 秒间隔询问无回复则判定为无响应
    }
}