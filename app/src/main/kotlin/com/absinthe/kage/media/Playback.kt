package com.absinthe.kage.media

interface Playback {
    interface Callback {
        fun onCompletion()
        fun onError(error: String)
        fun onMediaMetadataChanged(localMedia: LocalMedia)
        fun onPlaybackStateChanged(state: Int)
    }

    val bufferPosition: Int
    val currentPosition: Int
    val duration: Int
    val state: Int

    fun pause()
    fun play()
    fun playMedia(localMedia: LocalMedia)
    fun seekTo(position: Int)
    fun setCallback(callback: Callback)
    fun stop(isStop: Boolean)
}