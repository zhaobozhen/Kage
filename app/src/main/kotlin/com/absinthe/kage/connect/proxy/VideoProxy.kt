package com.absinthe.kage.connect.proxy

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Device
import com.absinthe.kage.device.Device.OnReceiveMsgListener
import com.absinthe.kage.device.cmd.*
import com.absinthe.kage.device.model.VideoInfo

object VideoProxy : BaseProxy() {

    private var mInquiryCurrentPositionThread: InquiryCurrentPositionThread? = null
    private var mInquiryPlayStateThread: InquiryPlayStateThread? = null
    private var mOnPlayListener: OnPlayListener? = null
    private var mPlayPositionInquiryPeriod = 1000

    private val TAG = VideoProxy::class.java.simpleName
    private val mOnReceiveMsgListener: OnReceiveMsgListener
    private val mPlayInfo: PlayInfo = PlayInfo()
    private val mHandler = Handler(Looper.getMainLooper())

    init {
        mOnReceiveMsgListener = object : OnReceiveMsgListener {
            override fun onReceiveMsg(msg: String) {
                parserMsgAndNotifyIfNeed(msg)
            }
        }
    }

    fun play(videoInfo: VideoInfo) {
        if (videoInfo.url != null && mDevice != null && mDevice!!.isConnected) {
            mDevice!!.unregisterOnReceiveMsgListener(mOnReceiveMsgListener)
            cancelInquiryPlayState()
            cancelInquiryCurrentPosition()
            resetCurrentPlayInfo()
            mDevice!!.registerOnReceiveMsgListener(mOnReceiveMsgListener)

            val stopCmd = StopCommand()
            mDevice!!.sendCommand(stopCmd)

            val preparePlayCmd = MediaPreparePlayCommand()
            preparePlayCmd.type = MediaPreparePlayCommand.TYPE_VIDEO
            mDevice!!.sendCommand(preparePlayCmd)

            val videoInfoCommand = VideoInfoCommand()
            videoInfoCommand.title = videoInfo.title
            videoInfoCommand.url = videoInfo.url
            mDevice!!.sendCommand(videoInfoCommand)

            scheduleInquiryPlayState(1000)
        }
    }

    fun start() {
        if (mPlayInfo.currentPlayState == PLAY_STATUS.PAUSED_PLAYBACK.status && null != mDevice && mDevice!!.isConnected) {
            val videoInfoCommand = VideoInfoCommand()
            mDevice!!.sendCommand(videoInfoCommand)
        }
    }

    fun pause() {
        if (mPlayInfo.currentPlayState == PLAY_STATUS.PLAYING.status && null != mDevice && mDevice!!.isConnected) {
            val pauseCmd = MediaPausePlayingCommand()
            mDevice!!.sendCommand(pauseCmd)
        }
    }

    fun stop() {
        if (null != mDevice && mDevice!!.isConnected) {
            val stopCmd = StopCommand()
            mDevice!!.sendCommand(stopCmd)
        }
    }

    fun seekTo(position: Int) {
        if (null != mDevice && mDevice!!.isConnected) {
            val seekToCmd = SeekToCommand()
            seekToCmd.position = position
            mDevice!!.sendCommand(seekToCmd)
            mPlayInfo.position = position
        }
    }

    fun setOnPlayListener(onPlayListener: OnPlayListener?) {
        mOnPlayListener = onPlayListener
    }

    val playState: Int
        get() {
            return mPlayInfo.currentPlayState
        }

    val duration: Int
        get() = mPlayInfo.duration

    val currentPosition: Int
        get() = mPlayInfo.position

    private fun recycle() {
        if (mDevice != null) {
            mDevice!!.unregisterOnReceiveMsgListener(mOnReceiveMsgListener)
        }
        cancelInquiryCurrentPosition()
        cancelInquiryPlayState()
    }

