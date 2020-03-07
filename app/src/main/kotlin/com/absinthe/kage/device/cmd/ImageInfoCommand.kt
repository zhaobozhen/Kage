package com.absinthe.kage.device.cmd

import android.content.Intent
import android.text.TextUtils
import com.absinthe.kage.connect.Const
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.ui.receiver.ReceiverActivity

class ImageInfoCommand : Command() {
    @JvmField
    var info: String

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(info)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (parseReceived(received)) {
            if (!TextUtils.isEmpty(info) && client.deviceInfo != null) {
                var imageUri = info
                val ip = client.deviceInfo!!.ip

                if (!TextUtils.isEmpty(ip)) {
                    imageUri = String.format(Const.HTTP_SERVER_FORMAT, ip) + imageUri

                    val intent = Intent(client.context, ReceiverActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra(ReceiverActivity.EXTRA_IMAGE_URI, imageUri)
                    client.context.startActivity(intent)
                }
            }
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            info = splits[1]
            true
        } else {
            false
        }
    }

    companion object {
        const val LENGTH = 2
    }

    init {
        cmd = IpMessageConst.MEDIA_IMAGE_INFO
        info = ""
    }
}