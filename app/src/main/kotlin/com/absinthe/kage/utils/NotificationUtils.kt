package com.absinthe.kage.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.absinthe.kage.R

object NotificationUtils {

    const val TCP_CHANNEL_ID = "tcp_channel"

    @JvmStatic
    fun createTCPChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = context.getText(R.string.notification_channel_tcp)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(TCP_CHANNEL_ID, name, importance)

            mChannel.setShowBadge(false)
            mChannel.setSound(null, null)
            manager.createNotificationChannel(mChannel)
        }
    }
}