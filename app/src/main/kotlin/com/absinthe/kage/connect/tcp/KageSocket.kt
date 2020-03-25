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
                        mSocket = Socket().apply {
                            connect(InetSocketAddress(ip, port), timeout)
                            keepAlive = true
                            mIn = DataInputStream(getInputStream())
                            mOut = DataOutputStream(getOutputStream())
                        }

                        mOut?.let {
                            mPacketWriter = PacketWriter(it, mSocketCallback)
                            mPacketReader = PacketReader(mIn, mSocketCallback)
                        }

                        GlobalScope.launch(Dispatchers.Main) {
                            mSocketCallback?.onConnected()
                        }
                    } catch (e: Exception) {
                        Timber.i("Socket connection Exception: $e")
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

    fun disconnect(): Boolean {
        synchronized(KageSocket::class.java) {
            mSocket?.let {
                return if (it.isConnected) {
                    try {
                        mIn?.close()
                        mOut?.close()
                        mPacketWriter?.shutdown()
                        mPacketReader?.shutdown()
                        it.close()

                        mIn = null
                        mOut = null
                        mSocket = null
                        mPacketWriter = null
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
            return false
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