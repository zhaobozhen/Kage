package com.absinthe.kage.connect;

import android.util.Log;

import com.absinthe.kage.protocol.IpMessageProtocol;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

public class UDP {
    private static final String TAG = UDP.class.getSimpleName();
    private static final byte[] DEFAULT_RECEIVE_BUFFER = new byte[1024];
    private final byte[] LOCK = new byte[0];

    private DatagramSocket mDatagramSocket;
    private ReceivePacketThread mReceivePacketThread;
    private String mLocalIpAddress;

    public UDP(String localIpAddress, int monitorPort) {
        mLocalIpAddress = localIpAddress;
        int port = monitorPort;

        while (true) {
            try {
                mDatagramSocket = new DatagramSocket(port);
                break;
            } catch (SocketException e) {
                Log.e(TAG, "new DatagramSocket error!" + e.toString());
                if (e instanceof BindException) {
                    if (port - monitorPort <= 20) {
                        port++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void notify(IpMessageProtocol ipMsgSend, String ip, int port) {
        Log.i(TAG, "ipMsgSend.getProtocolString() : " + ipMsgSend.getProtocolString() + ", ip = " + ip);

        byte[] buffer;
        buffer = ipMsgSend.getProtocolString().getBytes(StandardCharsets.UTF_8);
        InetAddress broadcastAddress;
        try {
            broadcastAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            Log.e(TAG, "mDatagramSocket.send error," + e.toString());
            return;
        }

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, port);
        try {
            mDatagramSocket.send(packet);
        } catch (IOException e) {
            Log.e(TAG, "mDatagramSocket.send error," + e.toString());
        }
    }

    public void stopReceive() {
        synchronized (LOCK) {
            if (mReceivePacketThread != null) {
                mReceivePacketThread.setStop(true);
                mReceivePacketThread = null;
            }
            if (mDatagramSocket != null && !mDatagramSocket.isClosed()) {
                mDatagramSocket.close();
            }
        }
    }

    public void startReceive(IUDPCallback callback) {
        synchronized (LOCK) {
            if (mReceivePacketThread != null) {
                mReceivePacketThread.setStop(true);
                mReceivePacketThread.interrupt();
            }
            mReceivePacketThread = new ReceivePacketThread(mDatagramSocket, mLocalIpAddress);
            mReceivePacketThread.setCallback(callback);
            mReceivePacketThread.start();
        }
    }

    private static class ReceivePacketThread extends Thread {
        private IUDPCallback mCallback;
        private boolean isStop = false;
        private DatagramSocket mDataGramSocket;
        private String mLocalIpAddress;

        public void setStop(boolean stop) {
            isStop = stop;
        }

        public boolean isStop() {
            return isStop;
        }

        ReceivePacketThread(DatagramSocket dataGramSocket, String localIpAddress) {
            this.mDataGramSocket = dataGramSocket;
            this.mLocalIpAddress = localIpAddress;
        }

        public void setCallback(IUDPCallback callback) {
            this.mCallback = callback;
        }

        @Override
        public void run() {
            byte[] buffer = DEFAULT_RECEIVE_BUFFER;
            int buffLen = buffer.length;
            DatagramPacket packet = new DatagramPacket(buffer, buffLen);

            while (!isStop()) {
                try {
                    mDataGramSocket.receive(packet);
                } catch (IOException e) {
                    Log.e(TAG, "mDatagramSocket.receive error," + e.getMessage());
                    break;
                }

                if (isStop()) {
                    break;
                }
                if (packet.getLength() <= 0) {
                    Log.w(TAG, "receive packet.getLength() = " + packet.getLength());
                    break;
                }

                byte[] data = packet.getData();
                String dataStr = new String(data, 0, packet.getLength());
                Log.d(TAG, "ReceivePacketThread:" + dataStr);

                InetAddress address = packet.getAddress();
                if (address == null) {
                    Log.e(TAG, "address == null");
                    break;
                }

                String ip = address.getHostAddress();
                int port = packet.getPort();
                if (!mLocalIpAddress.equals(ip) && !Const.LOCAL_IP_IN_AP.equals(ip) && mCallback != null) {
                    mCallback.onReceive(ip, port, dataStr);
                }
                packet.setLength(buffLen);
            }
        }
    }

    public interface IUDPCallback {
        void onReceive(String ip, int port, String data);
    }
}
