package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer.playState
import java.io.IOException

class InquiryPlayStateCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        val command = SetPlayStateCommand()
        command.stateCode = playState

        try {
            client.writeToStream(command.pack())
        } catch (e: IOException) {
            e.printStackTrace()
            client.offline()
        }
    }

    override fun parseReceived(received: String): Boolean {
        return true
    }

    init {
        cmd = IpMessageConst.MEDIA_GET_PLAYING_STATE
    }
}