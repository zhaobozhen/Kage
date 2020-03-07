package com.absinthe.kage.device.model

import com.absinthe.kage.connect.protocol.IpMessageProtocol

class VideoInfo {

    var url: String? = null
    var title: String? = null

    val info: String
        get() = url + IpMessageProtocol.DELIMITER + title
}