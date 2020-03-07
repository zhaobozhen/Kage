package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client

class PlayNextCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(MESSAGE)
                .build()
    }

    override fun doWork(client: Client, received: String) {}

    override fun parseReceived(received: String): Boolean {
        return false
    }

    companion object {
        const val MESSAGE = "PLAY_NEXT"
    }

    init {
        cmd = IpMessageConst.MEDIA_PLAY_NEXT
    }
}