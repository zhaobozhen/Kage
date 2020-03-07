package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.device.model.DeviceInfo

class PromptPhoneConnectedCommand : Command() {

    var phoneName: String? = null
    var localIp: String? = null

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(phoneName)
                .append(localIp)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (client.deviceInfo == null) {
            client.deviceInfo = DeviceInfo()
        }
        if (parseReceived(received)) {
            client.deviceInfo!!.name = phoneName!!
            client.deviceInfo!!.ip = localIp!!
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            phoneName = splits[1]
            localIp = splits[2]
            true
        } else {
            false
        }
    }

    companion object {
        const val LENGTH = 3
    }

    init {
        cmd = IpMessageConst.PROMPT_PHONE_CONNECT
    }
}