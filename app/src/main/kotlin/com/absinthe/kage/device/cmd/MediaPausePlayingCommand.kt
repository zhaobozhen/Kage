package com.absinthe.kage.device.cmd

import android.media.session.PlaybackState
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.proxy.*
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.AudioPlayer
import com.absinthe.kage.media.video.LocalVideoPlayback
import timber.log.Timber

class MediaPausePlayingCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(PAUSE_MESSAGE)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (BaseProxy.CURRENT_MODE == MODE_AUDIO) {
            Timber.d("PlaybackState.STATE_PLAYING")
            if (AudioPlayer.playState == PlaybackState.STATE_PLAYING) {
                AudioPlayer.pause()
            }
        } else if (BaseProxy.CURRENT_MODE == MODE_VIDEO) {
            LocalVideoPlayback.INSTANCE?.let {
                if (it.state == PlaybackState.STATE_PAUSED)
                it.play()
            }
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