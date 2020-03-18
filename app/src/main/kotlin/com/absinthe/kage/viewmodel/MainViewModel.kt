package com.absinthe.kage.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.absinthe.kage.service.TCPService
import com.blankj.utilcode.util.ServiceUtils

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var isServiceRunning: MutableLiveData<Boolean> = MutableLiveData()
    var isDeviceConnected: MutableLiveData<Boolean> = MutableLiveData()

    fun startService(context: Context) {
        if (!ServiceUtils.isServiceRunning(TCPService::class.java)) {
            TCPService.start(context)
        }
        isServiceRunning.value = true
    }

    fun stopService(context: Context) {
        if (ServiceUtils.isServiceRunning(TCPService::class.java)) {
            TCPService.stop(context)
        }
        isServiceRunning.value = false
    }
}