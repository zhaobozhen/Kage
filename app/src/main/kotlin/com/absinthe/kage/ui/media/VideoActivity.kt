package com.absinthe.kage.ui.media

import android.media.session.PlaybackState
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.absinthe.kage.databinding.ActivityVideoBinding
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.DeviceObserverImpl
import com.absinthe.kage.device.IDeviceObserver
import com.absinthe.kage.device.model.DeviceInfo
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.video.VideoPlayer
import com.absinthe.kage.utils.ToastUtil

class VideoActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityVideoBinding
    private var mPlayState: Int = PlaybackState.STATE_NONE
    private val mDeviceManager: DeviceManager = DeviceManager
    private val mObserver: IDeviceObserver = object : DeviceObserverImpl() {

        override fun onDeviceConnected(deviceInfo: DeviceInfo?) {
            super.onDeviceConnected(deviceInfo)
            ToastUtil.makeText("Connected")
        }

        override fun onDeviceDisConnect(deviceInfo: DeviceInfo?) {
            super.onDeviceDisConnect(deviceInfo)
            ToastUtil.makeText("Disconnected")
        }
    }
    private var mLocalMedia: LocalMedia? = null
    private lateinit var mVideoPlayer: VideoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

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

    private fun initView() {
        mVideoPlayer = mBinding.videoPlayer
        mVideoPlayer.setVideoPlayCallback(object : VideoPlayer.VideoPlayCallback {

            override fun changeState(state: Int) {
                if (state == PlaybackState.STATE_STOPPED) {
                    finish()
                }
                mPlayState = state
            }
        })
    }

    private fun getMedia() {
        mLocalMedia = intent.getParcelableExtra(EXTRA_MEDIA)
        if (mLocalMedia == null) {
            finish()
        }
    }

    private fun initPlayer() {
        mLocalMedia?.let { mVideoPlayer.playMedia(it) }
        mVideoPlayer.changePlayer(VideoPlayer.TYPE_LOCAL)
    }

    companion object {
        const val EXTRA_MEDIA = "EXTRA_MEDIA"
    }
}