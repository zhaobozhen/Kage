package com.absinthe.kage.device.cmd

import android.content.Intent
import android.text.TextUtils
import com.absinthe.kage.connect.Const
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.TYPE_VIDEO
import com.absinthe.kage.ui.media.VideoActivity

class VideoInfoCommand : Command() {

    @JvmField
    var title: String? = null
    @JvmField
    var url: String? = null

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(title)
                .append(url)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (parseReceived(received)) {
            if (!TextUtils.isEmpty(url) && client.deviceInfo != null) {
                val localMedia = LocalMedia()
                val ip = client.deviceInfo!!.ip

                if (!TextUtils.isEmpty(ip)) {
                    localMedia.filePath = String.format(Const.HTTP_SERVER_FORMAT, ip) + url
                    localMedia.title = title
                    localMedia.type = TYPE_VIDEO

                    val intent = Intent(client.context, VideoActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra(VideoActivity.EXTRA_MEDIA, localMedia)
                    client.context.startActivity(intent)
                }
            }
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            title = splits[1]
            url = splits[2]
            true
        } else {
            false
        }
    }

    companion object {
        const val LENGTH = 3
    }

    init {
        cmd = IpMessageConst.MEDIA_VIDEO_INFO
    }
}