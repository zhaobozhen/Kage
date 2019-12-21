package com.absinthe.kage.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.absinthe.kage.server.ConnectionServer;

public class TCPService extends Service {
    private ConnectionServer mConnectionServer;

    @Override
    public void onCreate() {
        super.onCreate();

        new Thread(new Runnable() {
            @Override
            public void run() {
                mConnectionServer = new ConnectionServer();
                mConnectionServer.start();
            }
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
}