    private fun parserMsgAndNotifyIfNeed(msg: String?) {
        if (msg != null) {
            val split = msg.split(IpMessageProtocol.DELIMITER).toTypedArray()
            if (split.size < 2) {
                return
            }
            try {
                when (split[0].toInt()) {
                    IpMessageConst.RESPONSE_SET_PLAYBACK_PROGRESS -> {
                        val position = split[1].toInt()
                        if (isPlayerWorking) {
                            mPlayInfo.position = position
                        } else {
                            recycle()
                        }
                        notifyOnCurrentPositionChanged(mPlayInfo.duration, mPlayInfo.position)
                    }
                    IpMessageConst.RESPONSE_SET_MEDIA_DURATION -> {
                        val duration = split[1].toInt()
                        if (isPlayerWorking) {
                            mPlayInfo.duration = duration
                        } else {
                            recycle()
                        }
                        notifyOnCurrentPositionChanged(mPlayInfo.duration, mPlayInfo.position)
                        scheduleInquiryCurrentPosition()
                    }
                    IpMessageConst.MEDIA_SET_PLAYER_STATUS -> {
                        val playerState: Int = PLAYER_STATUS.valueOf(split[1]).status
                        val playOldState = mPlayInfo.currentPlayState
                        if (PlayStatue.PLAYER_EXIT == playerState) {
                            Log.i(TAG, "Receive Exit")
                            onPlayStopped()
                        }
                        notifyOnPlayStateChanged(playOldState, playerState)
                    }
                    IpMessageConst.MEDIA_SET_PLAYING_STATE -> {
                        val state = split[1]
                        val newState: Int = PLAY_STATUS.valueOf(state).status
                        val oldState = mPlayInfo.currentPlayState
                        Log.i(TAG, "newState:$newState-oldState:$oldState")
                        if (oldState == newState) {
                            return
                        }
                        if (PlayStatue.PLAYING == newState) {
                            inquiryDuration()
                        } else {
                            cancelInquiryCurrentPosition()
                        }
                        if (PlayStatue.STOPPED == newState) {
                            Log.i(TAG, "Receive STOP")
                            onPlayStopped()
                        }
                        mPlayInfo.currentPlayState = newState
                        notifyOnPlayStateChanged(oldState, newState)
                    }
                    else -> {
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "protocol invalid:" + e.message)
            }
        }
    }

    private val isPlayerWorking: Boolean
        get() = (mPlayInfo.currentPlayState == PlayStatue.PLAYING || mPlayInfo.currentPlayState == PlayStatue.PAUSED)

    private fun onPlayStopped() {
        resetCurrentPlayInfo()
        cancelInquiryPlayState()
    }

    private fun notifyOnCurrentPositionChanged(duration: Int, position: Int) {
        mHandler.post {
            if (mOnPlayListener != null) {
                mOnPlayListener!!.onCurrentPositionChanged(duration, position)
            }
        }
    }

    private fun notifyOnPlayStateChanged(oldState: Int, newState: Int) {
        mHandler.post {
            if (mOnPlayListener != null) {
                mOnPlayListener!!.onPlayStateChanged(oldState, newState)
            }
        }
    }

    private fun resetCurrentPlayInfo() {
        mPlayInfo.duration = 0
        mPlayInfo.position = 0
        mPlayInfo.currentPlayState = PlayStatue.STOPPED
    }

    private fun scheduleInquiryPlayState(period: Int) {
        cancelInquiryPlayState()
        mInquiryPlayStateThread = InquiryPlayStateThread(mDevice)
        mInquiryPlayStateThread!!.setPeriod(period)
        mInquiryPlayStateThread!!.start()
    }

    private fun cancelInquiryPlayState() {
        if (mInquiryPlayStateThread != null) {
            mInquiryPlayStateThread!!.interrupt()
            mInquiryPlayStateThread = null
        }
    }

    private fun inquiryDuration() {
        val inquiryDurationCmd = InquiryDurationCommand()
        mDevice!!.sendCommand(inquiryDurationCmd)
    }

    private fun scheduleInquiryCurrentPosition() {
        cancelInquiryCurrentPosition()
        val updatePeriod = 1000
        if (mPlayPositionInquiryPeriod < updatePeriod) {
            mPlayPositionInquiryPeriod = updatePeriod
        }
        mInquiryCurrentPositionThread = InquiryCurrentPositionThread(mDevice, updatePeriod, mPlayPositionInquiryPeriod, mPlayInfo)
        mInquiryCurrentPositionThread!!.start()
    }

    private fun cancelInquiryCurrentPosition() {
        if (mInquiryCurrentPositionThread != null) {
            mInquiryCurrentPositionThread!!.interrupt()
            mInquiryCurrentPositionThread = null
        }
    }

    private class InquiryCurrentPositionThread(private val mDevice: Device?, updatePeriod: Int, inquiryPeriod: Int, playInfo: PlayInfo?) : Thread() {
        private var mUpdatePeriod = 2000
        private var mInquiryPeriod = 4000
        private var mNoInquiryMills = 0
        private val mPlayInfo: PlayInfo?
        private val inquiryCurrentPositionCmd = InquiryPlayingPositionCommand()
        override fun run() {
            while (true) {
                if (isInterrupted) {
                    break
                }
                if (mDevice == null || !mDevice.isConnected) {
                    break
                }
                if (mPlayInfo == null) {
                    break
                }
                val updatePeriodMillis = mUpdatePeriod
                val inquiryPeriodMillis = mInquiryPeriod
                if (mNoInquiryMills >= inquiryPeriodMillis) {
                    //do inquiry remote
                    mDevice.sendCommand(inquiryCurrentPositionCmd)
                    mNoInquiryMills = 0
                } else {
                    //simulate update
                    if (mPlayInfo.position + updatePeriodMillis >= mPlayInfo.duration) {
                        mPlayInfo.position = mPlayInfo.duration
                    } else {
                        mPlayInfo.position += updatePeriodMillis
                    }
                    notifyOnCurrentPositionChanged(mPlayInfo.duration, mPlayInfo.position)
                }
                mNoInquiryMills += updatePeriodMillis
                try {
                    sleep(updatePeriodMillis.toLong())
                } catch (e: InterruptedException) {
                    Log.e(TAG, "InquiryCurrentPositionThread is interrupted")
                    break
                }
            }
        }

        init {
            mUpdatePeriod = updatePeriod
            mInquiryPeriod = inquiryPeriod
            mPlayInfo = playInfo
            mNoInquiryMills = inquiryPeriod
        }
    }

    private class InquiryPlayStateThread(private val mDevice: Device?) : Thread() {
        private var period = 2000
        override fun run() {
            while (true) {
                if (isInterrupted) {
                    break
                }
                if (mDevice == null || !mDevice.isConnected) {
                    break
                }
                val inquiryPlayStateCmd = InquiryPlayStateCommand()
                mDevice.sendCommand(inquiryPlayStateCmd)
                val inquiryPlayerStatusCmd = InquiryPlayerStatusCommand()
                mDevice.sendCommand(inquiryPlayerStatusCmd)
                val inquiryPeriod = period.toLong()
                try {
                    sleep(inquiryPeriod)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "InquiryCurrentPositionThread is interrupted")
                    break
                }
            }
        }

        fun setPeriod(period: Int) {
            this.period = period
        }

    }

