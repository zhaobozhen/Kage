package com.absinthe.kage.connect.tcp;

import android.util.Log;

import com.absinthe.kage.protocol.IpMessageConst;
import com.absinthe.kage.protocol.IpMessageProtocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PacketReader implements IPacketReader {
    private static final String TAG = PacketReader.class.getSimpleName();
    private static final long DEFAULT_TIMEOUT = 5 * 1000;
    private static final long MAX_READ_LENGTH = 223 * 10000;//一次性最大读取指令的长度，超出将可能OOM

    private Map<String, Request> mRequests = new TreeMap<>();
    private DataInputStream mIn;
    private KageSocket.ISocketCallback mSocketCallback;
    private LinkedBlockingQueue<Packet> mPackets = new LinkedBlockingQueue<>();
    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    private long timeout = DEFAULT_TIMEOUT;
    private boolean shutdown;

    public PacketReader(DataInputStream mIn, final KageSocket.ISocketCallback socketCallback) {
        this.mIn = mIn;
        this.mSocketCallback = socketCallback;
        new ReceiveDataThread().start();

        new Thread(() -> {
            try {
                while (!shutdown) {
                    Packet packet = mPackets.poll(timeout, TimeUnit.MILLISECONDS);
                    synchronized (PacketReader.class) {
                        if (shutdown) {
                            break;
                        }
                        if (null == packet) {
                            KageSocket.ISocketCallback.TCastSocketCallbackThreadHandler.getInstance().post(() -> {
                                if (null != mSocketCallback) {
                                    mSocketCallback.onReaderIdle();
                                }
                            });
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void addRequest(Request request) {
        synchronized (PacketReader.class) {
            mRequests.put(request.getId(), request);
        }
    }

    @Override
    public void shutdown() {
        synchronized (PacketReader.class) {
            shutdown = true;
            mExecutorService.shutdownNow();
            mRequests.clear();
        }
        mPackets.clear();
    }

    private class ReceiveDataThread extends Thread {

        @Override
        public void run() {
            while (!shutdown) {
                if (mIn != null) {
                    final String data;
                    try {
                        data = readFromStream(mIn);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        Log.d(TAG, "PacketReader error");
                        if (mSocketCallback != null) {
                            mSocketCallback.onReadAndWriteError(KageSocket.ISocketCallback.READ_ERROR_CODE_RECEIVE_LENGTH_TOO_BIG);
                        }
                        return;
                    }
                    synchronized (PacketReader.class) {
                        if (shutdown) {
                            break;
                        }
                        if (data == null) {
                            Log.e(TAG, "ReceiveDataThread receive data == null" + ", thread :" + Thread.currentThread().getName());
                            KageSocket.ISocketCallback.TCastSocketCallbackThreadHandler.getInstance().post(() -> {
                                if (mSocketCallback != null) {
                                    mSocketCallback.onReadAndWriteError(KageSocket.ISocketCallback.READ_ERROR_CODE_CONNECT_UNKNOWN);
                                }
                            });
                            break;
                        }
                        if (data.isEmpty()) {
                            continue;
                        }
                        Log.d(TAG, "receive Data: " + data);
                        mExecutorService.submit(() -> {
                            Packet packet = new Packet();
                            packet.setData(data);
                            try {
                                mPackets.put(packet);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                        responseAllHeartBeat();//收到任何数据都消费掉所有的心跳超时
                        if (!isHeartBeat(data)) {
                            KageSocket.ISocketCallback.TCastSocketCallbackThreadHandler.getInstance().post(() -> {
                                if (mSocketCallback != null) {
                                    mSocketCallback.onReceiveMsg(data);
                                }
                            });
                        }
                    }
                }
            }
        }

        private String readFromStream(DataInputStream dis) throws IllegalArgumentException {
            byte[] bArray = readNextPacket(dis);
            if (null == bArray) {
                return null;
            }
            return new String(bArray, StandardCharsets.UTF_8);
        }

    }

    /**
     * 响应所有心跳
     */
    private void responseAllHeartBeat() {
        Map<String, Request> tempMap = new TreeMap<>();
        synchronized (PacketReader.class) {
            if (!mRequests.isEmpty()) {
                tempMap.putAll(mRequests);
                mRequests.clear();
            }
        }
        if (!tempMap.isEmpty()) {
            Set<Map.Entry<String, Request>> entries = tempMap.entrySet();
            Iterator<Map.Entry<String, Request>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, Request> next = iterator.next();
                iterator.remove();
                mExecutorService.submit(() -> {
                    Response response = new Response();
                    next.getValue().setResponse(response);
                });
            }
        }
    }

    private byte[] readNextPacket(DataInputStream dis) throws IllegalArgumentException {
        int receiveLength;
        try {
            receiveLength = dis.readInt();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "readNextPacket IO : " + e.getMessage());
            return null;
        }
        Log.d(TAG, "receiveLength = " + receiveLength);
        if (receiveLength >= MAX_READ_LENGTH) {
            throw new IllegalArgumentException("receiveLength too big, receiveLength = " + receiveLength);
        }
        byte[] bArray = new byte[receiveLength];
        int bytesRead = 0;
        while (bytesRead < receiveLength) {
            try {
                int result = dis.read(bArray, bytesRead, receiveLength - bytesRead);
                if (result == -1) break;
                bytesRead += result;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return bArray;
    }

    private boolean isHeartBeat(String data) {
        String[] split = data.split(IpMessageProtocol.DELIMITER);
        if (split.length < 1) {
            return false;
        }
        int cmd = Integer.parseInt(split[0]);
        return IpMessageConst.IS_ONLINE == cmd;
    }
}

