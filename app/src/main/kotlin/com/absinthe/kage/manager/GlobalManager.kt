package com.absinthe.kage.manager

import androidx.lifecycle.MutableLiveData

object GlobalManager {

    var isServiceRunning: MutableLiveData<Boolean> = MutableLiveData()

}