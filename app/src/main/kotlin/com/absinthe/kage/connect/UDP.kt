package com.absinthe.kage.connect

import com.absinthe.kage.connect.protocol.IpMessageProtocol
import timber.log.Timber
import java.io.IOException
import java.net.*
import java.nio.charset.StandardCharsets

class UDP(private val mLocalIpAddress: String, monitorPort: Int) {

    private var mDatagramSocket: DatagramSocket? = null
    private var mReceivePacketThread: ReceivePacketThread? = null

    init {
        try {
            mDatagramSocket = DatagramSocket(monitorPort)
        } catch (e: SocketException) {
            Timber.e("Create DatagramSocket error: $e")
        }
    }

    fun notify(ipMsgSend: IpMessageProtocol, ip: String, port: Int) {
        Timber.i("UDP Device Online Message: ${ipMsgSend.protocolString}, IP = $ip")

        val buffer: ByteArray = ipMsgSend.protocolString.toByteArray(StandardCharsets.UTF_8)
        val broadcastAddress: InetAddress

        broadcastAddress = try {
            InetAddress.getByName(ip)
        } catch (e: UnknownHostException) {
            Timber.e("DatagramSocket Send error: $e")
            return
        }

        val packet = DatagramPacket(buffer, buffer.size, broadcastAddress, port)
        try {
            mDatagramSocket?.send(packet)
        } catch (e: IOException) {
            Timber.e("DatagramSocket Send error: $e")
        }
    }

    fun stopReceive() {
        synchronized(UDP::class.java) {
            mReceivePacketThread?.isStop = true
            mReceivePacketThread = null
            if (mDatagramSocket != null && !mDatagramSocket!!.isClosed) {
                mDatagramSocket?.close()
            }
        }
    }

    fun startReceive(callback: IUDPCallback?) {
        synchronized(UDP::class.java) {
            mReceivePacketThread?.isStop = true
            mReceivePacketThread?.interrupt()

            mReceivePacketThread = ReceivePacketThread(mDatagramSocket, mLocalIpAddress)
            mReceivePacketThread?.callback = callback
            mReceivePacketThread?.start()
        }
    }

    private class ReceivePacketThread internal constructor(
            private val mDatagramSocket: DatagramSocket?,
            private val mLocalIpAddress: String) : Thread() {

        var callback: IUDPCallback? = null
        var isStop = false

        override fun run() {
            val buffer = DEFAULT_RECEIVED_BUFFER
            val buffLen = buffer.size
            val packet = DatagramPacket(buffer, buffLen)

            while (!isStop) {
                try {
                    mDatagramSocket?.receive(packet)
                } catch (e: IOException) {
                    Timber.e("DatagramSocket receive error: $e")
                    break
                }

                if (isStop) {
                    break
                }
                if (packet.length <= 0) {
                    Timber.w("Received packet length = ${packet.length}")
                    break
                }

                val data = packet.data
                val dataStr = String(data, 0, packet.length)

                val address = packet.address
                if (address == null) {
                    Timber.e("Address == null")
                    break
                }

                val ip = address.hostAddress
                val port = packet.port
                if (mLocalIpAddress != ip && Const.LOCAL_IP_IN_AP != ip && callback != null) {
                    callback?.onReceive(ip, port, dataStr)
                }
                packet.length = buffLen
            }
        }

    }

    interface IUDPCallback {
        fun onReceive(ip: String, port: Int, data: String)
    }

    companion object {
        private val DEFAULT_RECEIVED_BUFFER = ByteArray(1024)
    }
}