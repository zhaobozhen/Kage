package com.absinthe.kage.connect.tcp

interface IPacketReader {
    fun addRequest(request: Request)
    fun shutdown()
}