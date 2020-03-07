package com.absinthe.kage.device.cmd

import android.media.session.PlaybackState
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer.pause
import com.absinthe.kage.media.audio.AudioPlayer.playState

class MediaPausePlayingCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(PAUSE_MESSAGE)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (playState == PlaybackState.STATE_PLAYING) {
            pause()
        }
    }

    override fun parseReceived(received: String): Boolean {
        return false
    }

    companion object {
        const val PAUSE_MESSAGE = "PAUSE"
    }

    init {
        cmd = IpMessageConst.MEDIA_PAUSE
    }
}