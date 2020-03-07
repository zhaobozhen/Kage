package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client

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

    override fun doWork(client: Client, received: String) {}

    override fun parseReceived(received: String): Boolean {
        return false
    }

    companion object {
        const val LENGTH = 3
    }

    init {
        cmd = IpMessageConst.MEDIA_VIDEO_INFO
    }
}