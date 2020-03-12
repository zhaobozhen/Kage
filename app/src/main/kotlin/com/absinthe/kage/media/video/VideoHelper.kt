package com.absinthe.kage.media.video

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.provider.MediaStore
import com.blankj.utilcode.util.FileUtils

object VideoHelper {
    fun getVideoCoverImage(filePath: String?): Bitmap? {
        return if (filePath != null && FileUtils.isFileExists(filePath)) {
            ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND)
        } else null
    }
}