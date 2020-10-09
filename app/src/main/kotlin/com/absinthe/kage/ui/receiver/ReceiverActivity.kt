package com.absinthe.kage.ui.receiver

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.R
import com.absinthe.kage.connect.proxy.BaseProxy
import com.absinthe.kage.connect.proxy.MODE_IMAGE
import com.absinthe.kage.databinding.ActivityReceiverBinding
import com.absinthe.kage.utils.AnimationUtil
import com.absinthe.kage.utils.AnimationUtil.showAndHiddenAnimation
import com.absinthe.kage.utils.ToastUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class ReceiverActivity : BaseActivity() {

    private lateinit var binding: ActivityReceiverBinding
    private var imageUri: String = ""

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

        binding.imageView.setSingleTapListener {
            AlertDialog.Builder(this)
                    .setTitle(R.string.title_save_media)
                    .setMessage(R.string.message_save_media)
                    .setPositiveButton(android.R.string.ok) { _, _ -> saveImage(imageUri) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        processIntent(intent)
        super.onNewIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        if (intent != null) {
            imageUri = intent.getStringExtra(EXTRA_IMAGE_URI) ?: ""
            if (!TextUtils.isEmpty(imageUri)) {
                if (imageUri == EXTRA_FINISH) {
                    finish()
                } else {
                    loadImage(imageUri)
                }
            }
        }
    }

    private fun loadImage(imageUri: String) {
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

    private fun saveImage(imageUri: String) = GlobalScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            ToastUtil.makeText(R.string.saving)
        }
        val bitmap = returnBitMap(imageUri)
        val imageName = imageUri.split(File.separator).last()
        val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + getString(R.string.app_name))
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let {
                contentResolver.openOutputStream(it)
            }
        } else {
            val file = File(Environment.DIRECTORY_PICTURES + File.separator + "Kage", imageName)
            FileOutputStream(file)
        }

        try {
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos?.flush()
            fos?.close()

            withContext(Dispatchers.Main) {
                ToastUtil.makeText(R.string.save_success)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                ToastUtil.makeText("Error")
            }
        }
    }

    private fun returnBitMap(url: String): Bitmap? {
        val myFileUrl: URL
        var bitmap: Bitmap? = null

        try {
            myFileUrl = URL(url)
            val conn: HttpURLConnection = myFileUrl.openConnection() as HttpURLConnection
            conn.doInput = true
            conn.connect()
            val `is`: InputStream = conn.inputStream
            bitmap = BitmapFactory.decodeStream(`is`)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmap
    }

    companion object {
        const val EXTRA_IMAGE_URI = "IMAGE_URI"
        const val EXTRA_FINISH = "FINISH"
    }
}