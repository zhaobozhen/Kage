package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.proxy.BaseProxy
import com.absinthe.kage.connect.proxy.MODE_AUDIO
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer
import com.absinthe.kage.media.video.LocalVideoPlayback
import java.io.IOException

class InquiryPlayerStatusCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        val command = SetPlayStatusCommand().apply {
            statusCode = if (BaseProxy.CURRENT_MODE == MODE_AUDIO)
                AudioPlayer.playState
            else
                LocalVideoPlayback.INSTANCE?.state ?: 0
        }

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
        cmd = IpMessageConst.MEDIA_GET_PLAYER_STATUS
    }
}