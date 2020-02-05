package com.absinthe.kage.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.absinthe.kage.R;
import com.absinthe.kage.server.ConnectionServer;
import com.absinthe.kage.utils.NotificationUtils;

public class TCPService extends Service {
    private ConnectionServer mConnectionServer;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, getNotificationInstance());

        new Thread(() -> {
            mConnectionServer = new ConnectionServer();
            mConnectionServer.start();
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        mConnectionServer.stop();
        super.onDestroy();
    }

    private Notification getNotificationInstance() {
        return new NotificationCompat.Builder(this, NotificationUtils.TCP_CHANNEL_ID)
                .setContentTitle(getText(R.string.notification_channel_tcp))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
