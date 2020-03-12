package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client

class SetPlayStateCommand : Command() {
    var stateCode = 0

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(stateCode.toString())
                .build()
    }

    override fun doWork(client: Client, received: String) {}

    override fun parseReceived(received: String): Boolean {
        return false
    }

    init {
        cmd = IpMessageConst.MEDIA_SET_PLAYING_STATE
    }
}