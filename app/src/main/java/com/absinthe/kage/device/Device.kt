package com.absinthe.kage.device

import android.util.Log
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
import java.util.*

class Device(private val mConfig: DeviceConfig?, protocolVersionString: String?) {

    private lateinit var mSocket: KageSocket
    private val mOnReceiveMsgListeners: MutableList<OnReceiveMsgListener> = ArrayList()
    private var mConnectCallback: IConnectCallback? = null
    private val mProtocolHandler: IProtocolHandler?
    private val mHeartbeatSender: HeartbeatSender?
    val deviceInfo: DeviceInfo?
    var onlineTime: Long = 0

    private fun heartbeat() {
        val heartbeatId = System.currentTimeMillis().toString()
        mHeartbeatSender!!.beat(heartbeatId, HEARTBEAT_DEFAULT_TIMEOUT, object : IHeartbeatCallback {
            override fun onBeatSuccess(heartbeatId: String) {
                Log.v(TAG, "onBeatSuccess,heartbeatId = $heartbeatId")
            }

            override fun onBeatTimeout(heartbeatId: String) {
                Log.d(TAG, "onBeatTimeout,heartbeatId = $heartbeatId")
                disConnect()
            }

            override fun onBeatCancel(heartbeatId: String) {
                Log.d(TAG, "onBeatCancel,heartbeatId = $heartbeatId")
            }
        })
    }

    fun connect(timeout: Int): Boolean {
        val isConnect = deviceInfo!!.isConnected
        val isInit = mConfig != null && mProtocolHandler != null
        val b = !isConnect && isInit
        return if (b) {
            mHeartbeatSender!!.init()
            deviceInfo.setStateConnecting()
            val ip = deviceInfo.ip
            val port = Config.PORT
            mSocket.connect(ip, port, timeout)
            true
        } else {
            false
        }
    }

    fun disConnect() {
        mHeartbeatSender?.release()
        mSocket.disConnect()
    }

    private fun sendMessage(data: String) {
        val packet = Packet()
        packet.data = data
        mSocket.send(packet)
    }

    fun sendCommand(cmd: Command) {
        val data = cmd.pack()
        data?.let { sendMessage(it) }
    }

    val isConnected: Boolean
        get() = deviceInfo!!.isConnected

    val state: Int
        get() = deviceInfo!!.state

    var ip: String?
        get() = deviceInfo!!.ip
        set(ip) {
            deviceInfo!!.ip = ip
        }

    var name: String?
        get() = deviceInfo!!.name
        set(name) {
            deviceInfo!!.name = name
        }

    val protocolVersion: String?
        get() = deviceInfo!!.protocolVersion

    var functionCode: String?
        get() = deviceInfo!!.functionCode
        set(functionCode) {
            deviceInfo!!.functionCode = functionCode
        }

    fun setConnectCallback(connectCallback: IConnectCallback?) {
        mConnectCallback = connectCallback
    }

    @Synchronized
    fun registerOnReceiveMsgListener(listener: OnReceiveMsgListener?) {
        if (null != listener && !mOnReceiveMsgListeners.contains(listener)) {
            mOnReceiveMsgListeners.add(listener)
            Log.d(TAG, "registerOnReceiveMsgListener")
        }
    }

    @Synchronized
    fun unregisterOnReceiveMsgListener(listener: OnReceiveMsgListener?) {
        if (null != listener) {
            mOnReceiveMsgListeners.remove(listener)
            Log.d(TAG, "unregisterOnReceiveMsgListener")
        }
    }

    interface IConnectCallback {
        fun onConnectedFailed(errorCode: Int, e: Exception?)
        fun onConnected()
        fun onDisConnect()
    }

    interface OnReceiveMsgListener {
        fun onReceiveMsg(msg: String?)
    }

    companion object {
        private val TAG = Device::class.java.simpleName
        private const val HEARTBEAT_DEFAULT_TIMEOUT = 20 * 1000
    }

    init {
        deviceInfo = DeviceInfo()
        deviceInfo.protocolVersion = protocolVersionString
        val mProtocolHandlerCallback: IProtocolHandleCallback = object : IProtocolHandleCallback {
            override fun onProtocolConnected() {
                Log.d(TAG, "onProtocolConnected")
                deviceInfo.isConnected = true
                if (null != mConnectCallback) {
                    mConnectCallback!!.onConnected()
                }
            }

            override fun onProtocolDisConnect() {
                deviceInfo.isConnected = false
                if (null != mConnectCallback) {
                    mConnectCallback!!.onDisConnect()
                }
            }

            override fun onProtocolConnectedFailed(errorCode: Int, e: Exception) {
                deviceInfo.isConnected = false
                if (null != mConnectCallback) {
                    mConnectCallback!!.onConnectedFailed(errorCode, e)
                }
            }

            override fun onProtocolSendOrReceiveError() {
                mSocket.disConnect()
            }
        }

        mProtocolHandler = ProtocolHandler(this, mConfig, mProtocolHandlerCallback)
        mSocket = KageSocket()
        mSocket.setSocketCallback(object : ISocketCallback {
            override fun onConnected() {
                mProtocolHandler.handleSocketConnectedEvent()
            }

            override fun onDisConnected() {
                mProtocolHandler.handleSocketDisConnectEvent()
            }

            override fun onReceiveMsg(msg: String) {
                Log.d(TAG, "onReceiveMsg: $msg")
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
}