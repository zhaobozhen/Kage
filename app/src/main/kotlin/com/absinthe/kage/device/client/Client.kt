package com.absinthe.kage.device.client

import android.content.Context
import android.util.Log
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.cmd.*
import com.absinthe.kage.device.model.DeviceInfo
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.nio.charset.StandardCharsets

class Client(val context: Context,
             private val mSocket: Socket,
             private val dis: DataInputStream,
             private val dos: DataOutputStream) : Thread(), Runnable {

    var deviceInfo: DeviceInfo? = null

    override fun run() {
        while (true) {
            val command = readToStream(dis)
            Log.i(TAG, "Received command: $command")

            if (command == null) {
                offline(mSocket, dis, dos)
                break
            }
            if (command.isEmpty()) {
                continue
            }

            var commandNum = 0
            try {
                commandNum = command.split(IpMessageProtocol.DELIMITER).toTypedArray()[0].toInt()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
            }

            when (commandNum) {
                IpMessageConst.IS_ONLINE -> HeartbeatCommand().doWork(this, command)
                IpMessageConst.GET_DEVICE_INFO -> InquiryDeviceInfoCommand().doWork(this, command)
                IpMessageConst.PROMPT_PHONE_CONNECT -> PromptPhoneConnectedCommand().doWork(this, command)
                IpMessageConst.MEDIA_STOP -> StopCommand().doWork(this, command)
                IpMessageConst.MEDIA_PREPARE_PLAY -> MediaPreparePlayCommand().doWork(this, command)
                IpMessageConst.MEDIA_IMAGE_INFO -> ImageInfoCommand().doWork(this, command)
                IpMessageConst.DEVICE_ROTATION -> DeviceRotationCommand().doWork(this, command)
                IpMessageConst.MEDIA_AUDIO_INFO -> AudioInfoCommand().doWork(this, command)
                IpMessageConst.MEDIA_PAUSE -> MediaPausePlayingCommand().doWork(this, command)
                IpMessageConst.MEDIA_GET_PLAYER_STATUS -> InquiryPlayerStatusCommand().doWork(this, command)
                IpMessageConst.MEDIA_GET_PLAYING_STATE -> InquiryPlayStateCommand().doWork(this, command)
                IpMessageConst.MEDIA_PLAY_AUDIO_LIST -> PlayAudioListCommand().doWork(this, command)
                IpMessageConst.MEDIA_GET_DURATION -> InquiryDurationCommand().doWork(this, command)
                IpMessageConst.MEDIA_GET_PLAYING_POSITION -> InquiryPlayingPositionCommand().doWork(this, command)
                IpMessageConst.MEDIA_RESUME_PLAY -> ResumePlayCommand().doWork(this, command)
                IpMessageConst.MEDIA_SEEK_TO -> SeekToCommand().doWork(this, command)
                IpMessageConst.MEDIA_VIDEO_INFO -> VideoInfoCommand().doWork(this, command)
            }
        }
    }

    @Synchronized
    @Throws(IOException::class)
    fun writeToStream(str: String) {
        writeToStream(dos, str)
    }

    fun offline() {
        offline(mSocket, dis, dos)
    }

    private fun readToStream(dis: DataInputStream): String? {
        val receivedLen: Int
        receivedLen = try {
            dis.readInt()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        if (receivedLen <= 0) {
            return ""
        }

        val bArray: ByteArray
        bArray = try {
            ByteArray(receivedLen)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            return ""
        }

        var bytesRead = 0
        while (bytesRead < receivedLen) {
            bytesRead += try {
                val result = dis.read(bArray, bytesRead, receivedLen - bytesRead)
                if (result == -1) break
                result
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }

        val enStr: String
        enStr = try {
            String(bArray, StandardCharsets.UTF_8)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            return ""
        }

        return enStr
    }

    @Synchronized
    @Throws(IOException::class)
    private fun writeToStream(dos: DataOutputStream, str: String) {
        Log.i(TAG, "writeToStream sendStr: $str")

        val bArray = str.toByteArray(StandardCharsets.UTF_8)
        val sendLen = bArray.size
        dos.writeInt(sendLen)
        dos.flush()
        dos.write(bArray, 0, sendLen)
    }

    private fun offline(socket: Socket, dis: DataInputStream?, dos: DataOutputStream?) {
        Log.i(TAG, "Offline: " + socket.inetAddress)

        synchronized(Client::class.java) {
            try {
                dis?.close()
                dos?.close()
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private val TAG = Client::class.java.simpleName
    }

}