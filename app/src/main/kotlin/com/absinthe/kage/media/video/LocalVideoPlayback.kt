package com.absinthe.kage.media.video

import android.media.session.PlaybackState
import android.net.Uri
import com.absinthe.kage.connect.proxy.BaseProxy
import com.absinthe.kage.connect.proxy.MODE_VIDEO
import com.absinthe.kage.manager.ActivityStackManager
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.Playback
import com.blankj.utilcode.util.Utils
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import timber.log.Timber

class LocalVideoPlayback(private val exoPlayer: SimpleExoPlayer) : Playback {

    private var mCallback: Playback.Callback? = null
    private var shouldPlay = true

    init {
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    ExoPlayer.STATE_READY -> {
                        Timber.i("STATE_READY")
                        if (shouldPlay) {
                            play()
                            shouldPlay = false
                        }
                    }
                    ExoPlayer.STATE_ENDED -> {
                        Timber.i("Complete")
                        state = PlaybackState.STATE_PAUSED
                        mCallback?.onCompletion()
                        shouldPlay = true
                    }
                    Player.STATE_BUFFERING -> {
                        Timber.d("Buffering")
                    }
                    Player.STATE_IDLE -> {
                        Timber.d("Idle")
                    }
                }
            }
        })
        INSTANCE = this
    }

    override var state = PlaybackState.STATE_NONE
        private set

    override val duration: Int
        get() = exoPlayer.duration.toInt()

    override val bufferPosition: Int
        get() = exoPlayer.bufferedPosition.toInt()

    override val currentPosition: Int
        get() = exoPlayer.currentPosition.toInt()

    override fun playMedia(localMedia: LocalMedia) {
        BaseProxy.CURRENT_MODE = MODE_VIDEO
        state = PlaybackState.STATE_BUFFERING

        setVideo(Uri.parse(localMedia.filePath))

        mCallback?.onMediaMetadataChanged(localMedia)
        mCallback?.onPlaybackStateChanged(state)
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
        exoPlayer.seekTo(position.toLong())
    }

    override fun setCallback(callback: Playback.Callback) {
        mCallback = callback
    }

    override fun stop(isStop: Boolean) {
        if (isStop) {
            mCallback?.onPlaybackStateChanged(state)
        }
    }

    private fun handlePlayState() {
        if (state == PlaybackState.STATE_PLAYING) {
            exoPlayer.playWhenReady = true
        } else if (state == PlaybackState.STATE_PAUSED) {
            exoPlayer.playWhenReady = false
        }
        mCallback?.onPlaybackStateChanged(state)
    }

    private fun setVideo(playerUri : Uri) {
        // 生成加载媒体数据的DataSource实例。
        val dataSourceFactory = DefaultDataSourceFactory(ActivityStackManager.topActivity,
                Util.getUserAgent(Utils.getApp().applicationContext, "Kage"))

        // MediaSource代表要播放的媒体。
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(playerUri)

        //Prepare the player with the source.
        exoPlayer.prepare(videoSource)
    }

    companion object {
        var INSTANCE : LocalVideoPlayback? = null
    }
}