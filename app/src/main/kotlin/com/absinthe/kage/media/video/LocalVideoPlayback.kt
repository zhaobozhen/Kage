package com.absinthe.kage.media.video

import android.media.session.PlaybackState
import android.net.Uri
import com.absinthe.kage.manager.ActivityStackManager
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.Playback
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import timber.log.Timber

class LocalVideoPlayback(private val exoPlayer: SimpleExoPlayer) : Playback {

    private var mCallback: Playback.Callback? = null

    init {
        exoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                Timber.d("ExoPlayer state = $playbackState")
                when (playbackState) {
                    ExoPlayer.STATE_READY -> {
                        play()
                    }
                    ExoPlayer.STATE_ENDED -> {
                        Timber.i("complete")
                        state = PlaybackState.STATE_PAUSED
                        if (mCallback != null) {
                            mCallback!!.onCompletion()
                        }
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
        state = PlaybackState.STATE_BUFFERING

        setVideo(Uri.parse(localMedia.filePath))

        if (mCallback != null) {
            mCallback?.onMediaMetadataChanged(localMedia)
            mCallback?.onPlaybackStateChanged(state)
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
        exoPlayer.seekTo(position.toLong())
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
        Timber.i("handlePlayState")
        if (state == PlaybackState.STATE_PLAYING && !exoPlayer.isPlaying) {
            exoPlayer.playWhenReady = true
        } else if (state == PlaybackState.STATE_PAUSED && exoPlayer.isPlaying) {
            exoPlayer.playWhenReady = false
        }
        if (mCallback != null) {
            mCallback!!.onPlaybackStateChanged(state)
        }
    }

    private fun setVideo(playerUri : Uri) {
        // 生成加载媒体数据的DataSource实例。
        val dataSourceFactory = DefaultDataSourceFactory(ActivityStackManager.topActivity, "kage-exoplayer")
        // 生成用于解析媒体数据的Extractor实例。
        val extractorsFactory = DefaultExtractorsFactory()

        // MediaSource代表要播放的媒体。
        val videoSource = ExtractorMediaSource(playerUri, dataSourceFactory, extractorsFactory,
                null, null)
        //Prepare the player with the source.
        exoPlayer.prepare(videoSource)
    }
}