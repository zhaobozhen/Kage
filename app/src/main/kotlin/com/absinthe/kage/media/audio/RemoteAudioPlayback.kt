package com.absinthe.kage.media.audio

import android.media.session.PlaybackState
import com.absinthe.kage.connect.Const
import com.absinthe.kage.connect.proxy.AudioProxy
import com.absinthe.kage.device.model.AudioInfo
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.PlayList
import com.absinthe.kage.media.Playback
import com.absinthe.kage.utils.NetUtils.localAddress
import com.blankj.utilcode.util.Utils
import timber.log.Timber
import java.io.File
import java.util.*

class RemoteAudioPlayback internal constructor() : Playback {

    private var mCallback: Playback.Callback? = null
    private val mAudioProxy = AudioProxy
    private var mPlayList: PlayList? = null

    override var state = PlaybackState.STATE_NONE
        private set

    override val duration: Int
        get() = mAudioProxy.duration

    override val bufferPosition: Int
        get() = mAudioProxy.duration

    override val currentPosition: Int
        get() = mAudioProxy.currentPosition

    init {
        mAudioProxy.setOnPlayListener(object : AudioProxy.OnPlayListener {

            override fun onCurrentPositionChanged(duration: Int, position: Int) {}

            override fun onPlayStateChanged(oldState: Int, newState: Int) {
                Timber.d("onPlayStateChanged: $newState")

                when {
                    newState == AudioProxy.PlayStatue.INVALIDATE -> {
                        state = PlaybackState.STATE_NONE
                    }
                    newState == AudioProxy.PlayStatue.PLAYER_EXIT -> {
                        state = PlaybackState.STATE_STOPPED
                    }
                    newState != AudioProxy.PlayStatue.DISCONNECT -> {
                        when (newState) {
                            AudioProxy.PlayStatue.TRANSITIONING -> state = PlaybackState.STATE_BUFFERING
                            AudioProxy.PlayStatue.STOPPED -> state = PlaybackState.STATE_STOPPED
                            AudioProxy.PlayStatue.PLAYING -> state = PlaybackState.STATE_PLAYING
                            AudioProxy.PlayStatue.PAUSED -> state = PlaybackState.STATE_PAUSED
                        }
                    }
                    else -> {
                        state = PlaybackState.STATE_STOPPED
                    }
                }
                handlePlayState(false)
            }

            override fun onPlayIndexChanged(index: Int) {
                Timber.d("onPlayIndexChanged: $index")
                mPlayList?.let {
                    it.currentIndex = index
                    it.currentMedia?.let { media ->
                        mCallback?.onMediaMetadataChanged(media)
                    }
                }
            }
        })
    }

    fun playListMedia(playlist: PlayList) {
        mPlayList = playlist
        val audioInfos: MutableList<AudioInfo> = ArrayList()

        for (media in playlist.list) {
            if (media is LocalMusic) {
                audioInfos.add(AudioInfo().apply {
                    name = media.title
                    url = media.url
                    artist = media.artist
                    album = media.album
                })
            }
        }
        mAudioProxy.playList(playlist.currentIndex, audioInfos)
        state = PlaybackState.STATE_BUFFERING

        playlist.currentMedia?.let { mCallback?.onMediaMetadataChanged(it) }
        mCallback?.onPlaybackStateChanged(state)
    }

    override fun playMedia(localMedia: LocalMedia) {
        if (localMedia is LocalMusic) {
            val info = AudioInfo().apply {
                name = localMedia.title
                url = localMedia.url
                artist = localMedia.artist
                album = localMedia.album
            }

            if (localAddress.isNotEmpty()) {
                info.coverPath = (String.format(Const.HTTP_SERVER_FORMAT, localAddress)
                        + Utils.getApp().applicationContext.externalCacheDir
                        + File.separator + localMedia.albumId + ".png")
            }
            mAudioProxy.play(info)
            state = PlaybackState.STATE_BUFFERING

            mCallback?.onMediaMetadataChanged(localMedia)
            mCallback?.onPlaybackStateChanged(state)
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
        mAudioProxy.seekTo(position)
    }

    override fun setCallback(callback: Playback.Callback) {
        mCallback = callback
    }

    override fun stop(isStop: Boolean) {
        state = PlaybackState.STATE_STOPPED
        if (isStop && mCallback != null) {
            mAudioProxy.stop()
            mCallback?.onPlaybackStateChanged(state)
        }
        mAudioProxy.recycle()
    }

    private fun handlePlayState(fromUser: Boolean) {
        if (fromUser) {
            if (state == PlaybackState.STATE_PLAYING) {
                mAudioProxy.start()
            } else if (state == PlaybackState.STATE_PAUSED) {
                mAudioProxy.pause()
            }
        }
        mCallback?.onPlaybackStateChanged(state)
    }
}