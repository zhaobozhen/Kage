package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.manager.ActivityStackManager
import com.absinthe.kage.ui.media.MusicActivity
import com.absinthe.kage.ui.media.VideoActivity
import com.absinthe.kage.ui.receiver.ReceiverActivity

class MediaPreparePlayCommand : Command() {
    var type = "UNKNOWN"

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(type)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (parseReceived(received)) {
            when(type) {
                TYPE_IMAGE -> {
                    if (ActivityStackManager.topActivity is MusicActivity || ActivityStackManager.topActivity is VideoActivity) {
                        ActivityStackManager.killTopActivity()
                    }
                }
                TYPE_MUSIC -> {
                    if (ActivityStackManager.topActivity is ReceiverActivity || ActivityStackManager.topActivity is VideoActivity) {
                        ActivityStackManager.killTopActivity()
                    }
                }
                TYPE_VIDEO -> {
                    if (ActivityStackManager.topActivity is MusicActivity
                            || ActivityStackManager.topActivity is ReceiverActivity
                            || ActivityStackManager.topActivity is VideoActivity) {
                        ActivityStackManager.killTopActivity()
                    }
                }
            }
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size >= MIN_LENGTH) {
            type = splits[1]
            true
        } else {
            false
        }
    }

    companion object {
        const val TYPE_IMAGE = "IMAGE"
        const val TYPE_VIDEO = "VIDEO"
        const val TYPE_MUSIC = "MUSIC"
        const val MIN_LENGTH = 2
    }

    init {
        cmd = IpMessageConst.MEDIA_PREPARE_PLAY
    }
}