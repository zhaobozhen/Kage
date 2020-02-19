package com.absinthe.kage.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.OnLifecycleEvent;

import com.absinthe.kage.R;
import com.absinthe.kage.connect.proxy.ImageProxy;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.connect.protocol.Config;
import com.absinthe.kage.device.server.KageServer;
import com.absinthe.kage.ui.main.MainActivity;
import com.absinthe.kage.utils.NotificationUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPService extends LifecycleService {
    private ServerSocket mServerSocket;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, getNotificationInstance());

        DeviceManager deviceManager = DeviceManager.Singleton.INSTANCE.getInstance();
        deviceManager.init();
        addProxy(deviceManager);
        deviceManager.startMonitorDevice(2000);
        getLifecycle().addObserver(deviceManager);

        new Thread(() -> {
            try {
                mServerSocket = new ServerSocket(Config.PORT);
                while (true) {
                    Socket socket = mServerSocket.accept();
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    new Client(this, socket, dis, dos).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        KageServer kageServer = new KageServer();
        try {
            kageServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public static void start(Context context) {
        Intent intent = new Intent(context, TCPService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtils.createTCPChannel(context);
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public static void stop(Context context) {
        Intent intent = new Intent(context, TCPService.class);
        context.stopService(intent);
    }

    private Notification getNotificationInstance() {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, NotificationUtils.TCP_CHANNEL_ID)
                .setContentTitle(getText(R.string.notification_channel_tcp))
                .setContentText(getText(R.string.kage_service_notification_content))
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_stat_logo)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void addProxy(DeviceManager deviceManager) {
        deviceManager.addProxy(ImageProxy.getInstance());
    }
}
