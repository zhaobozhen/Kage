package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.proxy.BaseProxy
import com.absinthe.kage.connect.proxy.MODE_AUDIO
import com.absinthe.kage.connect.proxy.MODE_VIDEO
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer
import com.absinthe.kage.media.video.LocalVideoPlayback
import timber.log.Timber

class ResumePlayCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (BaseProxy.CURRENT_MODE == MODE_AUDIO) {
            AudioPlayer.play()
        } else if (BaseProxy.CURRENT_MODE == MODE_VIDEO) {
            LocalVideoPlayback.INSTANCE?.play()
        }
    }

    override fun parseReceived(received: String): Boolean {
        return false
    }

    init {
        cmd = IpMessageConst.MEDIA_RESUME_PLAY
    }
}