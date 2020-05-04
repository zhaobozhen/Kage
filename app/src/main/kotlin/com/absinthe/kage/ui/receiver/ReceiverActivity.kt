package com.absinthe.kage.ui.receiver

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.connect.proxy.BaseProxy
import com.absinthe.kage.connect.proxy.MODE_IMAGE
import com.absinthe.kage.databinding.ActivityReceiverBinding
import com.absinthe.kage.utils.AnimationUtil
import com.absinthe.kage.utils.AnimationUtil.showAndHiddenAnimation
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class ReceiverActivity : BaseActivity() {

    private lateinit var binding: ActivityReceiverBinding

    override fun setViewBinding() {
        binding = ActivityReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setToolbar() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        processIntent(intent)
        BaseProxy.CURRENT_MODE = MODE_IMAGE
    }

    override fun onNewIntent(intent: Intent) {
        processIntent(intent)
        super.onNewIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        if (intent != null) {
            val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)
            if (!TextUtils.isEmpty(imageUri)) {
                if (imageUri == EXTRA_FINISH) {
                    finish()
                } else {
                    loadImage(imageUri)
                }
            }
        }
    }

    private fun loadImage(imageUri: String?) {
        showLoading()

        Glide.with(this)
                .load(imageUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .thumbnail(0.1f)
                .into(object : CustomTarget<Drawable?>() {

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable?>?) {
                        binding.imageView.setImageDrawable(resource)
                        hideLoading()
                    }
                })
    }

    private fun showLoading() {
        showAndHiddenAnimation(binding.layoutLoading.root, AnimationUtil.AnimationState.STATE_SHOW, 300)
    }

    private fun hideLoading() {
        showAndHiddenAnimation(binding.layoutLoading.root, AnimationUtil.AnimationState.STATE_GONE, 300)
    }

    companion object {
        const val EXTRA_IMAGE_URI = "IMAGE_URI"
        const val EXTRA_FINISH = "FINISH"
    }
}