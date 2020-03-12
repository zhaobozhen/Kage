package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client

class SetDurationCommand : Command() {
    var duration = 0

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(duration.toString())
                .build()
    }

    override fun doWork(client: Client, received: String) {}

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            try {
                duration = splits[1].toInt()
                true
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                false
            }
        } else {
            false
        }
    }

    companion object {
        const val LENGTH = 2
    }

    init {
        cmd = IpMessageConst.RESPONSE_SET_MEDIA_DURATION
    }
}