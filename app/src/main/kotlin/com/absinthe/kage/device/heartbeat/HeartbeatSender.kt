package com.absinthe.kage.device.heartbeat

import com.absinthe.kage.connect.tcp.KageSocket
import java.util.*
import java.util.concurrent.Executors

class HeartbeatSender(private val mSocket: KageSocket) {

    private val mExecutorService = Executors.newCachedThreadPool()
    private val mHeartbeatTaskList: MutableList<HeartbeatTask> = ArrayList()
    private var isInit = false

    fun init() {
        synchronized(HeartbeatSender::class.java) { isInit = true }
    }

    fun beat(heartbeatId: String, timeout: Int, callback: IHeartbeatCallback?) {
        val heartbeatTask = HeartbeatTask(heartbeatId, timeout)

        heartbeatTask.setHeartbeatCallback(object : IHeartbeatCallback {
            override fun onBeatSuccess(heartbeatId: String) {
                synchronized(HeartbeatSender::class.java) { mHeartbeatTaskList.remove(heartbeatTask) }
                callback?.onBeatSuccess(heartbeatId)
            }

            override fun onBeatTimeout(heartbeatId: String) {
                synchronized(HeartbeatSender::class.java) { mHeartbeatTaskList.remove(heartbeatTask) }
                callback?.onBeatTimeout(heartbeatId)
            }

            override fun onBeatCancel(heartbeatId: String) {
                synchronized(HeartbeatSender::class.java) { mHeartbeatTaskList.remove(heartbeatTask) }
                callback?.onBeatCancel(heartbeatId)
            }
        })
        synchronized(HeartbeatSender::class.java) {
            if (isInit) {
                mHeartbeatTaskList.add(heartbeatTask)
                mExecutorService.submit(heartbeatTask)
                return
            }
        }
        callback?.onBeatCancel(heartbeatId)
    }

    fun release() {
        synchronized(HeartbeatSender::class.java) {
            isInit = false
        }
        for (heartbeatTask in mHeartbeatTaskList) {
            heartbeatTask.releaseBeat()
        }
    }

    private inner class HeartbeatTask constructor(private val mId: String, private val mTimeout: Int) : Runnable {

        private var mCallback: IHeartbeatCallback? = null
        private val mHeartbeatRequest: HeartbeatRequest = HeartbeatRequest()

        override fun run() {
            mSocket.send(mHeartbeatRequest)

            when (mHeartbeatRequest.waitResponse(mTimeout)) {
                is ErrorResponse -> {
                    mCallback?.onBeatTimeout(mId)
                }
                is CancelBeatResponse -> {
                    mCallback?.onBeatCancel(mId)
                }
                else -> {
                    mCallback?.onBeatSuccess(mId)
                }
            }
        }

        fun releaseBeat() {
            mHeartbeatRequest.setResponse(CancelBeatResponse())
        }

        fun setHeartbeatCallback(callback: IHeartbeatCallback?) {
            mCallback = callback
        }

        init {
            mHeartbeatRequest.id = mId
        }
    }

    interface IHeartbeatCallback {
        fun onBeatSuccess(heartbeatId: String)
        fun onBeatTimeout(heartbeatId: String)
        fun onBeatCancel(heartbeatId: String)
    }
}