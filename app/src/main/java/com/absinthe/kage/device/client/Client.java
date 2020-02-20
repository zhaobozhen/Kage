package com.absinthe.kage.device.client;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.absinthe.kage.connect.Const;
import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.cmd.ImageInfoCommand;
import com.absinthe.kage.device.cmd.InquiryDeviceInfoCommand;
import com.absinthe.kage.device.cmd.MediaPreparePlayCommand;
import com.absinthe.kage.device.cmd.PromptPhoneConnectedCommand;
import com.absinthe.kage.device.heartbeat.HeartbeatRequest;
import com.absinthe.kage.device.model.DeviceInfo;
import com.absinthe.kage.ui.receiver.ReceiverActivity;

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

            String[] splits = command.split(IpMessageProtocol.DELIMITER);
            int commandNum = 0;
            try {
                commandNum = Integer.parseInt(splits[0]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            switch (commandNum) {
                case IpMessageConst.IS_ONLINE:
                    try {
                        writeToStream(dos, new HeartbeatRequest().getData());
                    } catch (IOException e) {
                        e.printStackTrace();
                        offline(mSocket, dis, dos);
                    }
                    break;
                case IpMessageConst.GET_DEVICE_INFO:
                    try {
                        InquiryDeviceInfoCommand deviceInfoCommand = new InquiryDeviceInfoCommand();
                        deviceInfoCommand.phoneName = DeviceManager.Singleton.INSTANCE.getInstance().getConfig().name;
                        writeToStream(dos, deviceInfoCommand.pack());
                    } catch (IOException e) {
                        e.printStackTrace();
                        offline(mSocket, dis, dos);
                    }
                    break;
                case IpMessageConst.PROMPT_PHONE_CONNECT:
                    if (mDeviceInfo == null) {
                        mDeviceInfo = new DeviceInfo();
                    }
                    if (splits.length == PromptPhoneConnectedCommand.LENGTH) {
                        mDeviceInfo.setName(splits[1]);
                        mDeviceInfo.setIp(splits[2]);
                    }
                    break;
                case IpMessageConst.MEDIA_STOP:
                    Intent stopIntent = new Intent(mContext, ReceiverActivity.class);
                    stopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    stopIntent.putExtra(ReceiverActivity.EXTRA_IMAGE_URI, ReceiverActivity.EXTRA_FINISH);
                    mContext.startActivity(stopIntent);
                    break;
                case IpMessageConst.MEDIA_PREPARE_PLAY:
                    if (splits.length >= MediaPreparePlayCommand.MIN_LENGTH) {
                        String type = splits[1];

                        if (type.equals(MediaPreparePlayCommand.TYPE_IMAGE)) {

                        }
                    }
                    break;
                case IpMessageConst.MEDIA_IMAGE_INFO:
                    if (splits.length == ImageInfoCommand.LENGTH) {
                        String imageUri = splits[1];

                        if (!TextUtils.isEmpty(imageUri)) {
                            if (mDeviceInfo != null) {
                                String ip = mDeviceInfo.getIp();
                                if (!TextUtils.isEmpty(ip)) {
                                    imageUri = String.format(Const.HTTP_SERVER_FORMAT, ip) + imageUri;

                                    Intent intent = new Intent(mContext, ReceiverActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.putExtra(ReceiverActivity.EXTRA_IMAGE_URI, imageUri);
                                    mContext.startActivity(intent);
                                }
                            }
                        }
                    }
                    break;
                default:
            }
        }
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
