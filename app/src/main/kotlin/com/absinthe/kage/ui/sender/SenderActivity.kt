package com.absinthe.kage.ui.sender

import android.Manifest
import android.content.Intent
import android.os.Bundle
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.R
import com.absinthe.kage.connect.proxy.ImageProxy
import com.absinthe.kage.databinding.ActivitySenderBinding
import com.absinthe.kage.media.LocalMedia
import com.absinthe.kage.media.TYPE_VIDEO
import com.absinthe.kage.ui.connect.ConnectActivity
import com.absinthe.kage.ui.media.VideoActivity
import com.absinthe.kage.utils.ToastUtil.makeText
import com.microsoft.appcenter.analytics.Analytics
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zhihu.matisse.Matisse
import com.zhihu.matisse.MimeType
import com.zhihu.matisse.engine.impl.GlideEngine
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter
import com.zhihu.matisse.internal.utils.PathUtils
import com.zhihu.matisse.listener.OnChooseItemListener

class SenderActivity : BaseActivity() {

    private lateinit var mBinding: ActivitySenderBinding
    private lateinit var mImageListener: OnChooseItemListener
    private lateinit var mVideoListener: AlbumMediaAdapter.OnMediaClickListener
    private val rxPermissions = RxPermissions(this)

    override fun setViewBinding() {
        mBinding = ActivitySenderBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
    }

    override fun setToolbar() {
        mToolbar = mBinding.toolbar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initView()
    }

    override fun onDestroy() {
        ImageProxy.close()
        super.onDestroy()
    }

    private fun initView() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mImageListener = object : OnChooseItemListener {

            override fun onChoose(itemUri: String) {
                ImageProxy.cast(itemUri)
            }

            override fun onStop() {
                ImageProxy.stop()
                ImageProxy.close()
            }

            override fun onNotConnect() {
                startActivity(Intent(this@SenderActivity, ConnectActivity::class.java))
                finish()
            }

            override fun onPreview() {
            }

            override fun onNext() {
            }
        }

        mVideoListener = AlbumMediaAdapter.OnMediaClickListener { _, item, _ ->
            val localMedia = LocalMedia().apply {
                type = TYPE_VIDEO
                title = "Unknown"
                filePath = PathUtils.getPath(this@SenderActivity, item?.uri)
            }

            val intent = Intent(this@SenderActivity, VideoActivity::class.java).apply {
                putExtra(VideoActivity.EXTRA_MEDIA, localMedia)
                putExtra(VideoActivity.EXTRA_TYPE, VideoActivity.TYPE_SENDER)
            }
            startActivity(intent)
        }

        mBinding.cardImage.setOnClickListener {
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
            Analytics.trackEvent("Cast Image clicked")
        }
        mBinding.cardVideo.setOnClickListener {
            rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe { grant: Boolean ->
                        if (grant) {
                            Matisse.from(this@SenderActivity)
                                    .choose(MimeType.ofVideo())
                                    .showSingleMediaType(true)
                                    .countable(false)
                                    .maxSelectable(1)
                                    .theme(com.zhihu.matisse.R.style.Matisse_Dracula)
                                    .setOnMediaClickListener(mVideoListener)
                                    .imageEngine(GlideEngine())
                                    .forResult(REQUEST_CODE_CHOOSE)
                        } else {
                            makeText(R.string.toast_grant_storage_perm)
                        }
                    }
            Analytics.trackEvent("Cast Video clicked")
        }
        mBinding.cardMusic.setOnClickListener {
            rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe { grant: Boolean ->
                        if (grant) {
                            startActivity(Intent(this, MusicListActivity::class.java))
                        } else {
                            makeText(R.string.toast_grant_storage_perm)
                        }
                    }
            Analytics.trackEvent("Cast Music clicked")
        }
    }

    companion object {
        private const val REQUEST_CODE_CHOOSE = 1001
    }
}