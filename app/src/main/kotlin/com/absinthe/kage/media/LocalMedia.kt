package com.absinthe.kage.media

import android.os.Parcel
import android.os.Parcelable
import com.absinthe.kage.media.MediaHelper.encodePath
import com.blankj.utilcode.util.EncryptUtils

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

    constructor(parcel: Parcel) {
        title = parcel.readString()
        filePath = parcel.readString()
        date = parcel.readString()
        sortLetters = parcel.readString()
        type = parcel.readInt()
        duration = parcel.readLong()
        size = parcel.readFloat()
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

    companion object CREATOR : Parcelable.Creator<LocalMedia> {

        override fun createFromParcel(parcel: Parcel): LocalMedia {
            return LocalMedia(parcel)
        }

        override fun newArray(size: Int): Array<LocalMedia?> {
            return arrayOfNulls(size)
        }
    }
}