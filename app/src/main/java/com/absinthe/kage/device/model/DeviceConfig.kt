package com.absinthe.kage.device.model

import com.absinthe.kage.connect.Const
import com.absinthe.kage.connect.protocol.Config
import com.absinthe.kage.utils.NetUtils
import java.util.*

class DeviceConfig {
    @JvmField
    var name: String = "Unknown"
    @JvmField
    var uuid: String = UUID.randomUUID().toString()
    @JvmField
    var localHost: String = NetUtils.localAddress
    @JvmField
    var broadcastHostInWifi: String = Const.BROADCAST_IP_IN_WIFI
    @JvmField
    var broadcastHostInAp: String = Const.BROADCAST_IP_IN_AP
    @JvmField
    var broadcastMonitorPort = Config.PORT
    @JvmField
    var broadcastPort = Config.PORT
}