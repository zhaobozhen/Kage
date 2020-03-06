package com.absinthe.kage.connect.protocol

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

interface IProtocolHandler {

    fun handleSocketConnectedEvent()
    fun handleSocketMassage(msg: String?)
    fun handleSocketDisConnectEvent()
    fun handleSocketConnectFail(errorCode: Int, e: Exception?)
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

    class KageProtocolThreadHandler(looper: Looper) : Handler(looper) {

        companion object {
            private var mHandlerThread: HandlerThread? = null
            private var mHandler: KageProtocolThreadHandler? = null

            @JvmStatic
            val instance: KageProtocolThreadHandler?
                get() {
                    if (null == mHandler) {
                        synchronized(KageProtocolThreadHandler::class.java) {
                            if (null == mHandler) {
                                if (null == mHandlerThread) {
                                    mHandlerThread = HandlerThread(KageProtocolThreadHandler::class.java.simpleName)
                                    mHandlerThread!!.start()
                                }
                                mHandler = KageProtocolThreadHandler(mHandlerThread!!.looper)
                            }
                        }
                    }
                    return mHandler
                }
        }
    }
}