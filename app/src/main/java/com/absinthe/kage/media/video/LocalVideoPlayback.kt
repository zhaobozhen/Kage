package com.absinthe.kage.media.video

import android.media.session.PlaybackState
import android.util.Log
import android.view.View
import android.widget.VideoView
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.Playback

class LocalVideoPlayback(private val mVideoView: VideoView) : Playback {

    private var mCallback: Playback.Callback? = null

    init {
        mVideoView.setOnPreparedListener { play() }
        mVideoView.setOnCompletionListener {
            Log.i(TAG, "complete")
            state = PlaybackState.STATE_PAUSED
            if (mCallback != null) {
                mCallback!!.onCompletion()
            }
        }
    }

    override var state = PlaybackState.STATE_NONE
        private set

    override val duration: Int
        get() = mVideoView.duration

    override val bufferPosition: Int
        get() = mVideoView.bufferPercentage

    override val currentPosition: Int
        get() = mVideoView.currentPosition

    override fun playMedia(localMedia: LocalMedia) {
        state = PlaybackState.STATE_BUFFERING

        if (mVideoView.visibility == View.VISIBLE) {
            mVideoView.setVideoPath(localMedia.filePath)
        }
        if (mCallback != null) {
            mCallback!!.onMediaMetadataChanged(localMedia)
            mCallback!!.onPlaybackStateChanged(state)
        }
    }

    override fun play() {
        state = PlaybackState.STATE_PLAYING
        handlePlayState()
    }

    override fun pause() {
        state = PlaybackState.STATE_PAUSED
        handlePlayState()
    }

    override fun seekTo(position: Int) {
        mVideoView.seekTo(position)
    }

    override fun setCallback(callback: Playback.Callback) {
        mCallback = callback
    }

    override fun stop(isStop: Boolean) {
        if (isStop) {
            if (mCallback != null) {
                mCallback!!.onPlaybackStateChanged(state)
            }
        }
    }

    private fun handlePlayState() {
        Log.i(TAG, "handlePlayState")
        if (state == PlaybackState.STATE_PLAYING && !mVideoView.isPlaying) {
            mVideoView.start()
        } else if (state == PlaybackState.STATE_PAUSED && mVideoView.isPlaying) {
            mVideoView.pause()
        }
        if (mCallback != null) {
            mCallback!!.onPlaybackStateChanged(state)
        }
    }

    companion object {
        private val TAG = LocalVideoPlayback::class.java.simpleName
    }
}