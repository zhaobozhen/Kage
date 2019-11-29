package com.absinthe.kage.server;

import com.absinthe.kage.protocol.BaseProtocol;
import com.absinthe.kage.protocol.DataAckProtocol;
import com.absinthe.kage.protocol.DataProtocol;
import com.absinthe.kage.protocol.PingAckProtocol;
import com.absinthe.kage.protocol.PingProtocol;
import com.absinthe.kage.utils.SocketUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerResponseTask implements Runnable {

    private ReciveTask reciveTask;
    private SendTask sendTask;
    private Socket socket;
    private IResponseCallback tBack;

    private volatile ConcurrentLinkedQueue<BaseProtocol> dataQueue = new ConcurrentLinkedQueue<>();
    private static ConcurrentHashMap<String, Socket> onLineClient = new ConcurrentHashMap<>();

    private String userIP;

    public String getUserIP() {
        return userIP;
    }

    public ServerResponseTask(Socket socket, IResponseCallback tBack) {
        this.socket = socket;
        this.tBack = tBack;
        this.userIP = socket.getInetAddress().getHostAddress();
        System.out.println("用户IP地址：" + userIP);
    }

    @Override
    public void run() {
        try {
            //开启接收线程
            reciveTask = new ReciveTask();
            reciveTask.inputStream = new DataInputStream(socket.getInputStream());
            reciveTask.start();

            //开启发送线程
            sendTask = new SendTask();
            sendTask.outputStream = new DataOutputStream(socket.getOutputStream());
            sendTask.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (reciveTask != null) {
            reciveTask.isCancle = true;
            reciveTask.interrupt();
            if (reciveTask.inputStream != null) {
                SocketUtil.closeInputStream(reciveTask.inputStream);
                reciveTask.inputStream = null;
            }
            reciveTask = null;
        }

        if (sendTask != null) {
            sendTask.isCancle = true;
            sendTask.interrupt();
            if (sendTask.outputStream != null) {
                synchronized (sendTask.outputStream) {//防止写数据时停止，写完再停
                    sendTask.outputStream = null;
                }
            }
            sendTask = null;
        }
    }

    public void addMessage(BaseProtocol data) {
        if (!isConnected()) {
            return;
        }

        dataQueue.offer(data);
        toNotifyAll(dataQueue);//有新增待发送数据，则唤醒发送线程
    }

    public Socket getConnectedClient(String clientID) {
        return onLineClient.get(clientID);
    }

    /**
     * 打印已经链接的客户端
     */
    public static void printAllClient() {
        if (onLineClient == null) {
            return;
        }
        for (String s : onLineClient.keySet()) {
            System.out.println("client:" + s);
        }
    }

    public void toWaitAll(Object o) {
        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void toNotifyAll(Object obj) {
        synchronized (obj) {
            obj.notifyAll();
        }
    }

    private boolean isConnected() {
        if (socket.isClosed() || !socket.isConnected()) {
            onLineClient.remove(userIP);
            ServerResponseTask.this.stop();
            System.out.println("socket closed...");
            return false;
        }
        return true;
    }

    public class ReciveTask extends Thread {

        private DataInputStream inputStream;
        private boolean isCancle;

        @Override
        public void run() {
            while (!isCancle) {
                if (!isConnected()) {
                    isCancle = true;
                    break;
                }

                BaseProtocol clientData = SocketUtil.readFromStream(inputStream);

                if (clientData != null) {
                    if (clientData.getProtocolType() == 0) {
                        System.out.println("dtype: " + ((DataProtocol) clientData).getDtype() + ", pattion: " + ((DataProtocol) clientData).getPattion() + ", msgId: " + ((DataProtocol) clientData).getMsgId() + ", data: " + ((DataProtocol) clientData).getData());

                        DataAckProtocol dataAck = new DataAckProtocol();
                        dataAck.setUnused("收到消息：" + ((DataProtocol) clientData).getData());
                        dataQueue.offer(dataAck);
                        toNotifyAll(dataQueue); //唤醒发送线程

                        tBack.targetIsOnline(userIP);
                    } else if (clientData.getProtocolType() == 2) {
                        System.out.println("pingId: " + ((PingProtocol) clientData).getPingId());

                        PingAckProtocol pingAck = new PingAckProtocol();
                        pingAck.setUnused("收到心跳");
                        dataQueue.offer(pingAck);
                        toNotifyAll(dataQueue); //唤醒发送线程

                        tBack.targetIsOnline(userIP);
                    }
                } else {
                    System.out.println("client is offline...");
                    break;
                }
            }

            SocketUtil.closeInputStream(inputStream);
        }
    }

    public class SendTask extends Thread {

        private DataOutputStream outputStream;
        private boolean isCancle;

        @Override
        public void run() {
            while (!isCancle) {
                if (!isConnected()) {
                    isCancle = true;
                    break;
                }

                BaseProtocol procotol = dataQueue.poll();
                if (procotol == null) {
                    toWaitAll(dataQueue);
                } else if (outputStream != null) {
                    synchronized (outputStream) {
                        SocketUtil.write2Stream(procotol, outputStream);
                    }
                }
            }

            SocketUtil.closeOutputStream(outputStream);
        }
    }
}
