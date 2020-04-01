package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.connect.proxy.AudioProxy
import com.absinthe.kage.connect.proxy.BaseProxy
import com.absinthe.kage.connect.proxy.MODE_AUDIO
import com.absinthe.kage.connect.proxy.MODE_VIDEO
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer
import com.absinthe.kage.media.video.LocalVideoPlayback

class SeekToCommand : Command() {
    var position = 0

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(position.toString())
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (parseReceived(received)) {
            if (BaseProxy.CURRENT_MODE == MODE_AUDIO) {
                AudioPlayer.seekTo(position)
            } else if (BaseProxy.CURRENT_MODE == MODE_VIDEO) {
                LocalVideoPlayback.INSTANCE?.seekTo(position)
            }
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            position = try {
                splits[1].toInt()
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                return false
            }
            true
        } else {
            false
        }
    }

    companion object {
        const val LENGTH = 2
    }

    init {
        cmd = IpMessageConst.MEDIA_SEEK_TO
    }
}