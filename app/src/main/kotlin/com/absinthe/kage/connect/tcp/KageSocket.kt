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

    fun connect(ip: String?, port: Int, timeout: Int) {
        synchronized(this) {
            if (mSocket == null) {
                ConnectThread(ip, port, timeout).start()
            } else {
                Timber.e("Socket is already exist")
            }
        }
    }

    fun disconnect(): Boolean {
        synchronized(this) {
            return if (mSocket != null && mSocket!!.isConnected) {
                try {
                    mIn?.close()
                    mOut?.close()
                    mSocket?.close()
                    mIn = null
                    mOut = null
                    mSocket = null

                    mPacketWriter?.shutdown()
                    mPacketWriter = null

                    mPacketReader?.shutdown()
                    mPacketReader = null

                    GlobalScope.launch(Dispatchers.Main) {
                        mSocketCallback?.onDisConnected()
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
        synchronized(this) {
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

    private inner class ConnectThread(private val ip: String?, private val port: Int, private val timeout: Int) : Thread() {

        override fun run() {
            synchronized(this) {
                if (mSocket == null) {
                    try {
                        mSocket = Socket()

                        mSocket?.let {
                            it.connect(InetSocketAddress(ip, port), timeout)
                            it.keepAlive = true

                            mIn = DataInputStream(it.getInputStream()).apply {
                                mPacketReader = PacketReader(this, mSocketCallback)
                            }
                            mOut = DataOutputStream(it.getOutputStream()).apply {
                                mPacketWriter = PacketWriter(this, mSocketCallback)
                            }

                            GlobalScope.launch(Dispatchers.Main) {
                                mSocketCallback?.onConnected()
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e("Socket connection Exception: $e")
                        mSocket = null
                        when (e) {
                            is SocketTimeoutException -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    mSocketCallback?.onConnectError(ISocketCallback.CONNECT_ERROR_CODE_CONNECT_TIMEOUT, e)
                                }
                            }
                            is ConnectException -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    mSocketCallback?.onConnectError(ISocketCallback.CONNECT_ERROR_CODE_CONNECT_IP_OR_PORT_UNREACHABLE, e)
                                }
                            }
                            else -> {
                                GlobalScope.launch(Dispatchers.Main) {
                                    mSocketCallback?.onConnectError(ISocketCallback.CONNECT_ERROR_CODE_CONNECT_UNKNOWN, e)
                                }
                            }
                        }
                    }
                }
            }
        }
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