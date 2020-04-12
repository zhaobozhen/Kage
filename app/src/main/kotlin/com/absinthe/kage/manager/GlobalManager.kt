package com.absinthe.kage.manager

import androidx.lifecycle.MutableLiveData
import com.absinthe.kage.media.audio.LocalMusic
import java.util.ArrayList

object GlobalManager {

    var isServiceRunning: MutableLiveData<Boolean> = MutableLiveData()
    var musicList: MutableList<LocalMusic> = ArrayList()

}