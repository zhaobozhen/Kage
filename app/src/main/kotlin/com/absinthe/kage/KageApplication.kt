package com.absinthe.kage

import android.app.Application
import android.content.Context
import com.absinthe.kage.service.TCPService
import com.absinthe.kage.utils.timber.ReleaseTree
import com.absinthe.kage.utils.timber.ThreadAwareDebugTree
import com.blankj.utilcode.util.ServiceUtils
import timber.log.Timber


class KageApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        sContext = this

        if (BuildConfig.DEBUG) {
            Timber.plant(ThreadAwareDebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        if (!ServiceUtils.isServiceRunning(TCPService::class.java)) {
            TCPService.start(this)
        }
    }

    companion object {
        lateinit var sContext: Context
    }
}