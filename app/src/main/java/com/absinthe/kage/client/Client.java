package com.absinthe.kage.client;

import android.util.Log;

import com.absinthe.kage.device.heartbeat.HeartbeatRequest;
import com.absinthe.kage.protocol.IpMessageConst;
import com.absinthe.kage.protocol.IpMessageProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client extends Thread implements Runnable {
    private static final String TAG = Client.class.getSimpleName();

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private boolean isFirstCmd;

    public Client(Socket s, DataInputStream is, DataOutputStream os) {
        socket = s;
        dis = is;
        dos = os;
        isFirstCmd = true;
    }

    @Override
    public void run() {
        while (true) {
            String command = readToStream(dis, Client.this);
            Log.i(TAG, TAG + "  received command is " + command);

            if (command == null) {
                offline(socket, dis, dos);
                break;
            }
            if (command.isEmpty()) {
                continue;
            }
            String[] str = command.split(IpMessageProtocol.DELIMITER);
            int commandNum = 0;
            try {
                commandNum = Integer.parseInt(str[0]);
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "parseInt Exception = " + e);
            }
            switch (commandNum) {
                case IpMessageConst.IS_ONLINE:
                    try {
                        writeToStream(dos, new HeartbeatRequest().getData());
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        offline(socket, dis, dos);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    break;
                case IpMessageConst.GET_DEVICE_INFO:
                    try {
                        writeToStream(dos, IpMessageConst.GET_DEVICE_INFO + IpMessageProtocol.DELIMITER + "YES");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        offline(socket, dis, dos);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    break;
                default:
            }
        }
    }

    private String readToStream(DataInputStream dis, Client client) {
        Log.i(TAG, "readToStream: isFirstCmd:" + client.isFirstCmd);
        int receivedLen;
        try {
            receivedLen = dis.readInt();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        Log.i(TAG, "readToStream received str length == " + receivedLen);
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

    private synchronized void writeToStream(DataOutputStream dos, String str) throws Exception {
        Log.i(TAG, "writeToStream sendStr is " + str);
        byte[] bArray = str.getBytes(StandardCharsets.UTF_8);
        int sendLen = bArray.length;
        Log.i(TAG, "writeToStream sendStr length is " + sendLen);
        dos.writeInt(sendLen);
        dos.flush();
        dos.write(bArray, 0, sendLen);
    }

    private void offline(Socket socket, DataInputStream dis, DataOutputStream dos) {
        Log.i(TAG, "offline " + socket.getInetAddress());
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
                Log.i(TAG, "IOException=" + e);
            }
        }
    }
}
