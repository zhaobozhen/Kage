package com.absinthe.kage.client;

import android.util.Log;

import com.absinthe.kage.protocol.IpMessageConst;
import com.absinthe.kage.protocol.IpMessageProtocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client extends Thread implements Runnable {
    private static final String TAG = Client.class.getSimpleName();
    private final byte[] LOCK = new byte[0];

    private Socket socket;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
    private long mStartTime;
    private boolean isFirstCmd;

    public Client(Socket s, DataInputStream is, DataOutputStream os) {
        socket = s;
        dis = is;
        dos = os;
        isFirstCmd = true;
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (true) {
            String command = readMyUTF(dis, Client.this);
            Log.i(TAG, TAG + "  received command is " + command);

            if (command == null) {
                offline(socket, dis, dos);
                break;
            }
            if (command.isEmpty()) {
                continue;
            }
            String[] str = command.split(IpMessageProtocol.DELIMITER);
            int length = str.length;
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
                        writeMyUTF(dos, IpMessageConst.IS_ONLINE + IpMessageProtocol.DELIMITER + "YES");
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        offline(socket, dis, dos);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    break;
                case IpMessageConst.GET_CLIENTTYPE:
                    try {
                        writeMyUTF(dos, IpMessageConst.GET_CLIENTTYPE + IpMessageProtocol.DELIMITER + "YES");
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

    private String readMyUTF(DataInputStream dis, Client client) {
        Log.i(TAG, "readMyUTF:isFirstCmd:" + client.isFirstCmd);
        while (true) {
            int receiveLen = 0;
            try {
                receiveLen = dis.readInt();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Log.i(TAG, "read Int erro  IOException==" + e);
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.i(TAG, "read Int erro  Exception==" + e);
                e.printStackTrace();
                return null;
            }
            Log.i(TAG, "read Int recvLen==" + receiveLen);
            if (receiveLen <= 0) {
                return "";
            }
            byte[] bArray;
            try {
                bArray = new byte[receiveLen];
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                Log.i(TAG, "read Int erro  OutOfMemoryError=" + e);
                return "";
            }
            int bytesRead = 0;
            while (bytesRead < receiveLen) {
                try {
                    int result = dis.read(bArray, bytesRead, receiveLen - bytesRead);
                    if (result == -1)
                        break;
                    bytesRead += result;
                } catch (IOException e) {
                    Log.i(TAG, "read Int erro----  IOException=" + e);
                    return null;
                }
            }
            String enStr;
            try {
                if (client.isFirstCmd) {
                    enStr = new String(bArray, StandardCharsets.UTF_8);
                    String[] str = enStr.split(IpMessageProtocol.DELIMITER);
                    int length = str.length;
                    int n = 0;
                    try {
                        n = Integer.parseInt(str[0]);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i(TAG, "read parseInt Exception = " + e);
                    }
                    client.isFirstCmd = false;
                    enStr = new String(bArray, StandardCharsets.UTF_8);
                } else {
                    enStr = new String(bArray, StandardCharsets.UTF_8);
                }
            } catch (OutOfMemoryError e) {
                e.printStackTrace();
                Log.i(TAG, "read  new String  OutOfMemoryError=" + e);
                return "";
            }
            return enStr;
        }
    }

    private synchronized void writeMyUTF(DataOutputStream dos, String str) throws Exception {
        // String deStr = URLEncoder.encode(str, "utf-8");
        Log.i(TAG, "writeMyUTF sendStr is " + str);
        byte[] bArray = str.getBytes(StandardCharsets.UTF_8);
        int sendLen = bArray.length;
        Log.i(TAG, "writeMyUTF sendLen is " + sendLen);
        dos.writeInt(sendLen);
        dos.flush();
        dos.write(bArray, 0, sendLen);
    }

    private void offline(Socket socket, DataInputStream dis, DataOutputStream dos) {
        Log.i(TAG, "offline " + socket.getInetAddress());
        synchronized (LOCK) {
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
