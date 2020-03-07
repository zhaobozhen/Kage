package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import java.io.IOException

class HeartbeatCommand : Command() {

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(HEARTBEAT_MESSAGE)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        try {
            client.writeToStream(pack())
        } catch (e: IOException) {
            e.printStackTrace()
            client.offline()
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            try {
                splits[0].toInt() == cmd
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                false
            }
        } else {
            false
        }
    }

    companion object {
        const val HEARTBEAT_MESSAGE = "HEARTBEAT"
        const val LENGTH = 2
    }

    init {
        cmd = IpMessageConst.IS_ONLINE
    }
}