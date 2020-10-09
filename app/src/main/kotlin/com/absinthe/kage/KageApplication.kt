package com.absinthe.kage

import android.app.Application
import com.absinthe.kage.connect.Const
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

        if (BuildConfig.DEBUG) {
            Timber.plant(ThreadAwareDebugTree())
        } else {
            Timber.plant(ReleaseTree())
            AppCenter.start(this, Const.APP_CENTER_SECRET,
                    Analytics::class.java, Crashes::class.java)
        }

        if (!ServiceUtils.isServiceRunning(TCPService::class.java)) {
            TCPService.start(this)
        }
    }
}