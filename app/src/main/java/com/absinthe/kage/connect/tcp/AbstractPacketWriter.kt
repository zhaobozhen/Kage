package com.absinthe.kage.connect.tcp

import android.util.Log
import com.absinthe.kage.connect.tcp.KageSocket.ISocketCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

open class AbstractPacketWriter(out: DataOutputStream, socketCallback: ISocketCallback?) : IPacketWriter {

    private var timeout = DEFAULT_TIMEOUT
    private var mPacketQueue = LinkedBlockingQueue<Packet?>()
    private var shutdown = false

    @Throws(IOException::class)
    protected fun writeToStream(dos: DataOutputStream, packet: Packet) {
        val data = packet.data
        Log.d(TAG, "Send data: $data")

        val bArray = data!!.toByteArray(StandardCharsets.UTF_8)
        val sendLen = bArray.size

        dos.writeInt(sendLen)
        dos.flush()
        dos.write(bArray, 0, sendLen)
        dos.flush()
    }

    override fun writePacket(packet: Packet?) {
        try {
            mPacketQueue.put(packet)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun shutdown() {
        shutdown = true
        mPacketQueue.clear()
    }

    companion object {
        private val TAG = AbstractPacketWriter::class.java.simpleName
        protected const val DEFAULT_TIMEOUT = 5 * 1000.toLong()
    }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            while (!shutdown) {
                try {
                    val packet = mPacketQueue.poll(timeout, TimeUnit.MILLISECONDS)
                    if (shutdown) {
                        break
                    }

                    packet?.let { writeToStream(out, it) }
                            ?: ISocketCallback.KageSocketCallbackThreadHandler.instance?.post {
                                socketCallback?.onWriterIdle()
                            }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    Log.e(TAG, "Send error: " + e.message)
                    ISocketCallback.KageSocketCallbackThreadHandler.instance?.post {
                        socketCallback?.onReadAndWriteError(ISocketCallback.WRITE_ERROR_CODE_CONNECT_UNKNOWN)
                    }
                }
            }
        }
    }
}