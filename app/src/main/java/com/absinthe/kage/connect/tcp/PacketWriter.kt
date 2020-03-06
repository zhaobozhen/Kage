package com.absinthe.kage.connect.tcp

import com.absinthe.kage.connect.tcp.KageSocket.ISocketCallback
import java.io.DataOutputStream

class PacketWriter(out: DataOutputStream, socketCallback: ISocketCallback?) : AbstractPacketWriter(out, socketCallback)