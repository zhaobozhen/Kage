package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer.seekTo

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
            seekTo(position)
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