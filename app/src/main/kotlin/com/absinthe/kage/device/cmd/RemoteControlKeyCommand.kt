package com.absinthe.kage.device.cmd

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client

class RemoteControlKeyCommand : Command() {

    var keyCode = 0

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(keyCode.toString())
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (parseReceived(received)) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    val audioManager = client.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val audioManager = client.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                }
            }
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            keyCode = splits[1].toInt()
            true
        } else {
            false
        }
    }

    init {
        cmd = IpMessageConst.KEY_EVENT
    }

    companion object {
        const val LENGTH = 2
    }
}