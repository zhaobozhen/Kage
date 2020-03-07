package com.absinthe.kage.device.cmd

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.manager.ActivityStackManager.topActivity

class DeviceRotationCommand : Command() {

    var flag = TYPE_LAND

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(flag.toString())
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (parseReceived(received)) {
            val topActivity = topActivity

            if (topActivity != null) {
                topActivity.requestedOrientation = flag
            }
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            try {
                flag = splits[1].toInt()
                flag = if (flag == Configuration.ORIENTATION_LANDSCAPE) {
                    TYPE_LAND
                } else {
                    TYPE_PORT
                }
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                return false
            }
            true
        } else {
            false
        }
    }

    companion object {
        const val TYPE_LAND = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        const val TYPE_PORT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        const val LENGTH = 2
    }

    init {
        cmd = IpMessageConst.DEVICE_ROTATION
    }
}