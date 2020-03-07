package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer.currentPosition
import java.io.IOException

class InquiryPlayingPositionCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(MESSAGE)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        val position = currentPosition
        val command = SetPlayingPositionCommand()
        command.position = position

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
        const val MESSAGE = "INQUIRY_PLAYING_POSITION"
    }

    init {
        cmd = IpMessageConst.MEDIA_GET_PLAYING_POSITION
    }
}