package com.absinthe.kage.device.model

import com.absinthe.kage.connect.protocol.Config

class DeviceInfo {

    var name: String = "Unknown"
    var ip: String = Config.ADDRESS
    var protocolVersion: String? = null
    var functionCode: String? = null
    var isConnected = false
    var state = STATE_IDLE

    fun setStateConnecting() {
        state = STATE_CONNECTING
    }

    override fun toString(): String {
        return "DeviceInfo: Name = $name, IP = $ip"
    }

    companion object {
        const val STATE_IDLE = 0 //未连接
        const val STATE_CONNECTING = 1 //连接中
        const val STATE_CONNECTED = 2 //已连接
    }
}