package com.absinthe.kage.connect.proxy

import android.media.session.PlaybackState
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Device
import com.absinthe.kage.device.Device.OnReceiveMsgListener
import com.absinthe.kage.device.cmd.*
import com.absinthe.kage.device.model.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

object VideoProxy : BaseProxy() {

    private var mInquiryCurrentPositionThread: InquiryCurrentPositionThread? = null
    private var mInquiryPlayStateThread: InquiryPlayStateThread? = null
    private var mOnPlayListener: OnPlayListener? = null
    private var mPlayPositionInquiryPeriod = 1000

    private val mOnReceiveMsgListener: OnReceiveMsgListener
    private val mPlayInfo: PlayInfo = PlayInfo()

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

            val preparePlayCmd = MediaPreparePlayCommand().apply {
                type = MediaPreparePlayCommand.TYPE_VIDEO
            }
            mDevice!!.sendCommand(preparePlayCmd)

            val videoInfoCommand = VideoInfoCommand().apply {
                title = videoInfo.title
                url = videoInfo.url
            }
            mDevice!!.sendCommand(videoInfoCommand)

            scheduleInquiryPlayState()
        }
    }

    fun start() {
        if (mPlayInfo.currentPlayState == PlayingStatus.PAUSED_PLAYBACK.status && mDevice!!.isConnected) {
            val videoInfoCommand = VideoInfoCommand()
            mDevice!!.sendCommand(videoInfoCommand)
        }
    }

    fun pause() {
        if (mPlayInfo.currentPlayState == PlayingStatus.PLAYING.status && mDevice!!.isConnected) {
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
            val seekToCmd = SeekToCommand().apply {
                this.position = position
            }
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

    fun recycle() {
        if (mDevice != null) {
            mDevice!!.unregisterOnReceiveMsgListener(mOnReceiveMsgListener)
        }
        cancelInquiryCurrentPosition()
        cancelInquiryPlayState()
    }

    private fun parserMsgAndNotifyIfNeed(msg: String) {
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
                    val playerState: Int = PlayerStatus.valueOf(split[1]).status
                    val playOldState = mPlayInfo.currentPlayState

                    if (PlayStatue.PLAYER_EXIT == playerState) {
                        Timber.i("Receive Exit")
                        onPlayStopped()
                    }
                    notifyOnPlayStateChanged(playOldState, playerState)
                }
                IpMessageConst.MEDIA_SET_PLAYING_STATE -> {
                    val state = split[1]
                    val newState: Int = PlayerStatus.valueOf(state).status
                    val oldState = mPlayInfo.currentPlayState
                    Timber.i("newState:$newState-oldState:$oldState")

                    if (oldState == newState) {
                        return
                    }
                    if (PlayStatue.PLAYING == newState) {
                        inquiryDuration()
                    } else {
                        cancelInquiryCurrentPosition()
                    }
                    if (PlayStatue.STOPPED == newState) {
                        Timber.i("Receive STOP")
                        onPlayStopped()
                    }
                    mPlayInfo.currentPlayState = newState
                    notifyOnPlayStateChanged(oldState, newState)
                }
                else -> {
                }
            }
        } catch (e: Exception) {
            Timber.e("protocol invalid:%s", e.message)
        }
    }

    private val isPlayerWorking: Boolean
        get() = (mPlayInfo.currentPlayState == PlayStatue.PLAYING || mPlayInfo.currentPlayState == PlayStatue.PAUSED)

    private fun onPlayStopped() {
        resetCurrentPlayInfo()
        cancelInquiryPlayState()
    }

    private fun notifyOnCurrentPositionChanged(duration: Int, position: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            mOnPlayListener?.onCurrentPositionChanged(duration, position)
        }
    }

    private fun notifyOnPlayStateChanged(oldState: Int, newState: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            mOnPlayListener?.onPlayStateChanged(oldState, newState)
        }
    }

    private fun resetCurrentPlayInfo() {
        mPlayInfo.apply {
            duration = 0
            position = 0
            currentPlayState = PlayStatue.STOPPED
        }
    }

    private fun scheduleInquiryPlayState() {
        cancelInquiryPlayState()
        mInquiryPlayStateThread = InquiryPlayStateThread(mDevice).apply {
            setPeriod(1000)
            start()
        }
    }

    private fun cancelInquiryPlayState() {
        mInquiryPlayStateThread?.interrupt()
        mInquiryPlayStateThread = null
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
        mInquiryCurrentPositionThread = InquiryCurrentPositionThread(mDevice, updatePeriod, mPlayPositionInquiryPeriod, mPlayInfo).apply {
            start()
        }
    }

    private fun cancelInquiryCurrentPosition() {
        mInquiryCurrentPositionThread?.interrupt()
        mInquiryCurrentPositionThread = null
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
                    Timber.e("InquiryCurrentPositionThread is interrupted")
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
            const val STOPPED = PlaybackState.STATE_STOPPED
            const val TRANSITIONING = PlaybackState.STATE_BUFFERING
            const val PLAYING = PlaybackState.STATE_PLAYING
            const val PAUSED = PlaybackState.STATE_PAUSED
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

    //播放状态
    private enum class PlayingStatus(val status: Int) {
        STOPPED(PlayStatue.STOPPED),
        PLAYING(PlayStatue.PLAYING),
        PAUSED_PLAYBACK(PlayStatue.PAUSED),
        TRANSITIONING(PlayStatue.TRANSITIONING);
    }

    override fun onDeviceConnected(device: Device) {
        if (mDevice != device) {
            mDevice?.unregisterOnReceiveMsgListener(mOnReceiveMsgListener)
            cancelInquiryPlayState()
            cancelInquiryCurrentPosition()
            mDevice = device
        }
    }

    override fun onDeviceDisconnected(device: Device) {
        super.onDeviceDisconnected(device)
        onPlayStopped()
        notifyOnPlayStateChanged(mPlayInfo.currentPlayState, PlayStatue.DISCONNECT)
    }
}