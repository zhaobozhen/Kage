package com.absinthe.kage.media.audio

import android.content.Context
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import com.absinthe.kage.media.*
import com.blankj.utilcode.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

object AudioPlayer : Observable(), Playback.Callback {

    private const val EXTRA_PLAY_MODE = "EXTRA_PLAY_MODE"
    private const val NOT_REPEATING = 0
    private const val REPEAT_ONE = 1
    private const val SHUFFLED = 2
    private const val REPEAT_ALL = 3

    private var mPlayback: Playback? = null
    private var mPlaylist: PlayList = PlayList()
    private var mWakeLock: WakeLock
    private var playType = TYPE_LOCAL
    private var mPosition = 0

    private val mPlayMode = NOT_REPEATING

    var playState = PlaybackState.STATE_NONE
        private set

    val currentMedia: LocalMedia?
        get() = mPlaylist.currentMedia

    val playbackState: PlaybackState
        get() {
            playState = mPlayback?.state ?: PlaybackState.STATE_NONE
            var actions = if (playState == PlaybackState.STATE_PLAYING) {
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

            val builder = PlaybackState.Builder().apply {
                setActions(actions)
                setState(playState, mPlayback!!.currentPosition.toLong(), 1.0f)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val extras = Bundle().apply {
                        putInt(EXTRA_PLAY_MODE, mPlayMode)
                    }
                    setExtras(extras)
                }
                setActiveQueueItemId(mPlaylist.currentIndex.toLong())
            }

            return builder.build()
        }

    val duration: Int
        get() = if (mPlayback != null && isPlayOrPause) {
            mPlayback!!.duration
        } else 0

    val bufferPosition: Int
        get() = if (mPlayback != null && isPlayOrPause) {
            mPlayback!!.bufferPosition
        } else 0

    val currentPosition: Int
        get() = if (mPlayback != null && isPlayOrPause) {
            mPlayback!!.currentPosition
        } else 0

    init {
        val pm = Utils.getApp().applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AudioPlayer:WakeLock")
    }

    fun release() {
        playState = PlaybackState.STATE_NONE
        mPlayback?.stop(false)
        deleteObservers()

        if (mWakeLock.isHeld) {
            mWakeLock.release()
        }
    }

    fun setPlayerType(type: Int) {
        playType = type
        mPlayback?.let {
            mPosition = it.currentPosition
            it.stop(true)
        }

        if (type == TYPE_LOCAL) {
            mPlayback = LocalAudioPlayback(Utils.getApp().applicationContext)
            (mPlayback as LocalAudioPlayback).setCallback(this)
        } else if (type == TYPE_REMOTE) {
            mPlayback = RemoteAudioPlayback()
            (mPlayback as RemoteAudioPlayback).setCallback(this)
        }
        mPlaylist.currentMedia?.let { mPlayback?.playMedia(it) }
    }

    @Synchronized
    fun playMedia(media: LocalMedia) {
        if (mPlaylist.queryMediaIndex(media) == -1) {
            mPlaylist.addMediaToTop(media)
            mPlaylist.currentIndex = 0
        } else {
            mPlaylist.currentIndex.plus(1)
        }

        mPosition = 0
        mPlayback?.playMedia(media)
    }

    fun playMediaList(playList: PlayList) {
        mPosition = 0

        if (mWakeLock.isHeld) {
            mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        }
        mPlaylist.setList(playList.list, playList.currentIndex)

        if (mPlayback is RemoteAudioPlayback) {
            (mPlayback as RemoteAudioPlayback).playListMedia(mPlaylist)
        } else if (mPlayback is LocalAudioPlayback) {
            mPlaylist.currentMedia?.let { (mPlayback as LocalAudioPlayback).playMedia(it) }
        }
    }

    fun play() {
        if (mWakeLock.isHeld) {
            mWakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        }
        mPlayback?.play()
    }

    fun pause() {
        mPlayback?.pause()

        if (mWakeLock.isHeld) {
            mWakeLock.release()
        }
    }

    fun playNext() {
        val next: LocalMedia? = mPlaylist.getNextMedia(
                mPlayMode == REPEAT_ONE || mPlayMode == REPEAT_ALL,
                mPlayMode == SHUFFLED)
        if (next != null) {
            playMedia(next)
        }
    }

    fun playPrevious() {
        val previous: LocalMedia? = mPlaylist.getPreviousMedia(
                mPlayMode == REPEAT_ONE || mPlayMode == REPEAT_ALL,
                mPlayMode == SHUFFLED)
        if (previous != null) {
            playMedia(previous)
        }
    }

    fun seekTo(to: Int) {
        mPlayback?.seekTo(to)
    }

    override fun onCompletion() {
        if (playState == PlaybackState.STATE_PLAYING) {
            playNext()
        } else {
            Timber.w("PlayState is not playing")
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        mPlayback?.let {
            if (playState == PlaybackState.STATE_BUFFERING && state == PlaybackState.STATE_PLAYING && mPosition > 0) {
                Timber.d("Seek to: $mPosition")
                it.seekTo(mPosition)
                mPosition = 0
            }
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
        GlobalScope.launch(Dispatchers.Main) {
            synchronized(this) {
                setChanged()
                notifyObservers(media)
            }
        }
    }

    private fun updateMediaPlayState() {
        GlobalScope.launch(Dispatchers.Main) {
            synchronized(this) {
                setChanged()
                notifyObservers(playbackState)
            }
        }
    }

    private operator fun hasNext(): Boolean {
        return mPlaylist.hasNextMedia()
    }

    private fun hasPre(): Boolean {
        return mPlaylist.hasPreviousMedia()
    }

    private val isPlayOrPause: Boolean
        get() = if (currentMedia != null) {
            playState == PlaybackState.STATE_PLAYING || playState == PlaybackState.STATE_PAUSED
        } else false
}