package com.absinthe.kage.media

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import com.blankj.utilcode.util.EncryptUtils
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.util.*

const val TYPE_IMAGE = 1
const val TYPE_VIDEO = 2
const val TYPE_AUDIO = 3

open class LocalMedia : Parcelable {
    var title: String? = null
    var filePath: String? = null
    var date: String? = null
    var sortLetters: String? = null

    var type = 0
    var size = 0f
    var duration: Long = 0

    constructor()

    constructor(`in`: Parcel) {
        title = `in`.readString()
        filePath = `in`.readString()
        date = `in`.readString()
        sortLetters = `in`.readString()
        type = `in`.readInt()
        duration = `in`.readLong()
        size = `in`.readFloat()
    }

    val mediaKey: String
        get() = EncryptUtils.encryptMD5ToString(filePath)

    val url: String
        get() = encodePath(filePath)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other !is LocalMedia) {
            false
        } else mediaKey == other.mediaKey
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeString(filePath)
        dest.writeString(date)
        dest.writeString(sortLetters)
        dest.writeInt(type)
        dest.writeLong(duration)
        dest.writeFloat(size)
    }

    override fun hashCode(): Int {
        var result = title?.hashCode() ?: 0
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + (sortLetters?.hashCode() ?: 0)
        result = 31 * result + type
        result = 31 * result + duration.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }

    companion object {
        private val TAG = LocalMedia::class.java.simpleName

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
                        val info = "Image dir: " + dirId +
                                ", name: " + dirName +
                                ", path: " + path
                        Log.i(TAG, info)
                        val media = LocalMedia()
                        media.title = title
                        media.filePath = path
                        media.type = TYPE_IMAGE
                        var flag = false
                        for (directory in result) {
                            if (directory.id == dirId && directory.name == dirName) {
                                directory.addMedia(media)
                                flag = true
                            }
                        }
                        if (!flag) {
                            val directory = MediaDirectory()
                            directory.id = dirId
                            directory.name = dirName
                            directory.type = TYPE_IMAGE
                            directory.addMedia(media)
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
                        val info = "Video dir: " + dirId +
                                ", name: " + dirName +
                                ", path: " + path
                        Log.i(TAG, info)
                        val media = LocalMedia()
                        media.title = title
                        media.type = TYPE_VIDEO
                        media.filePath = path
                        var flag = false
                        for (directory in result) {
                            if (directory.id == dirId && directory.name == dirName) {
                                directory.addMedia(media)
                                flag = true
                            }
                        }
                        if (!flag) {
                            val directory = MediaDirectory()
                            directory.id = dirId
                            directory.name = dirName
                            directory.type = TYPE_VIDEO
                            directory.addMedia(media)
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
            val stringBuilder = StringBuilder()
            if (hours != 0L) {
                stringBuilder.append(if (hours < 10) "0" else "")
                stringBuilder.append(hours)
                stringBuilder.append(":")
            }
            stringBuilder.append(if (minutes < 10) "0" else "")
            stringBuilder.append(minutes)
            stringBuilder.append(":")
            stringBuilder.append(if (secs < 10) "0" else "")
            stringBuilder.append(secs)
            return stringBuilder.toString()
        }

        private fun encodePath(path: String?): String {
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

        val CREATOR: Parcelable.Creator<LocalMedia> = object : Parcelable.Creator<LocalMedia> {
            override fun createFromParcel(`in`: Parcel): LocalMedia? {
                return LocalMedia(`in`)
            }

            override fun newArray(size: Int): Array<LocalMedia?> {
                return arrayOfNulls(size)
            }
        }
    }
}