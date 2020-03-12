package com.absinthe.kage.media.audio

import android.os.Parcel
import android.os.Parcelable
import com.absinthe.kage.media.LocalMedia

class LocalMusic : LocalMedia, Parcelable {
    var album: String? = null
    var albumId = 0
    var artist: String? = null
    var artistId = 0
    var coverPath: String? = null

    constructor()

    private constructor(`in`: Parcel) {
        title = `in`.readString()
        filePath = `in`.readString()
        date = `in`.readString()
        sortLetters = `in`.readString()
        type = `in`.readInt()
        duration = `in`.readLong()
        size = `in`.readFloat()
        album = `in`.readString()
        albumId = `in`.readInt()
        artist = `in`.readString()
        artistId = `in`.readInt()
        coverPath = `in`.readString()
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
        dest.writeString(album)
        dest.writeInt(albumId)
        dest.writeString(artist)
        dest.writeInt(artistId)
        dest.writeString(coverPath)
    }

    companion object {
        val CREATOR: Parcelable.Creator<LocalMusic> = object : Parcelable.Creator<LocalMusic> {
            override fun createFromParcel(`in`: Parcel): LocalMusic? {
                return LocalMusic(`in`)
            }

            override fun newArray(size: Int): Array<LocalMusic?> {
                return arrayOfNulls(size)
            }
        }
    }
}