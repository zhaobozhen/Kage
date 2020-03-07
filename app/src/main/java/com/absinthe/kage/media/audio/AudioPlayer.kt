package com.absinthe.kage.media.audio

import android.content.Context
import android.media.session.PlaybackState
import android.os.*
import android.os.PowerManager.WakeLock
import android.util.Log
import com.absinthe.kage.KageApplication
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.cmd.ResumePlayCommand
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.PlayList
import com.absinthe.kage.media.Playback
import java.util.*

object AudioPlayer: Observable(), Playback.Callback {

    const val TAG = "AudioPlayer"
    const val TYPE_LOCAL = 1
    const val TYPE_REMOTE = 2

    private const val EXTRA_PLAY_MODE = "EXTRA_PLAY_MODE"
    private const val NOT_REPEATING = 0
    private const val REPEAT_ONE = 1
    private const val SHUFFLED = 2
    private const val REPEAT_ALL = 3

    private var mPlayback: Playback? = null
    private var mPlaylist: PlayList? = null
    private var mWakeLock: WakeLock
    private var mPosition = 0

    private val mHandler = Handler(Looper.getMainLooper())
    private val mPlayMode = NOT_REPEATING

    private var playType = TYPE_LOCAL
    var playState = PlaybackState.STATE_NONE
        private set

    init {
        val pm = KageApplication.sContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:WakeLock")
    }

    fun release() {
        playState = PlaybackState.STATE_NONE
        if (mPlayback != null) {
            mPlayback!!.stop(false)
        }
        deleteObservers()
        if (mWakeLock.isHeld) {
            mWakeLock.release()
        }
    }

    fun setPlayerType(type: Int) {
        playType = type
        if (mPlayback != null) {
            mPosition = mPlayback!!.currentPosition
            mPlayback!!.stop(true)
        }
        if (type == TYPE_LOCAL) {
            mPlayback = LocalAudioPlayback(KageApplication.sContext)
            (mPlayback as LocalAudioPlayback).setCallback(this)
        } else if (type == TYPE_REMOTE) {
            mPlayback = RemoteAudioPlayback()
            (mPlayback as RemoteAudioPlayback).setCallback(this)
        }
        if (mPlayback != null && mPlaylist != null) {
            mPlayback!!.playMedia(mPlaylist!!.currentMedia)
        }
    }

    @Synchronized
    fun playMedia(media: LocalMedia) {
        mPosition = 0
        mPlayback!!.playMedia(media)
        if (mPlaylist == null) {
            mPlaylist = PlayList()
        }
        mPlaylist!!.addMedia(media)
        mPlaylist!!.currentIndex = 0
    }

    fun playMediaList(playList: PlayList?) {
        mPosition = 0
        if (mWakeLock.isHeld) {
            mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        }
        if (playList != null) {
            if (mPlaylist == null) {
                mPlaylist = PlayList()
            }
            mPlaylist!!.setList(playList.list, playList.currentIndex)
            if (mPlayback is RemoteAudioPlayback) {
                (mPlayback as RemoteAudioPlayback).playListMedia(mPlaylist!!)
            } else if (mPlayback is LocalAudioPlayback) {
                (mPlayback as LocalAudioPlayback).playMedia(mPlaylist!!.currentMedia)
            }
        }
    }

    fun play() {
        if (mWakeLock.isHeld) {
            mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        }
        if (mPlayback != null) {
            mPlayback!!.play()
        }
    }

    fun pause() {
        if (mPlayback != null) {
            mPlayback!!.pause()
        }
        if (mWakeLock.isHeld) {
            mWakeLock.release()
        }
    }

    fun resumePlay() {
        val deviceManager = DeviceManager.Singleton.INSTANCE.instance
        if (deviceManager.isConnected) {
            deviceManager.sendCommandToCurrentDevice(ResumePlayCommand())
        }
    }

