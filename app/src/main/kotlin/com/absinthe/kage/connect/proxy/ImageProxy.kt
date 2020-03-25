package com.absinthe.kage.connect.proxy

import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Device
import com.absinthe.kage.device.Device.OnReceiveMsgListener
import com.absinthe.kage.device.cmd.ImageInfoCommand
import com.absinthe.kage.device.cmd.MediaPreparePlayCommand
import com.absinthe.kage.device.cmd.StopCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

object ImageProxy : BaseProxy() {

    private var mOnReceiveMsgListener: OnReceiveMsgListener
    private var mOnPlayListener: OnPlayListener? = null
    private var mInquiryPlayStateThread: InquiryPlayStateThread? = null

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
                        GlobalScope.launch(Dispatchers.Main) {
                            mOnPlayListener?.onPlayStateChanged(playOldState, mPlayInfo.playState)
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
                        GlobalScope.launch(Dispatchers.Main) {
                            mOnPlayListener?.onPlayStateChanged(oldState, mPlayInfo.playState)
                        }
                    }
                    mPlayInfo.playState = newState
                }
                IpMessageConst.MEDIA_PLAY_PREVIOUS -> if (mOnPlayListener != null) {
                    GlobalScope.launch(Dispatchers.Main) {
                        mOnPlayListener?.onRemotePreview()
                    }
                }
                IpMessageConst.MEDIA_PLAY_NEXT -> if (mOnPlayListener != null) {
                    GlobalScope.launch(Dispatchers.Main) {
                        mOnPlayListener?.onRemoteNext()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Protocol invalid: %s", e.message)
        }
    }

    fun close() {
        mDevice?.unregisterOnReceiveMsgListener(mOnReceiveMsgListener)
    }

    fun cast(url: String, needStop: Boolean = false) {
        mDevice?.let {
            if (it.isConnected) {
                mPlayInfo.playState = PlayStatue.TRANSITIONING
                it.registerOnReceiveMsgListener(mOnReceiveMsgListener)
                if (needStop) {
                    it.sendCommand(StopCommand())
                }

                it.sendCommand(MediaPreparePlayCommand().apply {
                    type = MediaPreparePlayCommand.TYPE_IMAGE
                })

                it.sendCommand(ImageInfoCommand().apply {
                    info = url
                })

                scheduleInquiryPlayState()
            }
        }
    }

    fun stop() {
        mDevice?.let {
            if (it.isConnected) {
                it.sendCommand(StopCommand())
            }
        }
    }

    fun setOnPlayListener(onPlayListener: OnPlayListener?) {
        mOnPlayListener = onPlayListener
    }

    private fun scheduleInquiryPlayState() {
        cancelInquiryPlayState()
        mInquiryPlayStateThread = InquiryPlayStateThread(mDevice).apply {
            setPeriod(1000)
            start()
        }
    }

    private fun onPlayExit() {}

    private fun cancelInquiryPlayState() {
        mInquiryPlayStateThread?.interrupt()
        mInquiryPlayStateThread = null
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

                try {
                    sleep(period.toLong())
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
        mDevice?.let {
            it.unregisterOnReceiveMsgListener(mOnReceiveMsgListener)
            cancelInquiryPlayState()
            mDevice = device
        }
    }

    override fun onDeviceDisconnected(device: Device) {
        super.onDeviceDisconnected(device)
        val playerState = PlayStatue.DISCONNECT
        val playOldState = mPlayInfo.playState

        onPlayExit()
        mOnPlayListener?.onPlayStateChanged(playOldState, playerState)
        mPlayInfo.playState = playerState
    }
}