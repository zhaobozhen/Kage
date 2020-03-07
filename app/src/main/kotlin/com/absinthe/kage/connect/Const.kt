package com.absinthe.kage.connect

import com.absinthe.kage.connect.protocol.Config

object Const {
    const val LOCAL_IP_IN_AP = "192.168.43.1"
    const val BROADCAST_IP_IN_AP = "192.168.43.255"
    const val BROADCAST_IP_IN_WIFI = "255.255.255.255"
    const val HTTP_SERVER_FORMAT = "http://%s:" + Config.HTTP_SERVER_PORT
}