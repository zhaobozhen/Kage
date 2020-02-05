package com.absinthe.kage.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.absinthe.kage.R;

public class NotificationUtils {
    public static final String TCP_CHANNEL_ID = "tcp_channel";

    public static void createTCPChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            CharSequence name = context.getText(R.string.notification_channel_tcp);
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(TCP_CHANNEL_ID, name, importance);
            mChannel.setShowBadge(false);
            mChannel.setSound(null, null);
            if (manager != null) {
                manager.createNotificationChannel(mChannel);
            }
        }
    }
}
