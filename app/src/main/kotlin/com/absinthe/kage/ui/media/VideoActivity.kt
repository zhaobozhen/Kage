package com.absinthe.kage.ui.media

import android.content.Intent
import android.media.session.PlaybackState
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.connect.proxy.RemoteControlProxy
import com.absinthe.kage.databinding.ActivityVideoBinding
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.DeviceObserverImpl
import com.absinthe.kage.device.IDeviceObserver
import com.absinthe.kage.device.model.DeviceInfo
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.TYPE_LOCAL
import com.absinthe.kage.media.TYPE_REMOTE
import com.absinthe.kage.media.video.VideoPlayer
import com.absinthe.kage.ui.connect.ConnectActivity
import com.blankj.utilcode.util.BarUtils

class VideoActivity : BaseActivity() {

    private lateinit var mBinding: ActivityVideoBinding
    private lateinit var mVideoPlayer: VideoPlayer
    private var mPlayState: Int = PlaybackState.STATE_NONE
    private var mType = TYPE_SENDER
    private var mLocalMedia: LocalMedia? = null

    private val mDeviceManager: DeviceManager = DeviceManager
    private val mObserver: IDeviceObserver = object : DeviceObserverImpl() {

        override fun onDeviceConnected(deviceInfo: DeviceInfo) {
            super.onDeviceConnected(deviceInfo)
            mBinding.toolbar.ibConnect.isSelected = true
        }

        override fun onDeviceDisConnect(deviceInfo: DeviceInfo) {
            super.onDeviceDisConnect(deviceInfo)
            mBinding.toolbar.ibConnect.isSelected = false
        }
    }

    override fun setViewBinding() {
        mBinding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
    }

    override fun setToolbar() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getType()
        getMedia()
        initView()
        initPlayer()
        mDeviceManager.register(mObserver)
    }

    override fun onDestroy() {
        mVideoPlayer.release()
        mDeviceManager.unregister(mObserver)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (event?.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                RemoteControlProxy.sendVolumeUpKeyAction()
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                RemoteControlProxy.sendVolumeDownKeyAction()
            }
        }
        return super.onKeyDown(keyCode, event)

    }

    private fun initView() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        mBinding.toolbar.root.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0)

        mVideoPlayer = mBinding.videoPlayer
        mVideoPlayer.setVideoPlayCallback(object : VideoPlayer.VideoPlayCallback {

            override fun changeState(state: Int) {
                if (state == PlaybackState.STATE_STOPPED) {
                    finish()
                }
                mPlayState = state
            }
        })

        mBinding.apply {
            toolbar.ibBack.setOnClickListener { finish() }

            if (mType == TYPE_SENDER) {
                toolbar.ibConnect.setOnClickListener {
                    startActivity(Intent(this@VideoActivity, ConnectActivity::class.java))
                }
                videoPlayer.setCastButtonClickListener(View.OnClickListener {
                    if (DeviceManager.isConnected) {
                        mVideoPlayer.setPlayerType(TYPE_REMOTE)
                    } else {
                        startActivity(Intent(this@VideoActivity, ConnectActivity::class.java))
                    }
                })
            } else {
                toolbar.ibConnect.visibility = View.GONE
                videoPlayer.setCastButtonVisibility(View.GONE)
            }
        }
    }

    private fun getType() {
        mType = intent.getIntExtra(EXTRA_TYPE, TYPE_SENDER)
    }

    private fun getMedia() {
        mLocalMedia = intent.getParcelableExtra(EXTRA_MEDIA)
        mLocalMedia ?: finish()
    }

    private fun initPlayer() {
        mLocalMedia?.let { mVideoPlayer.playMedia(it) }
        mVideoPlayer.setPlayerType(TYPE_LOCAL)
    }

    companion object {
        const val EXTRA_MEDIA = "EXTRA_MEDIA"
        const val EXTRA_TYPE = "EXTRA_TYPE"
        const val TYPE_SENDER = 0
        const val TYPE_RECEIVER = 1
    }
}