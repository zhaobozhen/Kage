package com.absinthe.kage.device.cmd

import android.content.Intent
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.ui.receiver.ReceiverActivity

class StopCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(STOP)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        val stopIntent = Intent(client.context, ReceiverActivity::class.java)
        stopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        stopIntent.putExtra(ReceiverActivity.EXTRA_IMAGE_URI, ReceiverActivity.EXTRA_FINISH)
        client.context.startActivity(stopIntent)
    }

    override fun parseReceived(received: String): Boolean {
        return false
    }

    companion object {
        private const val STOP = "STOP"
    }

    init {
        cmd = IpMessageConst.MEDIA_STOP
    }
}