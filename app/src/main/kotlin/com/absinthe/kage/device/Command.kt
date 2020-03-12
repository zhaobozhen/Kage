package com.absinthe.kage.device

import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.client.Client

abstract class Command protected constructor() {

    var cmd = 0

    abstract fun pack(): String
    abstract fun doWork(client: Client, received: String)
    abstract fun parseReceived(received: String): Boolean

    companion object {
        const val DELIMITER = IpMessageProtocol.DELIMITER
    }
}