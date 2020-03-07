package com.absinthe.kage

import android.app.Application
import android.content.Context
import com.absinthe.kage.Settings.deviceNecessary
import com.absinthe.kage.utils.Logger

class KageApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        sContext = this
        Logger.isDebugMode = BuildConfig.DEBUG
        deviceNecessary = false
    }

    companion object {
        lateinit var sContext: Context
    }
}