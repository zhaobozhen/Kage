package com.absinthe.kage.connect.protocol

interface IProtocolHandler {

    fun handleSocketConnectedEvent()
    fun handleSocketMassage(msg: String)
    fun handleSocketDisConnectEvent()
    fun handleSocketConnectFail(errorCode: Int, e: Exception)
    fun handleSocketSendOrReceiveError()

    interface IProtocolHandleCallback {
        fun onProtocolConnected()
        fun onProtocolDisConnect()
        fun onProtocolConnectedFailed(errorCode: Int, e: Exception?)
        fun onProtocolSendOrReceiveError()
    }
}