    interface OnPlayListener {
        fun onCurrentPositionChanged(duration: Int, position: Int)
        fun onPlayStateChanged(oldState: Int, newState: Int)
    }

    internal class PlayInfo {
        var duration = 0
        var position = 0
        var currentPlayState = 0
    }

    interface PlayStatue {
        companion object {
            const val INVALIDATE = -1
            const val STOPPED = 1
            const val TRANSITIONING = 2
            const val PLAYING = 3
            const val PAUSED = 4
            const val OK = 10
            const val PLAYER_EXIT = 11
            const val ERROR_OCCURRED = 12
            const val DISCONNECT = 20
        }
    }

    //  播放器状态
    private enum class PLAYER_STATUS(val status: Int) {
        OK(PlayStatue.OK),
        PLAYER_EXIT(PlayStatue.PLAYER_EXIT),
        ERROR_OCCURRED(PlayStatue.ERROR_OCCURRED);
    }

    //播放状态
    private enum class PLAY_STATUS(val status: Int) {
        STOPPED(PlayStatue.STOPPED),
        PLAYING(PlayStatue.PLAYING),
        PAUSED_PLAYBACK(PlayStatue.PAUSED),
        TRANSITIONING(PlayStatue.TRANSITIONING);
    }

    override fun onDeviceConnected(device: Device) {
        if (mDevice != device) {
            if (mDevice != null) {
                mDevice!!.unregisterOnReceiveMsgListener(mOnReceiveMsgListener)
                cancelInquiryPlayState()
                cancelInquiryCurrentPosition()
            }
            mDevice = device
        }
    }

    override fun onDeviceDisconnected(device: Device) {
        super.onDeviceDisconnected(device)
        val playerState = PlayStatue.DISCONNECT
        val playOldState = mPlayInfo.currentPlayState
        onPlayStopped()
        notifyOnPlayStateChanged(playOldState, playerState)
    }
}