package com.absinthe.kage.connect.protocol

import java.util.*

class IpMessageProtocol {

    var version: String = "1" //Version number
    var packetNum: String //Packet number
    var senderName: String = "Unknown" //Sender name
    var cmd: Int = 0 //Command
    var additionalSection: String? = null

    constructor() {
        packetNum = seconds
    }

    // 根据协议字符串初始化
    constructor(protocolString: String) {
        val args = protocolString.split(DELIMITER).toTypedArray()
        version = args[0]
        packetNum = args[1]
        senderName = args[2]
        cmd = args[3].toInt()
        additionalSection = if (args.size >= 5) args[4] else ""
    }

    constructor(senderName: String, cmd: Int, additionalSection: String = "") {
        packetNum = seconds
        this.senderName = senderName
        this.cmd = cmd
        this.additionalSection = additionalSection
    }

    //得到协议串
    val protocolString: String
        get() = version + DELIMITER +
                packetNum + DELIMITER +
                senderName + DELIMITER +
                cmd + DELIMITER +
                additionalSection

    //得到数据包编号，毫秒数
    private val seconds: String
        get() {
            return Date().time.toString()
        }

    companion object {
        const val DELIMITER = "-->"
    }
}