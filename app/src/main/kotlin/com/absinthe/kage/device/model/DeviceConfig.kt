package com.absinthe.kage.device.model

import com.absinthe.kage.connect.Const
import com.absinthe.kage.connect.protocol.Config
import com.absinthe.kage.utils.NetUtils
import java.util.*

class DeviceConfig {
    var name: String = "Unknown"
    var uuid: String = UUID.randomUUID().toString()
    var localHost: String = NetUtils.localAddress
    var broadcastHostInWifi: String = Const.BROADCAST_IP_IN_WIFI
    var broadcastHostInAp: String = Const.BROADCAST_IP_IN_AP
    var broadcastMonitorPort = Config.PORT
    var broadcastPort = Config.PORT
}