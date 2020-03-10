package com.absinthe.kage

import android.app.Application
import android.content.Context
import com.absinthe.kage.Settings.deviceNecessary
import com.absinthe.kage.service.TCPService
import com.absinthe.kage.utils.Logger
import com.blankj.utilcode.util.ServiceUtils

class KageApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        sContext = this
        Logger.isDebugMode = BuildConfig.DEBUG
        deviceNecessary = false

        if (!ServiceUtils.isServiceRunning(TCPService::class.java)) {
            TCPService.start(this)
        }
    }

    companion object {
        lateinit var sContext: Context
    }
}