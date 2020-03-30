package com.absinthe.kage.device

import com.absinthe.kage.connect.protocol.Config
import com.absinthe.kage.connect.protocol.IProtocolHandler
import com.absinthe.kage.connect.protocol.IProtocolHandler.IProtocolHandleCallback
import com.absinthe.kage.connect.protocol.ProtocolHandler
import com.absinthe.kage.connect.tcp.KageSocket
import com.absinthe.kage.connect.tcp.KageSocket.ISocketCallback
import com.absinthe.kage.connect.tcp.Packet
import com.absinthe.kage.device.heartbeat.HeartbeatSender
import com.absinthe.kage.device.heartbeat.HeartbeatSender.IHeartbeatCallback
import com.absinthe.kage.device.model.DeviceConfig
import com.absinthe.kage.device.model.DeviceInfo
import timber.log.Timber
import java.util.*

class Device(config: DeviceConfig, protocolVersionString: String?) {

    private lateinit var mSocket: KageSocket
    private var mProtocolHandler: IProtocolHandler
    private var mHeartbeatSender: HeartbeatSender
    private var mConnectCallback: IConnectCallback? = null

    private val mOnReceiveMsgListeners: MutableList<OnReceiveMsgListener> = ArrayList()
    val deviceInfo: DeviceInfo = DeviceInfo()
    var onlineTime: Long = 0

    init {
        deviceInfo.protocolVersion = protocolVersionString

        val mProtocolHandlerCallback: IProtocolHandleCallback = object : IProtocolHandleCallback {
            override fun onProtocolConnected() {
                Timber.d("onProtocolConnected")
                deviceInfo.isConnected = true
                mConnectCallback?.onConnected()
            }

            override fun onProtocolDisConnect() {
                deviceInfo.isConnected = false
                mConnectCallback?.onDisConnect()
            }

            override fun onProtocolConnectedFailed(errorCode: Int, e: Exception?) {
                deviceInfo.isConnected = false
                mConnectCallback?.onConnectedFailed(errorCode, e)
            }

            override fun onProtocolSendOrReceiveError() {
                mSocket.disconnect()
            }
        }

        mProtocolHandler = ProtocolHandler(this, config, mProtocolHandlerCallback)

        mSocket = KageSocket()
        mSocket.setSocketCallback(object : ISocketCallback {

            override fun onConnected() {
                mProtocolHandler.handleSocketConnectedEvent()
            }

            override fun onDisConnected() {
                mProtocolHandler.handleSocketDisConnectEvent()
            }

            override fun onReceiveMsg(msg: String) {
                Timber.d("onReceiveMsg: $msg")
                mProtocolHandler.handleSocketMassage(msg)
                if (!deviceInfo.isConnected) {
                    return
                }
                synchronized(this@Device) {
                    for (listener in mOnReceiveMsgListeners) {
                        listener.onReceiveMsg(msg)
                    }
                }
            }

            override fun onConnectError(errorCode: Int, e: Exception) {
                mProtocolHandler.handleSocketConnectFail(errorCode, e)
            }

            override fun onReadAndWriteError(errorCode: Int) {
                mProtocolHandler.handleSocketSendOrReceiveError()
            }

            override fun onWriterIdle() {
                heartbeat()
            }

            override fun onReaderIdle() {}
        })
        mHeartbeatSender = HeartbeatSender(mSocket)
    }

    fun connect(timeout: Int): Boolean {
        return if (!deviceInfo.isConnected) {
            mHeartbeatSender.init()
            deviceInfo.setStateConnecting()
            mSocket.connect(deviceInfo.ip, Config.PORT, timeout)
            true
        } else {
            false
        }
    }

    fun sendCommand(cmd: Command) {
        val data = cmd.pack()
        sendMessage(data)
    }

    fun setConnectCallback(connectCallback: IConnectCallback?) {
        mConnectCallback = connectCallback
    }

    @Synchronized
    fun registerOnReceiveMsgListener(listener: OnReceiveMsgListener?) {
        if (null != listener && !mOnReceiveMsgListeners.contains(listener)) {
            mOnReceiveMsgListeners.add(listener)
            Timber.d("registerOnReceiveMsgListener")
        }
    }

    @Synchronized
    fun unregisterOnReceiveMsgListener(listener: OnReceiveMsgListener?) {
        if (null != listener) {
            mOnReceiveMsgListeners.remove(listener)
            Timber.d("unregisterOnReceiveMsgListener")
        }
    }

    fun disConnect() {
        mHeartbeatSender.release()
        mSocket.disconnect()
    }

    private fun heartbeat() {
        val heartbeatId = System.currentTimeMillis().toString()
        mHeartbeatSender.beat(heartbeatId, HEARTBEAT_DEFAULT_TIMEOUT, object : IHeartbeatCallback {

            override fun onBeatSuccess(heartbeatId: String) {
                Timber.v("onBeatSuccess,heartbeatId = $heartbeatId")
            }

            override fun onBeatTimeout(heartbeatId: String) {
                Timber.d("onBeatTimeout,heartbeatId = $heartbeatId")
                disConnect()
            }

            override fun onBeatCancel(heartbeatId: String) {
                Timber.d("onBeatCancel,heartbeatId = $heartbeatId")
            }
        })
    }

    private fun sendMessage(data: String) {
        mSocket.send(Packet().apply {
            this.data = data
        })
    }

    val isConnected: Boolean
        get() = deviceInfo.isConnected

    val state: Int
        get() = deviceInfo.state

    var ip: String
        get() = deviceInfo.ip
        set(ip) {
            deviceInfo.ip = ip
        }

    var name: String
        get() = deviceInfo.name
        set(name) {
            deviceInfo.name = name
        }

    val protocolVersion: String?
        get() = deviceInfo.protocolVersion

    var functionCode: String?
        get() = deviceInfo.functionCode
        set(functionCode) {
            deviceInfo.functionCode = functionCode
        }

    interface IConnectCallback {
        fun onConnectedFailed(errorCode: Int, e: Exception?)
        fun onConnected()
        fun onDisConnect()
    }

    interface OnReceiveMsgListener {
        fun onReceiveMsg(msg: String)
    }

    companion object {
        private const val HEARTBEAT_DEFAULT_TIMEOUT = 20 * 1000
    }
}