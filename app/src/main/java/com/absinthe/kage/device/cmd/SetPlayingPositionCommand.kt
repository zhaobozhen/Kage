package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client

class SetPlayingPositionCommand : Command() {
    @JvmField
    var position = 0

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(position.toString())
                .build()
    }

    override fun doWork(client: Client, received: String) {}

    override fun parseReceived(received: String): Boolean {
        return false
    }

    companion object {
        const val LENGTH = 2
    }

    init {
        cmd = IpMessageConst.RESPONSE_SET_PLAYBACK_PROGRESS
    }
}