package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client

class SetAudioModeCommand : Command() {
    @JvmField
    var mode = MODE_

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(mode.toString())
                .build()
    }

    override fun doWork(client: Client, received: String) {}

    override fun parseReceived(received: String): Boolean {
        return false
    }

    companion object {
        const val MODE_ = 0
    }

    init {
        cmd = IpMessageConst.MEDIA_SET_AUDIO_MODE
    }
}