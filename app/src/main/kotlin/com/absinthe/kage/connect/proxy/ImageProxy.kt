package com.absinthe.kage.connect.proxy

import android.os.Handler
import android.os.Looper
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Device
import com.absinthe.kage.device.Device.OnReceiveMsgListener
import com.absinthe.kage.device.cmd.ImageInfoCommand
import com.absinthe.kage.device.cmd.MediaPreparePlayCommand
import com.absinthe.kage.device.cmd.StopCommand
import timber.log.Timber

object ImageProxy : BaseProxy() {

    private var mOnReceiveMsgListener: OnReceiveMsgListener
    private var mOnPlayListener: OnPlayListener? = null
    private var mInquiryPlayStateThread: InquiryPlayStateThread? = null

    private val mHandler = Handler(Looper.getMainLooper())
    private val mPlayInfo = PlayInfo()

    init {
        mOnReceiveMsgListener = object : OnReceiveMsgListener {
            override fun onReceiveMsg(msg: String) {
                parserMsgAndNotifyIfNeed(msg)
            }
        }
    }

    private fun parserMsgAndNotifyIfNeed(msg: String) {
        Timber.d("msg = $msg")

        val split = msg.split(IpMessageProtocol.DELIMITER).toTypedArray()
        if (split.size < 2) {
            return
        }
        try {
            when (split[0].toInt()) {
                IpMessageConst.MEDIA_SET_PLAYER_STATUS -> {
                    val playerState: Int = PlayerStatus.valueOf(split[1]).status
                    val playOldState = mPlayInfo.playState
                    if (playerState == playOldState) {
                        return
                    }
                    if (PlayStatue.PLAYER_EXIT == playerState) {
                        onPlayExit()
                    }
                    mPlayInfo.playState = playerState
                    if (mOnPlayListener != null) {
                        mHandler.post {
                            if (mOnPlayListener != null) {
                                mOnPlayListener!!.onPlayStateChanged(playOldState, mPlayInfo.playState)
                            }
                        }
                    }
                }
                IpMessageConst.MEDIA_SET_PLAYING_STATE -> {
                    val newState: Int = PlayingStatus.valueOf(split[1]).status
                    val oldState = mPlayInfo.playState
                    if (oldState == newState) {
                        return
                    }
                    if (PlayStatue.STOPPED == newState) {
                        onPlayExit()
                        mPlayInfo.playState = PlayStatue.PLAYER_EXIT
                    } else {
                        mPlayInfo.playState = newState
                    }
                    if (mOnPlayListener != null) {
                        mHandler.post {
                            if (mOnPlayListener != null) {
                                mOnPlayListener!!.onPlayStateChanged(oldState, mPlayInfo.playState)
                            }
                        }
                    }
                    mPlayInfo.playState = newState
                }
                IpMessageConst.MEDIA_PLAY_PREVIOUS -> if (mOnPlayListener != null) {
                    mHandler.post {
                        if (mOnPlayListener != null) {
                            mOnPlayListener!!.onRemotePreview()
                        }
                    }
                }
                IpMessageConst.MEDIA_PLAY_NEXT -> if (mOnPlayListener != null) {
                    mHandler.post {
                        if (mOnPlayListener != null) {
                            mOnPlayListener!!.onRemoteNext()
                        }
                    }
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            Timber.e("Protocol invalid: " + e.message)
        }
    }

    private fun onPlayExit() {}
    fun close() {
        if (mDevice != null) {
            mDevice!!.unregisterOnReceiveMsgListener(mOnReceiveMsgListener)
        }
    }

    fun cast(url: String, needStop: Boolean = false) {
        if (mDevice != null && mDevice!!.isConnected) {
            mPlayInfo.playState = PlayStatue.TRANSITIONING
            mDevice!!.registerOnReceiveMsgListener(mOnReceiveMsgListener)
            if (needStop) {
                val stopCmd = StopCommand()
                mDevice!!.sendCommand(stopCmd)
            }
            val preparePlayCommand = MediaPreparePlayCommand()
            preparePlayCommand.type = MediaPreparePlayCommand.TYPE_IMAGE
            mDevice!!.sendCommand(preparePlayCommand)
            val imageInfoCommand = ImageInfoCommand()
            imageInfoCommand.info = url
            mDevice!!.sendCommand(imageInfoCommand)
            scheduleInquiryPlayState()
        }
    }

    fun stop() {
        if (null != mDevice && mDevice!!.isConnected) {
            val stopCmd = StopCommand()
            mDevice!!.sendCommand(stopCmd)
        }
    }

    fun setOnPlayListener(onPlayListener: OnPlayListener?) {
        mOnPlayListener = onPlayListener
    }

    private fun scheduleInquiryPlayState() {
        cancelInquiryPlayState()
        mInquiryPlayStateThread = InquiryPlayStateThread(mDevice)
        mInquiryPlayStateThread!!.setPeriod(1000)
        mInquiryPlayStateThread!!.start()
    }

    private fun cancelInquiryPlayState() {
        if (mInquiryPlayStateThread != null) {
            mInquiryPlayStateThread!!.interrupt()
            mInquiryPlayStateThread = null
        }
    }

    interface OnPlayListener {
        fun onPlayStateChanged(playOldState: Int, playerState: Int)
        fun onRemoteNext()
        fun onRemotePreview()
    }

    private class PlayInfo {
        var direction = 0
        var scale = 0f
        var centerPoint = FloatArray(2)
        var playState = 0
    }

    interface PlayStatue {
        companion object {
            const val INVALIDATE = -1
            const val STOPPED = 1
            const val TRANSITIONING = 2
            const val PLAYING = 3
            const val OK = 10
            const val PLAYER_EXIT = 11
            const val ERROR_OCCURRED = 12
            const val DISCONNECT = 20
        }
    }

    //  播放器状态
    private enum class PlayerStatus(val status: Int) {
        OK(PlayStatue.OK),
        PLAYER_EXIT(PlayStatue.PLAYER_EXIT),
        ERROR_OCCURRED(PlayStatue.ERROR_OCCURRED);
    }

    private enum class PlayingStatus(val status: Int) {
        STOPPED(PlayStatue.STOPPED),
        PLAYING(PlayStatue.PLAYING),
        TRANSITIONING(PlayStatue.TRANSITIONING);
    }

    private class InquiryPlayStateThread internal constructor(private val mDevice: Device?) : Thread() {
        private var period = 2000
        override fun run() {
            while (true) {
                if (isInterrupted) {
                    break
                }
                if (mDevice == null || !mDevice.isConnected) {
                    break
                }
                val inquiryPeriod = period.toLong()
                try {
                    sleep(inquiryPeriod)
                } catch (e: InterruptedException) {
                    Timber.e("InquiryCurrentPositionThread is interrupted")
                    break
                }
            }
        }

        fun setPeriod(period: Int) {
            this.period = period
        }

    }

    override fun onDeviceConnected(device: Device) {
        if (mDevice != device) {
            if (mDevice != null) {
                mDevice!!.unregisterOnReceiveMsgListener(mOnReceiveMsgListener)
                cancelInquiryPlayState()
            }
            mDevice = device
        }
    }

    override fun onDeviceDisconnected(device: Device) {
        super.onDeviceDisconnected(device)
        val playerState = PlayStatue.DISCONNECT
        val playOldState = mPlayInfo.playState
        onPlayExit()
        if (mOnPlayListener != null) {
            mOnPlayListener!!.onPlayStateChanged(playOldState, playerState)
        }
        mPlayInfo.playState = playerState
    }
}