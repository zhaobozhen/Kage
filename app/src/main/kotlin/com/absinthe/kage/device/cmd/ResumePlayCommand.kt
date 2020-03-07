package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer

class ResumePlayCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        AudioPlayer.play()
    }

    override fun parseReceived(received: String): Boolean {
        return false
    }

    init {
        cmd = IpMessageConst.MEDIA_RESUME_PLAY
    }
}