package com.absinthe.kage.client;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.absinthe.kage.protocol.BaseProtocol;
import com.absinthe.kage.protocol.Config;
import com.absinthe.kage.protocol.DataProtocol;
import com.absinthe.kage.protocol.PingProtocol;
import com.absinthe.kage.utils.SocketUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.SocketFactory;

public class ClientRequestTask implements Runnable {

    private static final int SUCCESS = 100;
    private static final int FAILED = -1;

    private boolean isLongConnection = true;
    private String mIPAddress;
    private Handler mHandler;
    private SendTask mSendTask;
    private ReceiveTask mReceiveTask;
    private HeartBeatTask mHeartBeatTask;
    private Socket mSocket;

    private boolean isSocketAvailable;
    private boolean closeSendTask;

    protected volatile ConcurrentLinkedQueue<BaseProtocol> dataQueue = new ConcurrentLinkedQueue<>();

    public ClientRequestTask(String address, IRequestCallBack requestCallBacks) {
        mIPAddress = address;
        mHandler = new KageHandler(requestCallBacks);
    }

    @Override
    public void run() {
        try {
            try {
                mSocket = SocketFactory.getDefault().createSocket(mIPAddress, Config.PORT);
//                mSocket.setSoTimeout(10);
            } catch (ConnectException e) {
                failedMessage(-1, "服务器连接异常，请检查网络");
                return;
            }

            isSocketAvailable = true;

            //开启接收线程
            mReceiveTask = new ReceiveTask();
            mReceiveTask.inputStream = mSocket.getInputStream();
            mReceiveTask.start();

            //开启发送线程
            mSendTask = new SendTask();
            mSendTask.outputStream = mSocket.getOutputStream();
            mSendTask.start();

            //开启心跳线程
            if (isLongConnection) {
                mHeartBeatTask = new HeartBeatTask();
                mHeartBeatTask.outputStream = mSocket.getOutputStream();
                mHeartBeatTask.start();
            }
        } catch (IOException e) {
            failedMessage(-1, "网络发生异常，请稍后重试");
            e.printStackTrace();
        }
    }

    public void addRequest(DataProtocol data) {
        dataQueue.add(data);
        toNotifyAll(dataQueue);//有新增待发送数据，则唤醒发送线程
    }

    public synchronized void stop() {

        //关闭接收线程
        closeReceiveTask();

        //关闭发送线程
        closeSendTask = true;
        toNotifyAll(dataQueue);

        //关闭心跳线程
        closeHeartBeatTask();

        //关闭socket
        closeSocket();

        //清除数据
        clearData();

        failedMessage(-1, "断开连接");
    }

