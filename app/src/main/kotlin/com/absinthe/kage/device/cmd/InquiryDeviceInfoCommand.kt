package com.absinthe.kage.device.cmd

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.DeviceManager.config
import com.absinthe.kage.device.client.Client
import java.io.IOException

class InquiryDeviceInfoCommand : Command() {

    var phoneName: String? = null

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(phoneName)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        try {
            phoneName = config.name
            client.writeToStream(pack())
        } catch (e: IOException) {
            e.printStackTrace()
            client.offline()
        }
    }

    override fun parseReceived(received: String): Boolean {
        return false
    }

    init {
        cmd = IpMessageConst.GET_DEVICE_INFO
    }
}