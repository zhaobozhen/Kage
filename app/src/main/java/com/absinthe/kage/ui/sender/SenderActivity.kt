package com.absinthe.kage.ui.sender

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.R
import com.absinthe.kage.connect.proxy.ImageProxy
import com.absinthe.kage.databinding.ActivitySenderBinding
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.ui.connect.ConnectActivity
import com.absinthe.kage.ui.media.VideoActivity
import com.absinthe.kage.utils.ToastUtil.makeText
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.listener.OnChooseItemListener

class SenderActivity : BaseActivity() {

    private lateinit var mBinding: ActivitySenderBinding
    private lateinit var mImageListener: OnChooseItemListener
    private lateinit var mVideoListener: OnChooseItemListener
    private val rxPermissions = RxPermissions(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivitySenderBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initView()
    }

    override fun onDestroy() {
        ImageProxy.getInstance().close()
        super.onDestroy()
    }

    private fun initView() {
        mImageListener = object : OnChooseItemListener {

            override fun onChoose(itemUri: String) {
                if (DeviceManager.Singleton.INSTANCE.instance.isConnected) {
                    ImageProxy.getInstance().cast(itemUri)
                } else {
                    startActivity(Intent(this@SenderActivity, ConnectActivity::class.java))
                    finish()
                }
            }

            override fun onStop() {
                if (DeviceManager.Singleton.INSTANCE.instance.isConnected) {
                    ImageProxy.getInstance().stop()
                } else {
                    startActivity(Intent(this@SenderActivity, ConnectActivity::class.java))
                    finish()
                }
            }

            override fun onPreview() {
                Log.d(TAG, "onPreview()")
            }

            override fun onNext() {
                Log.d(TAG, "onNext()")
            }
        }

        mVideoListener = object : OnChooseItemListener {

            override fun onChoose(itemUri: String) {
                startActivity(Intent(this@SenderActivity, VideoActivity::class.java))
            }

            override fun onStop() {}
            override fun onPreview() {}
            override fun onNext() {}
        }

        mBinding.btnCastImage.setOnClickListener {
            rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe { grant: Boolean ->
                        if (grant) {
                            Matisse.from(this@SenderActivity)
                                    .choose(MimeType.ofImage())
                                    .countable(false)
                                    .showSingleMediaType(true)
                                    .maxSelectable(1)
                                    .theme(com.zhihu.matisse.R.style.Matisse_Dracula)
                                    .setOnChooseItemListener(mImageListener)
                                    .imageEngine(GlideEngine())
                                    .forResult(REQUEST_CODE_CHOOSE)
                        } else {
                            makeText(R.string.toast_grant_storage_perm)
                        }
                    }
        }
        mBinding.btnCastVideo.setOnClickListener {
            rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe { grant: Boolean ->
                        if (grant) {
                            Matisse.from(this@SenderActivity)
                                    .choose(MimeType.ofVideo())
                                    .showSingleMediaType(true)
                                    .countable(false)
                                    .maxSelectable(1)
                                    .theme(com.zhihu.matisse.R.style.Matisse_Dracula)
                                    .setOnChooseItemListener(mVideoListener)
                                    .imageEngine(GlideEngine())
                                    .forResult(REQUEST_CODE_CHOOSE)
                        } else {
                            makeText(R.string.toast_grant_storage_perm)
                        }
                    }
        }
        mBinding.btnCastMusic.setOnClickListener {
            rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe { grant: Boolean ->
                        if (grant) {
                            startActivity(Intent(this, MusicListActivity::class.java))
                        } else {
                            makeText(R.string.toast_grant_storage_perm)
                        }
                    }
        }
    }

    companion object {
        private val TAG = SenderActivity::class.java.simpleName
        private const val REQUEST_CODE_CHOOSE = 1001
    }
}