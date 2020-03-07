package com.absinthe.kage.media.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.Playback

class LocalAudioPlayback internal constructor(context: Context) : Playback {

    private lateinit var mFocusChangeListener: OnAudioFocusChangeListener
    private val mContext: Context = context.applicationContext
    private val mAudioManager: AudioManager

    private var mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK
    private var mAudioSession = 0
    private var mPlayOnFocusGain = false
    private var mCallback: Playback.Callback? = null
    private var mMediaPlayer: MediaPlayer = MediaPlayer()

    init {
        mAudioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setMediaPlayerListener()
        mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK)
        createAudioFocusChangeListener()
    }

    override var state = PlaybackState.STATE_NONE
        private set

    override fun playMedia(localMedia: LocalMedia) {
        try {
            mMediaPlayer.reset()

            if (localMedia.filePath.isEmpty()) {
                state = PlaybackState.STATE_ERROR
                if (mCallback != null) {
                    mCallback!!.onPlaybackStateChanged(state)
                }
                return
            }

            if (mCallback != null) {
                mCallback!!.onMediaMetadataChanged(localMedia)
            }

            if (mAudioSession != 0) {
                mMediaPlayer.audioSessionId = mAudioSession
            } else {
                mAudioSession = mMediaPlayer.audioSessionId
            }

            state = PlaybackState.STATE_BUFFERING
            if (mCallback != null) {
                mCallback!!.onPlaybackStateChanged(state)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mAudioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                mMediaPlayer.setAudioAttributes(mAudioAttributes)
            } else {
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }

            mMediaPlayer.setDataSource(localMedia.filePath)
            mMediaPlayer.prepareAsync()
        } catch (e: Exception) {
            e.printStackTrace()
            state = PlaybackState.STATE_ERROR
            if (mCallback != null) {
                mCallback!!.onPlaybackStateChanged(state)
            }
        }
    }

    override fun play() {
        if (mPlayOnFocusGain) {
            tryToGetAudioFocus()
        }
        state = PlaybackState.STATE_PLAYING
        handlePlayState()
    }

    override fun pause() {
        mPlayOnFocusGain = true
        state = PlaybackState.STATE_PAUSED
        handlePlayState()
    }

    override fun seekTo(position: Int) {
        var pos = position
        Log.d(TAG, "SeekTo: $pos")
        if (position < 0) {
            pos = 0
        }
        if (isPlayOrPause) {
            val bufferPos = duration
            if (pos > bufferPos) {
                pos = bufferPos
            }
            mMediaPlayer.seekTo(pos)
        }
    }

    override fun stop(isStop: Boolean) {
        Log.d(TAG, "stop")
        state = PlaybackState.STATE_STOPPED

        if (isStop) {
            if (mCallback != null) {
                mCallback!!.onPlaybackStateChanged(state)
            }
        }

        try {
            mMediaPlayer.stop()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
        mMediaPlayer.release()
        abandonAudioFocus()
    }

    override val duration: Int
        get() = if (isPlayOrPause) {
            mMediaPlayer.duration
        } else 0

    override val bufferPosition: Int
        get() = if (isPlayOrPause) {
            duration
        } else 0

    override val currentPosition: Int
        get() = if (isPlayOrPause) {
            mMediaPlayer.currentPosition
        } else 0

    override fun setCallback(callback: Playback.Callback) {
        mCallback = callback
    }

    private fun handlePlayState() {
        if (state == PlaybackState.STATE_PLAYING && !mMediaPlayer.isPlaying) {
            mMediaPlayer.start()
        } else if (state == PlaybackState.STATE_PAUSED && mMediaPlayer.isPlaying) {
            mMediaPlayer.pause()
        }
        if (mCallback != null) {
            mCallback!!.onPlaybackStateChanged(state)
        }
    }

    private val isPlayOrPause: Boolean
        get() = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_PAUSED

    private fun createAudioFocusChangeListener() {
        mFocusChangeListener = OnAudioFocusChangeListener { focusChange: Int ->
            Log.d(TAG, "onAudioFocusChange: $focusChange")
            if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                mAudioFocus = AUDIO_FOCUSED
                if (state == PlaybackState.STATE_PAUSED) {
                    play()
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    mAudioFocus = AUDIO_NO_FOCUS_CAN_DUCK
                }
                if (state == PlaybackState.STATE_PLAYING) {
                    pause()
                }
            } else {
                Log.d(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: $focusChange")
            }
        }
    }

    private fun tryToGetAudioFocus() {
        Log.d(TAG, "Try to get AudioFocus")
        if (mAudioFocus != AUDIO_FOCUSED) {
            val result: Int
            result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mAudioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                val mAudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(mAudioAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(mFocusChangeListener)
                        .build()
                mAudioManager.requestAudioFocus(mAudioFocusRequest)
            } else {
                mAudioManager.requestAudioFocus(mFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED
                Log.d(TAG, "Try to get AudioFocus success")
            }
        }
    }

    private fun abandonAudioFocus() {
        Log.d(TAG, "abandonAudioFocus")
        if (mAudioFocus == AUDIO_FOCUSED) {
            val result: Int
            result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mAudioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                val mAudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(mAudioAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(mFocusChangeListener)
                        .build()
                mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest)
            } else {
                mAudioManager.abandonAudioFocus(mFocusChangeListener)
            }
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK
                Log.d(TAG, "abandonAudioFocus success")
            }
        }
    }

    private fun setMediaPlayerListener() {
        mMediaPlayer.setOnErrorListener { _: MediaPlayer?, what: Int, extra: Int ->
            Log.d(TAG, "SetOnErrorListener, what: $what, extra: $extra")
            state = PlaybackState.STATE_ERROR
            false
        }
        mMediaPlayer.setOnCompletionListener {
            Log.d(TAG, "MediaPlayer#onCompletion")
            if (mCallback != null) {
                mCallback!!.onCompletion()
            }
        }
        mMediaPlayer.setOnPreparedListener {
            Log.d(TAG, "SetOnPreparedListener")
            play()
        }
        mMediaPlayer.setOnInfoListener { _: MediaPlayer?, what: Int, extra: Int ->
            Log.d(TAG, "SetOnInfoListener, what: $what, extra: $extra")
            false
        }
    }

    companion object {
        private val TAG = LocalAudioPlayback::class.java.simpleName
        private const val AUDIO_NO_FOCUS_NO_DUCK = 0
        private const val AUDIO_NO_FOCUS_CAN_DUCK = 1
        private const val AUDIO_FOCUSED = 2
    }
}