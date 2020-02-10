package com.absinthe.kage.server;

import android.util.Log;

import com.absinthe.kage.protocol.Config;
import com.absinthe.kage.protocol.DataProtocol;

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
        isStart = true;
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
                            }
                        });

                if (socket.isConnected()) {
                    executorService.execute(serverResponseTask);
                }
            }

            serverSocket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        isStart = false;
    }
}