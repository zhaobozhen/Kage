package com.absinthe.kage.media.video

import android.content.Context
import android.media.MediaPlayer
import android.media.session.PlaybackState
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.AttrRes
import com.absinthe.kage.R
import com.absinthe.kage.databinding.ViewVideoPlayerBinding
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.Playback
import com.absinthe.kage.media.video.VideoHelper.getVideoCoverImage
import java.util.*

open class VideoPlayer : FrameLayout, Playback.Callback {

    private lateinit var mBinding: ViewVideoPlayerBinding
    private lateinit var mRoot: View
    private lateinit var ibPlay: ImageButton
    private lateinit var seekbar: SeekBar
    private lateinit var videoView: VideoView
    private lateinit var ivCover: ImageView

    private var mPlayback: Playback? = null
    private var mLocalMedia: LocalMedia? = null
    private var mVideoPlayCallback: VideoPlayCallback? = null

    private var mContext: Context
    private var mFormatBuilder: StringBuilder = StringBuilder()
    private var mFormatter: Formatter = Formatter(mFormatBuilder, Locale.getDefault())
    private var mDragging = false
    private var mBeforePosition = 0
    private var mPlayState = 0
    private var isLoaded = false

    private val mPauseListener = OnClickListener { doPauseResume() }

    private val mShowProgress: Runnable = object : Runnable {
        override fun run() {
            val pos = setProgress()
            if (!mDragging && isPlaying) {
                postDelayed(this, 1000 - (pos % 1000).toLong())
            }
        }
    }

    private val mOnSeekBarChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            mDragging = true
            removeCallbacks(mShowProgress)
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser && mPlayback != null) {
                val newPosition = progress.toLong() * mPlayback!!.duration.toLong() / 1000
                mPlayback!!.seekTo(newPosition.toInt())
                mBinding.layoutSeekbar.seekbar.tvCurrentTime.text = stringForTime(newPosition.toInt())
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            mDragging = false
            setProgress()
            updatePausePlay()
            post(mShowProgress)
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        mContext = context
        init()
    }

    private fun init() {
        val frameParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        removeAllViews()
        addView(makeControllerView(), frameParams)
    }

    private fun makeControllerView(): View {
        val mInflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mBinding = ViewVideoPlayerBinding.inflate(mInflater)
        mRoot = mBinding.root
        initView()

        return mRoot
    }

    private fun initView() {
        ibPlay = mBinding.layoutSeekbar.ivPlay
        seekbar = mBinding.layoutSeekbar.seekbar.seekBar
        videoView = mBinding.videoView
        ivCover = mBinding.ivCover

        ibPlay.setOnClickListener(mPauseListener)
        seekbar.max = 1000
        seekbar.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
        videoView.setOnErrorListener { _: MediaPlayer?, what: Int, _: Int -> what == Int.MIN_VALUE || what == -38 }
    }

    fun playMedia(localMedia: LocalMedia) {
        removeCallbacks(mShowProgress)
        mBeforePosition = 0
        mLocalMedia = localMedia
        if (mPlayback != null) {
            mPlayback!!.playMedia(localMedia)
        }
    }

    fun setPlayerType(type: Int) {
        if (mPlayback != null) {
            mBeforePosition = mPlayback!!.currentPosition
            mPlayback!!.stop(true)
            mPlayback = null
        }
        if (type == TYPE_LOCAL) {
            ivCover.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            mPlayback = LocalVideoPlayback(videoView)
            (mPlayback as LocalVideoPlayback).setCallback(this)
        } else if (type == TYPE_REMOTE) {
            ivCover.visibility = View.VISIBLE
            videoView.visibility = View.GONE
            mPlayback = RemoteVideoPlayback()
            (mPlayback as RemoteVideoPlayback).setCallback(this)
        }
        if (mPlayback != null) {
            if (mLocalMedia != null) {
                mPlayback!!.playMedia(mLocalMedia!!)
            }
        }
    }

    fun release() {
        Log.d(TAG, "release")
        mPlayState = 0
        mPlayback?.stop(false)
    }

    fun setVideoPlayCallback(callback: VideoPlayCallback?) {
        mVideoPlayCallback = callback
    }

    private fun updatePausePlay() {
        if (isPlaying) {
            post(mShowProgress)
            ibPlay.setImageResource(R.drawable.ic_pause)
        } else {
            removeCallbacks(mShowProgress)
            ibPlay.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    private fun requestCoverImage() {
        if (ivCover.width > 0 && mLocalMedia != null) {
            Log.d(TAG, "mIVCover start load")
            val bitmap = getVideoCoverImage(mLocalMedia!!.filePath)
            isLoaded = true
            ivCover.setImageBitmap(bitmap)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!isLoaded) {
            requestCoverImage()
        }
    }

    override fun onCompletion() {
        Log.d(TAG, "onCompletion")
        seekbar.progress = seekbar.max
        updatePausePlay()
    }

    override fun onPlaybackStateChanged(state: Int) {
        if (mPlayback != null
                && mPlayState == PlaybackState.STATE_BUFFERING
                && state == PlaybackState.STATE_PLAYING
                && mBeforePosition > 0) {
            Log.d(TAG, "Seek to: $mBeforePosition")
            mPlayback!!.seekTo(mBeforePosition)
        }
        if (mPlayState != state) {
            updatePausePlay()
            mVideoPlayCallback?.changeState(state)
            mPlayState = state
        }
    }

    override fun onError(error: String) {}

    override fun onMediaMetadataChanged(localMedia: LocalMedia) {
        updatePausePlay()
        post(mShowProgress)
    }

    private fun setProgress(): Int {
        val position = mPlayback!!.currentPosition
        if (position == 0) {
            return 0
        }

        val duration = mPlayback!!.duration
        if (duration > 0) {
            seekbar.progress = (position.toLong() * 1000 / duration.toLong()).toInt()
        }
        seekbar.secondaryProgress = mPlayback!!.bufferPosition * 10
        mBinding.layoutSeekbar.seekbar.tvDuration.text = stringForTime(duration)
        mBinding.layoutSeekbar.seekbar.tvCurrentTime.text = stringForTime(position)
        return position
    }

    private fun doPauseResume() {
        if (mPlayback != null) {
            if (isPlaying) {
                mPlayback!!.pause()
            } else {
                mPlayback!!.play()
            }
            updatePausePlay()
            post(mShowProgress)
        }
    }

    private fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        mFormatBuilder.setLength(0)
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else mFormatter.format("%02d:%02d", minutes, seconds).toString()
    }

    private val isPlaying: Boolean
        get() = mPlayback != null && mPlayback!!.state == PlaybackState.STATE_PLAYING

    interface VideoPlayCallback {
        fun changeState(state: Int)
    }

    companion object {
        const val TYPE_LOCAL = 1
        const val TYPE_REMOTE = 2
        private val TAG = VideoPlayer::class.java.simpleName
    }
}