    /**
     * 关闭接收线程
     */
    private void closeReceiveTask() {
        if (mReceiveTask != null) {
            mReceiveTask.interrupt();
            mReceiveTask.isCancel = true;
            if (mReceiveTask.inputStream != null) {
                try {
                    if (isSocketAvailable && !mSocket.isClosed() && mSocket.isConnected()) {
                        mSocket.shutdownInput();//解决java.net.SocketException问题，需要先shutdownInput
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SocketUtil.closeInputStream(mReceiveTask.inputStream);
                mReceiveTask.inputStream = null;
            }
            mReceiveTask = null;
        }
    }

    /**
     * 关闭发送线程
     */
    private void closeSendTask() {
        if (mSendTask != null) {
            mSendTask.isCancle = true;
            mSendTask.interrupt();
            if (mSendTask.outputStream != null) {
                synchronized (mSendTask.outputStream) {//防止写数据时停止，写完再停
                    SocketUtil.closeOutputStream(mSendTask.outputStream);
                    mSendTask.outputStream = null;
                }
            }
            mSendTask = null;
        }
    }

    /**
     * 关闭心跳线程
     */
    private void closeHeartBeatTask() {
        if (mHeartBeatTask != null) {
            mHeartBeatTask.isCancle = true;
            if (mHeartBeatTask.outputStream != null) {
                SocketUtil.closeOutputStream(mHeartBeatTask.outputStream);
                mHeartBeatTask.outputStream = null;
            }
            mHeartBeatTask = null;
        }
    }

    /**
     * 关闭socket
     */
    private void closeSocket() {
        if (mSocket != null) {
            try {
                mSocket.close();
                isSocketAvailable = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 清除数据
     */
    private void clearData() {
        dataQueue.clear();
        isLongConnection = false;
    }

    private void toWait(Object o) {
        synchronized (o) {
            try {
                o.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * notify()调用后，并不是马上就释放对象锁的，而是在相应的synchronized(){}语句块执行结束，自动释放锁后
     *
     * @param o
     */
    protected void toNotifyAll(Object o) {
        synchronized (o) {
            o.notifyAll();
        }
    }

    private void failedMessage(int code, String msg) {
        Message message = mHandler.obtainMessage(FAILED);
        message.what = FAILED;
        message.arg1 = code;
        message.obj = msg;
        mHandler.sendMessage(message);
    }

    private void successMessage(BaseProtocol protocol) {
        Message message = mHandler.obtainMessage(SUCCESS);
        message.what = SUCCESS;
        message.obj = protocol;
        mHandler.sendMessage(message);
    }

    private boolean isConnected() {
        if (mSocket.isClosed() || !mSocket.isConnected()) {
            ClientRequestTask.this.stop();
            return false;
        }
        return true;
    }

    /**
     * 服务器返回处理，主线程运行
     */
    public static class KageHandler extends Handler {

        private IRequestCallBack mRequestCallBack;

        KageHandler(IRequestCallBack callBack) {
            super(Looper.getMainLooper());
            this.mRequestCallBack = callBack;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case SUCCESS:
                    mRequestCallBack.onSuccess((BaseProtocol) msg.obj);
                    break;
                case FAILED:
                    mRequestCallBack.onFailed(msg.arg1, (String) msg.obj);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 数据接收线程
     */
    public class ReceiveTask extends Thread {

        private boolean isCancel = false;
        private InputStream inputStream;

        @Override
        public void run() {
            while (!isCancel) {
                if (!isConnected()) {
                    break;
                }

                if (inputStream != null) {
                    BaseProtocol receiveData = SocketUtil.readFromStream(inputStream);
                    if (receiveData != null) {
                        if (receiveData.getProtocolType() == 1 || receiveData.getProtocolType() == 3) {
                            successMessage(receiveData);
                        }
                    } else {
                        break;
                    }
                }
            }

            SocketUtil.closeInputStream(inputStream);//循环结束则退出输入流
        }
    }

    /**
     * 数据发送线程
     * 当没有发送数据时让线程等待
     */
    public class SendTask extends Thread {

        private boolean isCancle = false;
        private OutputStream outputStream;

        @Override
        public void run() {
            while (!isCancle) {
                if (!isConnected()) {
                    break;
                }

                BaseProtocol dataContent = dataQueue.poll();
                if (dataContent == null) {
                    toWait(dataQueue);//没有发送数据则等待
                    if (closeSendTask) {
                        closeSendTask();//notify()调用后，并不是马上就释放对象锁的，所以在此处中断发送线程
                    }
                } else if (outputStream != null) {
                    synchronized (outputStream) {
                        SocketUtil.write2Stream(dataContent, outputStream);
                    }
                }
            }

            SocketUtil.closeOutputStream(outputStream);//循环结束则退出输出流
        }
    }

    /**
     * 心跳实现，频率5秒
     * Created by meishan on 16/12/1.
     */
    public class HeartBeatTask extends Thread {

        private static final int REPEATTIME = 5000;
        private boolean isCancle = false;
        private OutputStream outputStream;
        private int pingId;

        @Override
        public void run() {
            pingId = 1;
            while (!isCancle) {
                if (!isConnected()) {
                    break;
                }

                try {
                    mSocket.sendUrgentData(0xFF);
                } catch (IOException e) {
                    isSocketAvailable = false;
                    ClientRequestTask.this.stop();
                    break;
                }

                if (outputStream != null) {
                    PingProtocol pingProtocol = new PingProtocol();
                    pingProtocol.setPingId(pingId);
                    pingProtocol.setUnused("ping...");
                    SocketUtil.write2Stream(pingProtocol, outputStream);
                    pingId = pingId + 2;
                }

                try {
                    Thread.sleep(REPEATTIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            SocketUtil.closeOutputStream(outputStream);
        }
    }
}
