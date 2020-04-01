package com.absinthe.kage.media

import android.content.Context
import android.provider.MediaStore
import android.text.TextUtils
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

object MediaHelper {

    fun getMediaDirectory(context: Context, type: Int): List<MediaDirectory>? {
        if (type == TYPE_VIDEO) {
            return getVideoDirectory(context)
        } else if (type == TYPE_IMAGE) {
            return getImageDirectory(context)
        }
        return null
    }

    private fun getImageDirectory(context: Context): List<MediaDirectory> {
        val result: MutableList<MediaDirectory> = ArrayList()
        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val title = cursor.getString(cursor.getColumnIndex("title"))
                val dirId = cursor.getLong(cursor.getColumnIndex("bucket_id"))
                val dirName = cursor.getString(cursor.getColumnIndex("bucket_display_name"))
                val path = cursor.getString(cursor.getColumnIndex("_data"))

                if (!(TextUtils.isEmpty(title) || 0L == dirId || TextUtils.isEmpty(dirName))) {
                    val media = LocalMedia().apply {
                        this.title = title
                        this.filePath = path
                        this.type = TYPE_IMAGE
                    }
                    var flag = false

                    for (directory in result) {
                        if (directory.id == dirId && directory.name == dirName) {
                            directory.addMedia(media)
                            flag = true
                        }
                    }
                    if (!flag) {
                        val directory = MediaDirectory().apply {
                            id = dirId
                            name = dirName
                            type = TYPE_IMAGE
                            addMedia(media)
                        }
                        result.add(directory)
                    }
                }
            } while (cursor.moveToNext())

            cursor.close()
        }
        return result
    }

    private fun getVideoDirectory(context: Context): List<MediaDirectory> {
        val result: MutableList<MediaDirectory> = ArrayList()
        val cursor = context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val title = cursor.getString(cursor.getColumnIndex("title"))
                val dirId = cursor.getLong(cursor.getColumnIndex("bucket_id"))
                val dirName = cursor.getString(cursor.getColumnIndex("bucket_display_name"))
                val path = cursor.getString(cursor.getColumnIndex("_data"))
                if (!(TextUtils.isEmpty(title) || 0L == dirId || TextUtils.isEmpty(dirName))) {
                    val media = LocalMedia().apply {
                        this.title = title
                        this.type = TYPE_VIDEO
                        this.filePath = path
                    }
                    var flag = false

                    for (directory in result) {
                        if (directory.id == dirId && directory.name == dirName) {
                            directory.addMedia(media)
                            flag = true
                        }
                    }
                    if (!flag) {
                        val directory = MediaDirectory().apply {
                            id = dirId
                            name = dirName
                            type = TYPE_VIDEO
                            addMedia(media)
                        }
                        result.add(directory)
                    }
                }
            } while (cursor.moveToNext())
            
            cursor.close()
        }
        return result
    }

    fun millisecondToTimeString(time: Int): String {
        var thisTime = time
        thisTime /= 1000
        if (thisTime.toLong() == 0L) {
            return "00:00"
        }
        val hours = thisTime / 3600.toLong()
        val remainder = thisTime % 3600.toLong()
        val minutes = remainder / 60
        val secs = remainder % 60
        val stringBuilder = StringBuilder().apply {
            if (hours != 0L) {
                append(if (hours < 10) "0" else "")
                append(hours)
                append(":")
            }
            append(if (minutes < 10) "0" else "")
            append(minutes)
            append(":")
            append(if (secs < 10) "0" else "")
            append(secs)
        }


        return stringBuilder.toString()
    }

    fun encodePath(path: String?): String {
        val strs = path!!.split("/").toTypedArray()
        val builder = StringBuilder()
        try {
            for (str in strs) {
                if (!TextUtils.isEmpty(str)) {
                    builder.append("/")
                    builder.append(URLEncoder.encode(str, "utf-8"))
                }
            }
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return builder.toString()
    }
}