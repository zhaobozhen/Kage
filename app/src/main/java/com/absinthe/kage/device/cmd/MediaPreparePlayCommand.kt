package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client

class MediaPreparePlayCommand : Command() {
    @JvmField
    var type = "UNKNOWN"

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(type)
                .build()
    }

    override fun doWork(client: Client, received: String) {}
    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size >= MIN_LENGTH) {
            type = splits[1]
            true
        } else {
            false
        }
    }

    companion object {
        const val TYPE_IMAGE = "IMAGE"
        const val TYPE_VIDEO = "VIDEO"
        const val TYPE_MUSIC = "MUSIC"
        const val MIN_LENGTH = 2
    }

    init {
        cmd = IpMessageConst.MEDIA_PREPARE_PLAY
    }
}