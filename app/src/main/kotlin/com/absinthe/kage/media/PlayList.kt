package com.absinthe.kage.media

import android.os.Parcel
import android.os.Parcelable
import timber.log.Timber
import java.util.*

class PlayList : Parcelable {
    private var mCurrentIndex = -1
    private var mList: MutableList<LocalMedia> = ArrayList()

    constructor()

    constructor(`in`: Parcel) {
        mCurrentIndex = `in`.readInt()
        mList = `in`.createTypedArrayList(LocalMedia.CREATOR)!!
    }

    var currentIndex: Int
        get() = mCurrentIndex
        set(currentIndex) {
            mCurrentIndex = if (currentIndex < 0 || currentIndex > mList.size) {
                0
            } else {
                currentIndex
            }
        }

    val currentMedia: LocalMedia?
        get() = if (mCurrentIndex < 0 || mCurrentIndex >= mList.size) {
            null
        } else mList[mCurrentIndex]

    fun hasNextMedia(): Boolean {
        return mCurrentIndex >= 0 && mCurrentIndex < mList.size - 1
    }

    fun hasPreviousMedia(): Boolean {
        return mCurrentIndex > 0
    }

    fun getPreviousMedia(isRepeat: Boolean, isShuffled: Boolean): LocalMedia? {
        if (!hasPreviousMedia() || mCurrentIndex == -1) {
            return null
        }
        if (isShuffled) {
            val size = mList.size.toDouble()
            val random = Math.random()
            mCurrentIndex = (size * random).toInt()
        } else if (mCurrentIndex != 0) {
            mCurrentIndex--
        } else if (!isRepeat) {
            return null
        } else {
            mCurrentIndex = mList.size - 1
        }
        return mList[mCurrentIndex]
    }

    fun getNextMedia(isRepeat: Boolean, isShuffled: Boolean): LocalMedia? {
        if (!hasNextMedia() || mCurrentIndex == -1) {
            return null
        }
        if (isShuffled) {
            val size = mList.size.toDouble()
            val random = Math.random()
            mCurrentIndex = (size * random).toInt()
        } else if (mCurrentIndex != mList.size - 1) {
            mCurrentIndex++
        } else if (!isRepeat) {
            return null
        } else {
            mCurrentIndex = 0
        }
        return mList[mCurrentIndex]
    }

    fun getMedia(pos: Int): LocalMedia? {
        if (pos < 0 || pos >= mList.size) {
            return null
        }
        mCurrentIndex = pos
        return mList[pos]
    }

    fun setList(list: List<LocalMedia>) {
        setList(list, mCurrentIndex)
    }

    fun setList(list: List<LocalMedia>, index: Int) {
        mList.clear()
        mList.addAll(list)
        currentIndex = index
    }

    val list: List<LocalMedia>
        get() = mList

    @JvmOverloads
    fun addNextMedia(localMedia: LocalMedia, isMulti: Boolean = false) {
        addMedia(mCurrentIndex + 1, localMedia, isMulti)
    }

    fun addMedia(localMedia: LocalMedia) {
        addMedia(mList.size, localMedia, false)
    }

    fun addMediaToTop(localMedia: LocalMedia) {
        addMedia(0, localMedia, true)
    }

    private fun addMedia(position: Int, localMedia: LocalMedia, isMulti: Boolean) {
        if (position > mList.size || position < mCurrentIndex + 1) {
            Timber.e("position IndexOutOfBounds")
            return
        }
        if (isMulti) {
            mList.add(position, localMedia)
        } else {
            val index = mList.indexOf(localMedia)
            when {
                index < 0 -> {
                    mList.add(position, localMedia)
                }
                index != position -> {
                    mList.removeAt(index)
                    mList.add(position, localMedia)
                }
                else -> {
                    Collections.swap(mList, position, index)
                }
            }
        }
        if (mCurrentIndex == -1) {
            mCurrentIndex = 0
        }
    }

    fun queryMediaIndex(localMedia: LocalMedia?): Int {
        return mList.indexOf(localMedia)
    }

    fun clearPlaylist() {
        mList.clear()
        mCurrentIndex = -1
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(mCurrentIndex)
        dest.writeTypedList(mList)
    }

    companion object CREATOR : Parcelable.Creator<PlayList> {
        override fun createFromParcel(`in`: Parcel): PlayList {
            return PlayList(`in`)
        }

        override fun newArray(size: Int): Array<PlayList?> {
            return arrayOfNulls(size)
        }
    }
}