    fun playNext() {
        if (mPlaylist != null) {
            playMedia(mPlaylist!!.getNextMedia(
                    mPlayMode == REPEAT_ONE || mPlayMode == REPEAT_ALL,
                    mPlayMode == SHUFFLED))
        }
    }

    fun playPrevious() {
        if (mPlaylist != null) {
            playMedia(mPlaylist!!.getPreviousMedia(
                    mPlayMode == REPEAT_ONE || mPlayMode == REPEAT_ALL,
                    mPlayMode == SHUFFLED))
        }
    }

    fun seekTo(to: Int) {
        if (mPlayback != null) {
            mPlayback!!.seekTo(to)
        }
    }

    val currentMedia: LocalMedia?
        get() = if (mPlaylist == null) null else mPlaylist!!.currentMedia

    val playbackState: PlaybackState
        get() {
            var actions: Long
            playState = mPlayback!!.state
            actions = if (playState == PlaybackState.STATE_PLAYING) {
                (PlaybackState.ACTION_STOP or PlaybackState.ACTION_PAUSE
                        or PlaybackState.ACTION_REWIND or PlaybackState.ACTION_FAST_FORWARD
                        or PlaybackState.ACTION_SEEK_TO)
            } else {
                PlaybackState.ACTION_STOP or PlaybackState.ACTION_PLAY
            }

            if (hasNext()) {
                actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT
            }
            if (hasPre()) {
                actions = actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS
            }

            val extras = Bundle()
            extras.putInt(EXTRA_PLAY_MODE, mPlayMode)
            val builder = PlaybackState.Builder()
            builder.setActions(actions)
            builder.setState(playState, mPlayback!!.currentPosition.toLong(), 1.0f)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                builder.setExtras(extras)
            }
            if (mPlaylist != null) {
                builder.setActiveQueueItemId(mPlaylist!!.currentIndex.toLong())
            }

            return builder.build()
        }

    val duration: Int
        get() = if (isPlayOrPause) {
            mPlayback!!.duration
        } else 0

    val bufferPosition: Int
        get() = if (isPlayOrPause) {
            mPlayback!!.bufferPosition
        } else 0

    val currentPosition: Int
        get() = if (isPlayOrPause) {
            mPlayback!!.currentPosition
        } else 0

    override fun onCompletion() {
        if (playState == PlaybackState.STATE_PLAYING) {
            playNext()
        } else {
            Log.w(TAG, "PlayState is not playing")
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        if (mPlayback != null && playState == PlaybackState.STATE_BUFFERING && state == PlaybackState.STATE_PLAYING && mPosition > 0) {
            Log.d(TAG, "Seek to: $mPosition")
            mPlayback!!.seekTo(mPosition)
            mPosition = 0
        }
        if (playState != state) {
            updateMediaPlayState()
            playState = state
        }
    }

    override fun onError(error: String) {
        playState = PlaybackState.STATE_ERROR
        updateMediaPlayState()
    }

    override fun onMediaMetadataChanged(localMedia: LocalMedia) {
        updateMediaMetadata(localMedia)
    }

    private fun updateMediaMetadata(media: LocalMedia) {
        mHandler.post(object : Runnable {
            override fun run() {
                synchronized(this) {
                    setChanged()
                    notifyObservers(media)
                }
            }
        })
    }

    private fun updateMediaPlayState() {
        mHandler.post(object : Runnable {
            override fun run() {
                synchronized(this) {
                    setChanged()
                    notifyObservers(playbackState)
                }
            }
        })
    }

    private operator fun hasNext(): Boolean {
        return mPlaylist != null && mPlaylist!!.hasNextMedia()
    }

    private fun hasPre(): Boolean {
        return mPlaylist != null && mPlaylist!!.hasPreviousMedia()
    }

    private val isPlayOrPause: Boolean
        get() = if (currentMedia != null) {
            playState == PlaybackState.STATE_PLAYING || playState == PlaybackState.STATE_PAUSED
        } else false
}