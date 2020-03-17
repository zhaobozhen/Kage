package com.absinthe.kage.connect.tcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
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

    private val isConnected: Boolean
        get() = null != mSocket
                && null != mIn
                && null != mOut
                && null != mPacketWriter
                && null != mPacketReader
    
    fun connect(ip: String?, port: Int, timeout: Int) {
        synchronized(KageSocket::class.java) {
            if (mSocket == null) {
                ConnectThread(ip, port, timeout).start()
            } else {
                Timber.e("Socket is already exist")
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

                        GlobalScope.launch(Dispatchers.Main) {
                            mSocketCallback!!.onConnected()
                        }
                    } catch (e: Exception) {
                        Timber.i("Socket connection Exception: $e")
                        mSocket = null
                        when (e) {
                            is SocketTimeoutException -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    mSocketCallback!!.onConnectError(ISocketCallback.CONNECT_ERROR_CODE_CONNECT_TIMEOUT, e)
                                }
                            }
                            is ConnectException -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    mSocketCallback!!.onConnectError(ISocketCallback.CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE, e)
                                }
                            }
                            else -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    mSocketCallback!!.onConnectError(ISocketCallback.CONNECT_ERROR_CODE_CONNECT_UNKNOWN, e)
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    fun disconnect(): Boolean {
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
                    GlobalScope.launch(Dispatchers.Main) {
                        mSocketCallback!!.onDisConnected()
                    }
                    true
                } catch (e: IOException) {
                    Timber.e(e.toString())
                    false
                }
            } else false
        }
    }

    fun send(packet: Packet) {
        synchronized(KageSocket::class.java) {
            if (mPacketWriter != null) {
                if (packet is Request) {
                    packet.id = System.currentTimeMillis().toString()
                    mPacketReader?.addRequest(packet)
                }
                mPacketWriter?.writePacket(packet)
            } else {
                Timber.e("Send error: PacketWriter == null")
            }
        }
    }

    fun setSocketCallback(socketCallback: ISocketCallback) {
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
}