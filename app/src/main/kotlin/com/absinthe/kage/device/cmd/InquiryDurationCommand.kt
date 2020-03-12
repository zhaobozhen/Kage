package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer
import java.io.IOException

class InquiryDurationCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(INQUIRY_MESSAGE)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        val duration = AudioPlayer.duration
        val command = SetDurationCommand()
        command.duration = duration

        try {
            client.writeToStream(command.pack())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun parseReceived(received: String): Boolean {
        return false
    }

    companion object {
        const val INQUIRY_MESSAGE = "INQUIRY_DURATION"
    }

    init {
        cmd = IpMessageConst.MEDIA_GET_DURATION
    }
}