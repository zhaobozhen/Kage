package com.absinthe.kage.connect.protocol

interface IProtocolHandler {

    fun handleSocketConnectedEvent()
    fun handleSocketMassage(msg: String)
    fun handleSocketDisConnectEvent()
    fun handleSocketConnectFail(errorCode: Int, e: Exception)
    fun handleSocketSendOrReceiveError()

    /**
     * 为避免多线程安全问题，回调方法都在KageProtocolThreadHandler里执行
     */
    interface IProtocolHandleCallback {
        fun onProtocolConnected()
        fun onProtocolDisConnect()
        fun onProtocolConnectedFailed(errorCode: Int, e: Exception?)
        fun onProtocolSendOrReceiveError()
    }
}