package com.absinthe.kage.media.video

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.provider.MediaStore
import com.blankj.utilcode.util.FileUtils

object VideoHelper {
    @JvmStatic
    fun getVideoCoverImage(filePath: String): Bitmap? {
        return if (FileUtils.isFileExists(filePath)) {
            ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND)
        } else null
    }
}