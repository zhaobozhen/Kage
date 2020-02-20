package com.absinthe.kage.connect;

import android.util.Log;

import com.absinthe.kage.connect.protocol.IpMessageProtocol;

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
    private static final byte[] DEFAULT_RECEIVED_BUFFER = new byte[1024];

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
                Log.e(TAG, "Create DatagramSocket error!" + e.getMessage());
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
        Log.i(TAG, "ipMsgSend.getProtocolString(): " + ipMsgSend.getProtocolString() + ", IP = " + ip);

        byte[] buffer;
        buffer = ipMsgSend.getProtocolString().getBytes(StandardCharsets.UTF_8);
        InetAddress broadcastAddress;
        try {
            broadcastAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            Log.e(TAG, "DatagramSocket#send error: " + e.toString());
            return;
        }

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, port);
        try {
            mDatagramSocket.send(packet);
        } catch (IOException e) {
            Log.e(TAG, "DatagramSocket#send error: " + e.toString());
        }
    }

    public void stopReceive() {
        synchronized (UDP.class) {
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
        synchronized (UDP.class) {
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
        private DatagramSocket mDatagramSocket;
        private String mLocalIpAddress;
        private boolean isStop = false;

        public void setStop(boolean stop) {
            isStop = stop;
        }

        ReceivePacketThread(DatagramSocket datagramSocket, String localIpAddress) {
            this.mDatagramSocket = datagramSocket;
            this.mLocalIpAddress = localIpAddress;
        }

        public void setCallback(IUDPCallback callback) {
            this.mCallback = callback;
        }

        @Override
        public void run() {
            byte[] buffer = DEFAULT_RECEIVED_BUFFER;
            int buffLen = buffer.length;
            DatagramPacket packet = new DatagramPacket(buffer, buffLen);

            while (!isStop) {
                try {
                    mDatagramSocket.receive(packet);
                } catch (IOException e) {
                    Log.e(TAG, "DatagramSocket receive error:" + e.getMessage());
                    break;
                }

                if (isStop) {
                    break;
                }
                if (packet.getLength() <= 0) {
                    Log.w(TAG, "Receive packet.getLength() = " + packet.getLength());
                    break;
                }

                byte[] data = packet.getData();
                String dataStr = new String(data, 0, packet.getLength());
                Log.d(TAG, "ReceivePacketThread:" + dataStr);

                InetAddress address = packet.getAddress();
                if (address == null) {
                    Log.e(TAG, "Address == null");
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
