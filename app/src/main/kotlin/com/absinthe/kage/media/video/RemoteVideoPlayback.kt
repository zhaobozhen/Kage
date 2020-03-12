package com.absinthe.kage.media.video

import android.media.session.PlaybackState
import com.absinthe.kage.connect.proxy.VideoProxy
import com.absinthe.kage.device.model.VideoInfo
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.Playback
import timber.log.Timber

class RemoteVideoPlayback : Playback {

    private val mVideoProxy = VideoProxy
    private var mCallback: Playback.Callback? = null

    init {
        mVideoProxy.setOnPlayListener(object : VideoProxy.OnPlayListener {

            override fun onCurrentPositionChanged(duration: Int, position: Int) {}

            override fun onPlayStateChanged(oldState: Int, newState: Int) {
                when {
                    newState == VideoProxy.PlayStatue.INVALIDATE -> {
                        state = PlaybackState.STATE_NONE
                    }
                    newState == VideoProxy.PlayStatue.PLAYER_EXIT -> {
                        state = PlaybackState.STATE_STOPPED
                    }
                    newState != VideoProxy.PlayStatue.DISCONNECT -> {
                        when (newState) {
                            VideoProxy.PlayStatue.STOPPED -> state = PlaybackState.STATE_STOPPED
                            VideoProxy.PlayStatue.TRANSITIONING -> state = PlaybackState.STATE_BUFFERING
                            VideoProxy.PlayStatue.PLAYING -> state = PlaybackState.STATE_PLAYING
                            VideoProxy.PlayStatue.PAUSED -> state = PlaybackState.STATE_PAUSED
                        }
                    }
                    else -> {
                        state = PlaybackState.STATE_STOPPED
                    }
                }
                handlePlayState(false)
            }
        })
    }

    override var state = PlaybackState.STATE_NONE
        private set

    override val duration: Int
        get() = mVideoProxy.duration

    override val bufferPosition: Int
        get() = 100

    override val currentPosition: Int
        get() = mVideoProxy.currentPosition

    override fun playMedia(localMedia: LocalMedia) {
        val mediaInfo = VideoInfo()
        mediaInfo.url = localMedia.url
        mediaInfo.title = localMedia.title
        mVideoProxy.play(mediaInfo)
        Timber.i("playMedia")

        state = PlaybackState.STATE_BUFFERING
        if (mCallback != null) {
            mCallback!!.onMediaMetadataChanged(localMedia)
            mCallback!!.onPlaybackStateChanged(state)
        }
    }

    override fun play() {
        state = PlaybackState.STATE_PLAYING
        handlePlayState(true)
    }

    override fun pause() {
        state = PlaybackState.STATE_PAUSED
        handlePlayState(true)
    }

    override fun seekTo(position: Int) {
        mVideoProxy.seekTo(position)
    }

    override fun setCallback(callback: Playback.Callback) {
        mCallback = callback
    }

    override fun stop(isStop: Boolean) {
        state = PlaybackState.STATE_STOPPED
        if (isStop && mCallback != null) {
            mVideoProxy.stop()
        }
        mVideoProxy.recycle()
    }

    private fun handlePlayState(formUser: Boolean) {
        if (formUser) {
            if (state == PlaybackState.STATE_PLAYING) {
                mVideoProxy.start()
            } else if (state == PlaybackState.STATE_PAUSED) {
                mVideoProxy.pause()
            }
        }
        if (mCallback != null) {
            mCallback!!.onPlaybackStateChanged(state)
        }
    }
}