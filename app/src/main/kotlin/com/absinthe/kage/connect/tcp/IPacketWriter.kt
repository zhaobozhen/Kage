package com.absinthe.kage.connect.tcp

interface IPacketWriter {
    fun writePacket(packet: Packet)
    fun shutdown()
}