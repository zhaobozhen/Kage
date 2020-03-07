package com.absinthe.kage.device.heartbeat

import com.absinthe.kage.connect.tcp.Request
import com.absinthe.kage.device.cmd.HeartbeatCommand

class HeartbeatRequest : Request() {

    init {
        data = HeartbeatCommand().pack()
    }

}