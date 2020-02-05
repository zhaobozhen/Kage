package com.absinthe.kage.server;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.absinthe.kage.protocol.Config;
import com.absinthe.kage.protocol.DataProtocol;
import com.absinthe.kage.utils.ToastUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConnectionServer {

    private static final String TAG = ConnectionServer.class.getSimpleName();
    private static boolean isStart = true;
    private static ServerResponseTask serverResponseTask;

    public ConnectionServer() {
    }

    public void start() {
        ServerSocket serverSocket = null;
        ExecutorService executorService = Executors.newCachedThreadPool();
        try {
            serverSocket = new ServerSocket(Config.PORT);
            while (isStart) {
                Socket socket = serverSocket.accept();
                serverResponseTask = new ServerResponseTask(socket,
                        new IResponseCallback() {

                            @Override
                            public void targetIsOffline(DataProtocol receiveMsg) {
                                if (receiveMsg != null) {
                                    Log.d(TAG, receiveMsg.getData());
                                }
                            }

                            @Override
                            public void targetIsOnline(final String clientIp) {
                                Log.d(TAG, clientIp + " is online");
                                Log.d(TAG, "-----------------------------------------");
                                new Handler(Looper.getMainLooper()).post(() -> ToastUtil.makeText(clientIp + " is online"));
                            }
                        });

                if (socket.isConnected()) {
                    executorService.execute(serverResponseTask);
                }
            }

            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    isStart = false;
                    serverSocket.close();
                    serverResponseTask.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        isStart = false;
    }
}