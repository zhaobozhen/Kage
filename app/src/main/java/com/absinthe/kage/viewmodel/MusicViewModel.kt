package com.absinthe.kage.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.absinthe.kage.media.audio.LocalMusic
import com.absinthe.kage.media.audio.MusicHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    val musicList = MutableLiveData<List<LocalMusic>>()

    fun loadMusic(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val list = MusicHelper.getAllLocalMusic(context)
            withContext(Dispatchers.Main) {
                musicList.setValue(list)
            }
        }
    }
}