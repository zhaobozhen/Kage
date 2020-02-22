package com.absinthe.kage.device.client;

import android.content.Context;
import android.util.Log;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.cmd.DeviceRotationCommand;
import com.absinthe.kage.device.cmd.HeartbeatCommand;
import com.absinthe.kage.device.cmd.ImageInfoCommand;
import com.absinthe.kage.device.cmd.InquiryDeviceInfoCommand;
import com.absinthe.kage.device.cmd.MediaPreparePlayCommand;
import com.absinthe.kage.device.cmd.PromptPhoneConnectedCommand;
import com.absinthe.kage.device.cmd.StopCommand;
import com.absinthe.kage.device.model.DeviceInfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client extends Thread implements Runnable {
    private static final String TAG = Client.class.getSimpleName();

    private Context mContext;
    private Socket mSocket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private DeviceInfo mDeviceInfo;
    private boolean isFirstCmd;

    public Client(Context context, Socket s, DataInputStream is, DataOutputStream os) {
        mContext = context;
        mSocket = s;
        dis = is;
        dos = os;
        isFirstCmd = true;
    }

    @Override
    public void run() {
        while (true) {
            String command = readToStream(dis, Client.this);
            Log.i(TAG, "Received command: " + command);

            if (command == null) {
                offline(mSocket, dis, dos);
                break;
            }
            if (command.isEmpty()) {
                continue;
            }

            int commandNum = 0;
            try {
                commandNum = Integer.parseInt(command.split(IpMessageProtocol.DELIMITER)[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            switch (commandNum) {
                case IpMessageConst.IS_ONLINE:
                    new HeartbeatCommand().doWork(this, command);
                    break;
                case IpMessageConst.GET_DEVICE_INFO:
                    new InquiryDeviceInfoCommand().doWork(this, command);
                    break;
                case IpMessageConst.PROMPT_PHONE_CONNECT:
                    new PromptPhoneConnectedCommand().doWork(this, command);
                    break;
                case IpMessageConst.MEDIA_STOP:
                    new StopCommand().doWork(this, command);
                    break;
                case IpMessageConst.MEDIA_PREPARE_PLAY:
                    new MediaPreparePlayCommand().doWork(this, command);
                    break;
                case IpMessageConst.MEDIA_IMAGE_INFO:
                    new ImageInfoCommand().doWork(this, command);
                    break;
                case IpMessageConst.DEVICE_ROTATION:
                    new DeviceRotationCommand().doWork(this, command);
                    break;
                default:
            }
        }
    }

    public Context getContext() {
        return mContext;
    }

    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    public void setDeviceInfo(DeviceInfo mDeviceInfo) {
        this.mDeviceInfo = mDeviceInfo;
    }

    public synchronized void writeToStream(String str) throws IOException {
        writeToStream(dos, str);
    }

    public void offline() {
        offline(mSocket, dis, dos);
    }

    private String readToStream(DataInputStream dis, Client client) {
        Log.i(TAG, "readToStream: isFirstCmd = " + client.isFirstCmd);
        int receivedLen;

        try {
            receivedLen = dis.readInt();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        if (receivedLen <= 0) {
            return "";
        }

        byte[] bArray;
        try {
            bArray = new byte[receivedLen];
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return "";
        }

        int bytesRead = 0;
        while (bytesRead < receivedLen) {
            try {
                int result = dis.read(bArray, bytesRead, receivedLen - bytesRead);
                if (result == -1)
                    break;
                bytesRead += result;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        String enStr;
        try {
            if (client.isFirstCmd) {
                client.isFirstCmd = false;
            }
            enStr = new String(bArray, StandardCharsets.UTF_8);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return "";
        }
        return enStr;
    }

    private synchronized void writeToStream(DataOutputStream dos, String str) throws IOException {
        Log.i(TAG, "writeToStream sendStr: " + str);
        byte[] bArray = str.getBytes(StandardCharsets.UTF_8);
        int sendLen = bArray.length;
        dos.writeInt(sendLen);
        dos.flush();
        dos.write(bArray, 0, sendLen);
    }

    private void offline(Socket socket, DataInputStream dis, DataOutputStream dos) {
        Log.i(TAG, "Offline: " + socket.getInetAddress());
        synchronized (Client.class) {
            try {
                if (dis != null) {
                    dis.close();
                }
                if (dos != null) {
                    dos.close();
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
