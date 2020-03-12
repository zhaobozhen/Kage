package com.absinthe.kage.media

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class MediaDirectory : Parcelable {

    private var mediaList: MutableList<LocalMedia>? = null
    var name: String? = null
    var id: Long = 0
    var type = 0

    internal constructor()

    private constructor(`in`: Parcel) {
        id = `in`.readLong()
        mediaList = `in`.createTypedArrayList(LocalMedia.CREATOR)
        name = `in`.readString()
        type = `in`.readInt()
    }

    val itemCount: Int
        get() {
            val list: List<LocalMedia>? = mediaList
            return list?.size ?: 0
        }

    fun addMedia(media: LocalMedia) {
        if (mediaList == null) {
            mediaList = ArrayList()
        }
        mediaList!!.add(media)
    }

    fun getMediaList(): List<LocalMedia>? {
        return mediaList
    }

    val firstMediaPath: String?
        get() {
            val list: List<LocalMedia>? = mediaList
            return if (list == null) {
                null
            } else {
                mediaList!![0].filePath
            }
        }

    override fun equals(other: Any?): Boolean {
        if (other !is MediaDirectory) {
            return false
        }
        if (this === other) {
            return true
        }
        return other.id == id && other.name == name
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeTypedList(mediaList)
        dest.writeString(name)
        dest.writeInt(type)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (mediaList?.hashCode() ?: 0)
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + type
        return result
    }

    companion object {
        val CREATOR: Parcelable.Creator<MediaDirectory> = object : Parcelable.Creator<MediaDirectory> {
            override fun createFromParcel(`in`: Parcel): MediaDirectory? {
                return MediaDirectory(`in`)
            }

            override fun newArray(size: Int): Array<MediaDirectory?> {
                return arrayOfNulls(size)
            }
        }
    }
}