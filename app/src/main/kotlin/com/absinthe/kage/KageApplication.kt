package com.absinthe.kage

import android.app.Application
import android.content.Context
import com.absinthe.kage.service.TCPService
import com.absinthe.kage.utils.timber.ReleaseTree
import com.absinthe.kage.utils.timber.ThreadAwareDebugTree
import com.blankj.utilcode.util.ServiceUtils
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import timber.log.Timber


class KageApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        sContext = this

        if (BuildConfig.DEBUG) {
            Timber.plant(ThreadAwareDebugTree())
        } else {
            Timber.plant(ReleaseTree())
            AppCenter.start(this, "4b4faea6-9eed-4c30-a734-3fb9330da2cc",
                    Analytics::class.java, Crashes::class.java)
        }

        if (!ServiceUtils.isServiceRunning(TCPService::class.java)) {
            TCPService.start(this)
        }
    }

    companion object {
        lateinit var sContext: Context
    }
}