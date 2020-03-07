package com.absinthe.kage.ui.media

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.R
import com.absinthe.kage.databinding.ActivityMusicBinding
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.DeviceManager.isConnected
import com.absinthe.kage.device.DeviceObserverImpl
import com.absinthe.kage.device.IDeviceObserver
import com.absinthe.kage.device.model.DeviceInfo
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.PlayList
import com.absinthe.kage.media.audio.AudioPlayer
import com.absinthe.kage.media.audio.LocalMusic
import com.absinthe.kage.media.audio.MusicHelper.getAlbumArt
import com.absinthe.kage.ui.connect.ConnectActivity
import com.absinthe.kage.ui.sender.MusicListActivity
import com.absinthe.kage.utils.Logger.d
import com.absinthe.kage.utils.StorageUtils.saveBitmap
import com.blankj.utilcode.util.ImageUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.File
import java.util.*

class MusicActivity : BaseActivity(), Observer {
    
    private lateinit var mBinding: ActivityMusicBinding
    private lateinit var deviceObserver: IDeviceObserver
    private var mLocalMusic: LocalMusic? = null
    private var mDeviceManager: DeviceManager = DeviceManager
    private var mAudioPlayer: AudioPlayer = AudioPlayer
    private var mObjectAnimator: ObjectAnimator = ObjectAnimator()
    private val mHandler = Handler()
    private var isSeekBarTouch = false
    private var mCurrentRotation = 0.0f
    private var type = TYPE_NONE
    private val mShowProgressTask: Runnable = object : Runnable {
        override fun run() {
            mHandler.postDelayed(this, 1000 - (updatePlayPosition() % 1000).toLong())
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        mBinding = ActivityMusicBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initListener()
        initView()
        processIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        updateMediaInfo(mAudioPlayer.currentMedia)
        updatePlayState(mAudioPlayer.playbackState, false)
        mAudioPlayer.addObserver(this)
    }

    override fun onPause() {
        super.onPause()
        pauseAnimation()
        mHandler.removeCallbacks(mShowProgressTask)
        mAudioPlayer.deleteObserver(this)
    }

    override fun onDestroy() {
        mDeviceManager.unregister(deviceObserver)
        mAudioPlayer.release()
        super.onDestroy()
    }

    override fun update(o: Observable, arg: Any) {
        if (arg is PlaybackState) {
            updatePlayState(arg, true)
        } else if (arg is LocalMedia) {
            updateMediaInfo(arg)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        if (intent != null) {
            getMusicInfo(intent)
            initPlayer()
        } else {
            finish()
        }
    }

    private fun getMusicInfo(intent: Intent) {
        val localMusic: LocalMusic? = intent.getParcelableExtra(EXTRA_MUSIC_INFO)
        if (localMusic != null) {
            mLocalMusic = localMusic
        }
        
        type = intent.getIntExtra(EXTRA_DEVICE_TYPE, TYPE_NONE)
        when (type) {
            TYPE_NONE -> finish()
            TYPE_SENDER -> {
                mBinding.btnCast.visibility = View.VISIBLE
                mBinding.toolbar.ibConnect.visibility = View.VISIBLE
            }
            TYPE_RECEIVER -> {
                mBinding.btnCast.visibility = View.GONE
                mBinding.toolbar.ibConnect.visibility = View.GONE
            }
        }
    }

    private fun initView() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        mBinding.toolbar.ibConnect.isSelected = mDeviceManager.isConnected
        initAnimator()
    }

    private fun initListener() {
        deviceObserver = object : DeviceObserverImpl() {
            
            override fun onDeviceConnected(deviceInfo: DeviceInfo?) {
                mBinding.toolbar.ibConnect.isSelected = true
            }

            override fun onDeviceDisConnect(deviceInfo: DeviceInfo?) {
                mBinding.toolbar.ibConnect.isSelected = false
            }
        }
        mDeviceManager.register(deviceObserver)
        
        mBinding.toolbar.ibBack.setOnClickListener { finish() }
        mBinding.toolbar.ibConnect.setOnClickListener { 
            startActivity(Intent(this@MusicActivity, ConnectActivity::class.java)) 
        }
        mBinding.layoutSeekBar.seekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mHandler.removeCallbacks(mShowProgressTask)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isSeekBarTouch = true
                if (seekBar.progress > seekBar.secondaryProgress) {
                    seekBar.progress = seekBar.secondaryProgress
                }
                mAudioPlayer.seekTo(seekBar.progress)
                mHandler.post(mShowProgressTask)
            }
        })
        mBinding.btnCast.setOnClickListener {
            if (isConnected) {
                mAudioPlayer.setPlayerType(AudioPlayer.TYPE_REMOTE)
            } else {
                startActivity(Intent(this@MusicActivity, ConnectActivity::class.java))
            }
        }
        mBinding.layoutControls.btnPlay.setOnClickListener {
            val state = mAudioPlayer.playState
            if (state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING) {
                mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_play_arrow)
                mAudioPlayer.pause()
            } else if (state == PlaybackState.STATE_PAUSED) {
                mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_pause)
                mAudioPlayer.play()
            }
        }
        mBinding.layoutControls.btnPrevious.setOnClickListener {
            mAudioPlayer.playPrevious()
        }
        mBinding.layoutControls.btnNext.setOnClickListener {
            mAudioPlayer.playNext()
        }
    }

    private fun initPlayer() {
        mAudioPlayer.setPlayerType(AudioPlayer.TYPE_LOCAL)
        mLocalMusic?.let { mAudioPlayer.playMedia(it) }

        if (type == TYPE_SENDER) {
            val playList = PlayList()
            for (localMedia in MusicListActivity.sMusicList) {
                playList.addMedia(localMedia)
            }
            playList.currentIndex = MusicListActivity.sMusicList.indexOf(mLocalMusic)
            mAudioPlayer.playMediaList(playList)
        }
    }

    private fun updatePlayPosition(): Int {
        val max = mAudioPlayer.duration
        val current = mAudioPlayer.currentPosition
        val buffer = mAudioPlayer.bufferPosition

        mBinding.layoutSeekBar.seekBar.max = max
        if (isSeekBarTouch) {
            isSeekBarTouch = false
        } else {
            mBinding.layoutSeekBar.seekBar.progress = current
        }

        mBinding.layoutSeekBar.seekBar.secondaryProgress = buffer
        mBinding.layoutSeekBar.tvCurrentTime.text = LocalMedia.millisecondToTimeString(current)
        mBinding.layoutSeekBar.tvDuration.text = LocalMedia.millisecondToTimeString(max)

        return current
    }

    private fun updatePlayState(playbackState: PlaybackState, isNotify: Boolean) {
        val state = playbackState.state
        d("state:", state)

        when (state) {
            PlaybackState.STATE_BUFFERING -> {
                mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_pause)
            }
            PlaybackState.STATE_PLAYING -> {
                mHandler.post(mShowProgressTask)
                startAnimation()
                mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_pause)
            }
            else -> {
                mHandler.removeCallbacks(mShowProgressTask)
                pauseAnimation()
                mBinding.layoutControls.btnPlay.setIconResource(R.drawable.ic_play_arrow)
            }
        }

        val actions = playbackState.actions
        mBinding.layoutControls.btnPrevious.isEnabled = PlaybackState.ACTION_SKIP_TO_PREVIOUS and actions != 0L
        mBinding.layoutControls.btnNext.isEnabled = PlaybackState.ACTION_SKIP_TO_NEXT and actions != 0L

        if (isNotify && state == PlaybackState.STATE_STOPPED) {
            finish()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateMediaInfo(media: LocalMedia?) {
        if (media != null) {
            if (media is LocalMusic) {
                mBinding.toolbar.tvMusicName.text = media.getTitle()
                mBinding.toolbar.tvArtist.text = media.artist
                if (type == TYPE_SENDER) {
                    applyRouletteAndBlurBackground(media.albumId)
                } else if (type == TYPE_RECEIVER) {
                    applyRouletteAndBlurBackground(Uri.parse(media.coverPath))
                }
                mHandler.post(mShowProgressTask)
            }
            mBinding.layoutSeekBar.tvCurrentTime.text = "00:00"
            mBinding.layoutSeekBar.tvDuration.text = LocalMedia.millisecondToTimeString(media.duration.toInt())
        }
    }

    private fun applyRouletteAndBlurBackground(albumId: Int) {
        Glide.with(this)
                .asBitmap()
                .load(getAlbumArt(albumId.toLong()))
                .into(object : CustomTarget<Bitmap?>() {

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                        val result = ImageUtils.renderScriptBlur(resource, 25f)
                        val cMatrix = ColorMatrix()
                        val brightness = -70
                        cMatrix.set(floatArrayOf(1f, 0f, 0f, 0f, brightness.toFloat(), 0f, 1f, 0f, 0f, brightness.toFloat(), 0f, 0f, 1f, 0f, brightness.toFloat(), 0f, 0f, 0f, 1f, 0f))
                        val paint = Paint()
                        paint.colorFilter = ColorMatrixColorFilter(cMatrix)
                        val canvas = Canvas(result)
                        // 在 Canvas 上绘制一个已经存在的 Bitmap
                        canvas.drawBitmap(result, 0f, 0f, paint)
                        mBinding.ivBackground.setImageBitmap(result)
                        saveAlbumBitmap(resource, albumId)
                        Glide.with(applicationContext)
                                .load(resource)
                                .into(mBinding.musicRoulette)
                    }
                })
    }

    private fun applyRouletteAndBlurBackground(uri: Uri) {
        Glide.with(this)
                .asBitmap()
                .load(uri)
                .into(object : CustomTarget<Bitmap?>() {

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                        val result = ImageUtils.renderScriptBlur(resource, 25f)
                        val cMatrix = ColorMatrix()
                        val brightness = -70
                        cMatrix.set(floatArrayOf(1f, 0f, 0f, 0f, brightness.toFloat(), 0f, 1f, 0f, 0f, brightness.toFloat(), 0f, 0f, 1f, 0f, brightness.toFloat(), 0f, 0f, 0f, 1f, 0f))
                        val paint = Paint()
                        paint.colorFilter = ColorMatrixColorFilter(cMatrix)
                        val canvas = Canvas(result)
                        // 在 Canvas 上绘制一个已经存在的 Bitmap
                        canvas.drawBitmap(result, 0f, 0f, paint)
                        mBinding.ivBackground.setImageBitmap(result)
                        Glide.with(applicationContext)
                                .load(resource)
                                .into(mBinding.musicRoulette)
                    }
                })
    }

    private fun initAnimator() {
        mCurrentRotation = 0.0f
        mObjectAnimator.target = mBinding.musicRoulette
        mObjectAnimator.setPropertyName("rotation")
        mObjectAnimator.duration = 20000
        mObjectAnimator.interpolator = LinearInterpolator()
        mObjectAnimator.repeatCount = ValueAnimator.INFINITE
        mObjectAnimator.repeatMode = ValueAnimator.RESTART
        mObjectAnimator.addUpdateListener { animation: ValueAnimator -> mCurrentRotation = animation.animatedValue as Float }

        if (mObjectAnimator.isStarted) {
            mObjectAnimator.cancel()
        }
    }

    private fun startAnimation() {
        if (!mObjectAnimator.isStarted) {
            mObjectAnimator.setFloatValues(mCurrentRotation, mCurrentRotation + 360.0f)
            mObjectAnimator.start()
        }
    }

    private fun pauseAnimation() {
        if (mObjectAnimator.isStarted) {
            mObjectAnimator.cancel()
        }
    }

    private fun stopAnimation() {
        if (mObjectAnimator.isStarted || mObjectAnimator.isRunning) {
            mObjectAnimator.end()
        }
        mCurrentRotation = 0.0f
    }

    private fun saveAlbumBitmap(bitmap: Bitmap, albumId: Int) {
        val file = File(externalCacheDir, "$albumId.png")
        saveBitmap(bitmap, file)
    }

    companion object {
        const val EXTRA_MUSIC_INFO = "MUSIC_INFO"
        const val EXTRA_DEVICE_TYPE = "DEVICE_TYPE"
        const val TYPE_NONE = -1
        const val TYPE_SENDER = 0
        const val TYPE_RECEIVER = 1
    }
}