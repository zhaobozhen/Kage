package com.absinthe.kage.connect.tcp;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AbstractPacketWriter implements IPacketWriter {
    private static final String TAG = AbstractPacketWriter.class.getSimpleName();

    protected static final long DEFAULT_TIMEOUT = 5 * 1000;
    protected long timeout = DEFAULT_TIMEOUT;
    protected LinkedBlockingQueue<Packet> mPacketQueue = new LinkedBlockingQueue();
    protected boolean shutdown = false;

    public AbstractPacketWriter(final DataOutputStream out, final KageSocket.ISocketCallback socketCallback) {
        Thread thread = new Thread(() -> {
            while (!shutdown) {
                try {
                    Packet packet = mPacketQueue.poll(timeout, TimeUnit.MILLISECONDS);
                    if (shutdown) {
                        break;
                    }
                    if (null != packet) {
                        writeMyUTF(out, packet);
                    } else {
                        KageSocket.ISocketCallback.TCastSocketCallbackThreadHandler.getInstance().post(() -> {
                            if (null != socketCallback) {
                                socketCallback.onWriterIdle();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e(TAG, "send error:" + e.getMessage());
                    KageSocket.ISocketCallback.TCastSocketCallbackThreadHandler.getInstance().post(() -> {
                        if (null != socketCallback) {
                            socketCallback.onReadAndWriteError(KageSocket.ISocketCallback.WRITE_ERROR_CODE_CONNECT_UNKNOWN);
                        }
                    });
                }
            }
        });
        thread.start();
    }

    protected void writeMyUTF(DataOutputStream dos, Packet packet) throws IOException {
        String data = packet.getData();
        Log.d(TAG, "send data: " + data);
        byte[] bArray = data.getBytes(StandardCharsets.UTF_8);
        int sendLen = bArray.length;
        dos.writeInt(sendLen);
        dos.flush();
        dos.write(bArray, 0, sendLen);
        dos.flush();
    }

    @Override
    public void writePacket(Packet packet) {
        try {
            mPacketQueue.put(packet);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        this.shutdown = true;
        mPacketQueue.clear();
    }
}
