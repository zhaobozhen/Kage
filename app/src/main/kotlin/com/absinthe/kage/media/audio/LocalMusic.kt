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

    constructor(parcel: Parcel) {
        title = parcel.readString()
        filePath = parcel.readString()
        date = parcel.readString()
        sortLetters = parcel.readString()
        type = parcel.readInt()
        duration = parcel.readLong()
        size = parcel.readFloat()
        album = parcel.readString()
        albumId = parcel.readInt()
        artist = parcel.readString()
        artistId = parcel.readInt()
        coverPath = parcel.readString()
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

    companion object CREATOR : Parcelable.Creator<LocalMusic> {
        override fun createFromParcel(parcel: Parcel): LocalMusic {
            return LocalMusic(parcel)
        }

        override fun newArray(size: Int): Array<LocalMusic?> {
            return arrayOfNulls(size)
        }
    }

}