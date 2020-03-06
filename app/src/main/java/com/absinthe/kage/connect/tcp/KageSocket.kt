package com.absinthe.kage.connect.tcp

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.absinthe.kage.connect.protocol.IProtocolHandler.KageProtocolThreadHandler
import com.absinthe.kage.connect.tcp.KageSocket.ISocketCallback.KageSocketCallbackThreadHandler
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.*

class KageSocket {

    private var mSocket: Socket? = null
    private var mSocketCallback: ISocketCallback? = null
    private var mIn: DataInputStream? = null
    private var mOut: DataOutputStream? = null
    private var mPacketWriter: IPacketWriter? = null
    private var mPacketReader: IPacketReader? = null

    fun connect(ip: String?, port: Int, timeout: Int) {
        synchronized(KageSocket::class.java) {
            if (mSocket == null) {
                ConnectThread(ip, port, timeout).start()
            } else {
                Log.e(TAG, "Socket is already exist")
            }
        }
    }

    private inner class ConnectThread(private val ip: String?, private val port: Int, private val timeout: Int) : Thread() {

        override fun run() {
            synchronized(KageSocket::class.java) {
                if (mSocket == null) {
                    try {
                        mSocket = Socket()

                        val endPoint: SocketAddress = InetSocketAddress(ip, port)
                        mSocket!!.connect(endPoint, timeout)
                        mSocket!!.keepAlive = true
                        mIn = DataInputStream(mSocket!!.getInputStream())
                        mOut = DataOutputStream(mSocket!!.getOutputStream())
                        mPacketWriter = PacketWriter(mOut!!, mSocketCallback)
                        mPacketReader = PacketReader(mIn, mSocketCallback)

                        KageSocketCallbackThreadHandler.instance!!.post {
                            if (mSocketCallback != null) {
                                mSocketCallback!!.onConnected()
                            }
                        }
                    } catch (e: Exception) {
                        Log.i(TAG, "Socket connection Exception: $e")
                        mSocket = null
                        when (e) {
                            is SocketTimeoutException -> {
                                KageSocketCallbackThreadHandler.instance!!.post {
                                    if (mSocketCallback != null) {
                                        mSocketCallback!!.onConnectError(ISocketCallback.CONNECT_ERROR_CODE_CONNECT_TIMEOUT, e)
                                    }
                                }
                            }
                            is ConnectException -> {
                                KageSocketCallbackThreadHandler.instance!!.post {
                                    if (mSocketCallback != null) {
                                        mSocketCallback!!.onConnectError(ISocketCallback.CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE, e)
                                    }
                                }
                            }
                            else -> {
                                KageSocketCallbackThreadHandler.instance!!.post {
                                    if (mSocketCallback != null) {
                                        mSocketCallback!!.onConnectError(ISocketCallback.CONNECT_ERROR_CODE_CONNECT_UNKNOWN, e)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    fun disConnect(): Boolean {
        synchronized(KageSocket::class.java) {
            return if (mSocket != null && mSocket!!.isConnected) {
                try {
                    mIn!!.close()
                    mOut!!.close()
                    mSocket!!.close()
                    mIn = null
                    mOut = null
                    mSocket = null
                    if (null != mPacketWriter) {
                        mPacketWriter!!.shutdown()
                        mPacketWriter = null
                    }
                    if (null != mPacketReader) {
                        mPacketReader!!.shutdown()
                        mPacketReader = null
                    }
                    KageSocketCallbackThreadHandler.instance!!.post {
                        if (mSocketCallback != null) {
                            mSocketCallback!!.onDisConnected()
                        }
                    }
                    true
                } catch (e: IOException) {
                    Log.e(TAG, e.toString())
                    false
                }
            } else false
        }
    }

    fun send(packet: Packet?) {
        synchronized(KageSocket::class.java) {
            if (mPacketWriter != null) {
                if (packet is Request) {
                    packet.id = System.currentTimeMillis().toString()
                    mPacketReader?.addRequest(packet)
                }
                mPacketWriter?.writePacket(packet)
            } else {
                Log.e(TAG, "Send error: PacketWriter == null")
            }
        }
    }

    fun setSocketCallback(socketCallback: ISocketCallback?) {
        mSocketCallback = socketCallback
    }

    interface ISocketCallback {
        fun onConnected()
        fun onDisConnected()
        fun onReceiveMsg(msg: String)
        fun onConnectError(errorCode: Int, e: Exception)
        fun onReadAndWriteError(errorCode: Int)
        fun onWriterIdle()
        fun onReaderIdle()

        class KageSocketCallbackThreadHandler(looper: Looper) : Handler(looper) {
            companion object {
                private var mHandlerThread: HandlerThread? = null
                private var mHandler: KageProtocolThreadHandler? = null
                val instance: KageProtocolThreadHandler?
                    get() {
                        if (null == mHandler) {
                            synchronized(KageProtocolThreadHandler::class.java) {
                                if (null == mHandler) {
                                    if (null == mHandlerThread) {
                                        mHandlerThread = HandlerThread(KageSocketCallbackThreadHandler::class.java.simpleName)
                                        mHandlerThread!!.start()
                                    }
                                    mHandler = KageProtocolThreadHandler(mHandlerThread!!.looper)
                                }
                            }
                        }
                        return mHandler
                    }
            }
        }

        companion object {
            const val CONNECT_ERROR_CODE_CONNECT_UNKNOWN = 1
            const val CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE = 2
            const val CONNECT_ERROR_CODE_CONNECT_TIMEOUT = 3
            const val CONNECT_ERROR_CODE_HAND_SHAKE_NOT_COMPLETE = 4
            const val READ_ERROR_CODE_CONNECT_UNKNOWN = 101
            const val WRITE_ERROR_CODE_CONNECT_UNKNOWN = 102
            const val READ_ERROR_CODE_RECEIVE_LENGTH_TOO_BIG = 103
        }
    }

    private val isConnected: Boolean
        get() = null != mSocket
                && null != mIn
                && null != mOut
                && null != mPacketWriter
                && null != mPacketReader

    companion object {
        private val TAG = KageSocket::class.java.simpleName
    }
}