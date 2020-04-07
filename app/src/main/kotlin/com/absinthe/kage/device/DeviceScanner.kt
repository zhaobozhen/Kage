package com.absinthe.kage.device

import android.text.TextUtils
import com.absinthe.kage.connect.UDP
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.model.DeviceConfig
import com.absinthe.kage.device.model.DeviceInfo
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class DeviceScanner {

    private val mDevices: MutableMap<String?, Device> = ConcurrentHashMap()

    private lateinit var mScanCallback: IScanCallback
    private var mConfig: DeviceConfig = DeviceConfig()
    private var mUDP: UDP = UDP(mConfig.localHost, mConfig.broadcastMonitorPort)
    private var mNoticeOnlineThread: NoticeOnlineThread = NoticeOnlineThread(mUDP)

    val devices: Map<String?, Device>
        get() = mDevices

    var scanPeriod: Int
        get() {
            synchronized(this) {
                return if (!mNoticeOnlineThread.isStopped) {
                    mNoticeOnlineThread.period
                } else -1
            }
        }
        set(value) {
            synchronized(this) {
                if (!mNoticeOnlineThread.isStopped) {
                    mNoticeOnlineThread.period = value
                }
            }
        }

    fun setConfig(config: DeviceConfig) {
        synchronized(this) { mConfig = config }
    }

    fun queryDevice(key: String?): Device? {
        return mDevices[key]
    }

    fun startScan(period: Int, scanCallback: IScanCallback): Boolean {
        mScanCallback = scanCallback

        synchronized(this) {
            mUDP.stopReceive()
            mUDP = UDP(mConfig.localHost, mConfig.broadcastMonitorPort)
        }

        mUDP.startReceive(object : UDP.IUDPCallback {

            override fun onReceive(ip: String, port: Int, data: String) {
                val ipMessage: IpMessageProtocol
                ipMessage = try {
                    IpMessageProtocol(data)
                } catch (e: NumberFormatException) {
                    Timber.e("Parse UDP data error: $e")
                    return
                }

                val cmd = 0x000000FF and ipMessage.cmd
                var device = mDevices[ip]

                when (cmd) {
                    IpMessageConst.IP_MSG_BR_EXIT -> {
                        device?.let {
                            if (!it.isConnected) {
                                mDevices.remove(ip)
                                mScanCallback.onDeviceOffline(it)
                            }
                        }
                    }
                    IpMessageConst.IP_MSG_BR_ENTRY -> {
                        val ipMsgSend = IpMessageProtocol().apply {
                            this.version = IpMessageConst.VERSION.toString()
                            this.senderName = mConfig.name
                            this.cmd = IpMessageConst.IP_MSG_ANS_ENTRY // 回送报文命令
                            this.additionalSection = mConfig.uuid
                        }
                        mUDP.notify(ipMsgSend, ip, port)
                    }
                    IpMessageConst.IP_MSG_ANS_ENTRY -> {
                        if (device == null) {
                            device = Device(mConfig, ipMessage.version).apply {
                                this.ip = ip
                                this.name = ipMessage.senderName
                            }

                            val userInfo = ipMessage.additionalSection
                                    .split(IpMessageProtocol.DELIMITER).toTypedArray()

                            if (userInfo.isEmpty()) {
                                device.name = ipMessage.senderName
                            } else if (userInfo.size >= 2) {
                                device.functionCode = userInfo[1]
                            }
                            mDevices[ip] = device
                            mScanCallback.onDeviceOnline(device)
                        } else if (isDeviceInfoChanged(ipMessage, device)) {
                            device.name = ipMessage.senderName
                            val userInfo = ipMessage.additionalSection
                                    .split(IpMessageProtocol.DELIMITER).toTypedArray()

                            if (userInfo.isEmpty()) {
                                device.name = ipMessage.senderName
                            } else if (userInfo.size >= 2) {
                                device.functionCode = userInfo[1]
                            }
                            mScanCallback.onDeviceInfoChanged(device)
                        }

                        mScanCallback.onDeviceNotice(device)
                        updateOnlineTime(device)
                    }
                }
            }

        })

        synchronized(this) {
            mNoticeOnlineThread.apply {
                isStopped = true
                interrupt()
            }

            mNoticeOnlineThread = NoticeOnlineThread(mUDP).apply {
                this.period = period
                start()
            }
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
        synchronized(this) {
            mNoticeOnlineThread.apply {
                isStopped = true
                interrupt()
            }

            mUDP.stopReceive()
            offlineALlDevices()
        }
    }

    fun onlineDevice(deviceInfo: DeviceInfo): DeviceInfo? {
        return if (!mDevices.containsKey(deviceInfo.ip)) {
            val ip = deviceInfo.ip
            val name = deviceInfo.name
            val protocolVersion = deviceInfo.protocolVersion
            val functionCode = deviceInfo.functionCode

            if (TextUtils.isEmpty(protocolVersion) or TextUtils.isEmpty(functionCode)) {
                return null
            }

            val device = Device(mConfig, protocolVersion).apply {
                this.ip = ip
                this.name = name
                this.functionCode = functionCode
            }
            mDevices[ip] = device
            mScanCallback.onDeviceOnline(device)
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

        if (mDevices.containsKey(deviceInfo.ip)) {
            mDevices.remove(deviceInfo.ip)?.let {
                mScanCallback.onDeviceOffline(it)
                return true
            } ?: return false
        } else {
            return false
        }
    }

    internal inner class NoticeOnlineThread(private val udp: UDP) : Thread() {

        @Transient
        var isStopped = false
        private var mPeriod = DEFAULT_PERIOD

        var period: Int
            get() = mPeriod
            set(period) {
                mPeriod = period.coerceAtLeast(MIN_PERIOD)
            }

        override fun run() {
            val ipMsgSend = IpMessageProtocol().apply {
                version = IpMessageConst.VERSION.toString()
                senderName = mConfig.name
                cmd = IpMessageConst.IP_MSG_BR_ENTRY // 上线命令
                additionalSection = mConfig.uuid
            }
            val broadCastHost = mConfig.broadcastHostInWifi
            val broadCastHostInAp = mConfig.broadcastHostInAp
            val broadcastPort = mConfig.broadcastPort

            while (!isInterrupted && !isStopped) {
                checkOffline()
                udp.notify(ipMsgSend, broadCastHost, broadcastPort)
                udp.notify(ipMsgSend, broadCastHostInAp, broadcastPort)

                try {
                    sleep(mPeriod.toLong())
                } catch (e: InterruptedException) {
                    Timber.e("NoticeOnlineThread InterruptedException")
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
            Timber.d("Check Offline IP = ${device.ip}, State = ${device.state}, SpaceTime = $spaceTime")

            if (spaceTime > TIMEOUT && device.state == DeviceInfo.STATE_IDLE) {
                iterator.remove()
                mScanCallback.onDeviceOffline(device)
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
            mScanCallback.onDeviceOffline(device)
        }
    }

    interface IScanCallback {
        fun onDeviceOnline(device: Device?)
        fun onDeviceOffline(device: Device?)
        fun onDeviceInfoChanged(device: Device?)
        fun onDeviceNotice(device: Device?)
    }

    companion object {
        private const val TIMEOUT = 5000 //5 秒间隔询问无回复则判定为无响应
        private const val MIN_PERIOD = 1000 //间隔至少1秒
        private const val DEFAULT_PERIOD = 6000
    }
}