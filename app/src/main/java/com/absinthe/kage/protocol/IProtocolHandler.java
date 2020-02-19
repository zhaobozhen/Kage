package com.absinthe.kage.protocol;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

public interface IProtocolHandler {
    void handleSocketConnectedEvent();

    void handleSocketMassage(String msg);

    void handleSocketDisConnectEvent();

    void handleSocketConnectFail(int errorCode, Exception e);

    void handleSocketSendOrReceiveError();

    /**
     * 为避免多线程安全问题，回调方法都在KageProtocolThreadHandler里执行
     */
    interface IProtocolHandleCallback {

        void onProtocolConnected();

        void onProtocolDisConnect();

        void onProtocolConnectedFailed(int errorCode, Exception e);

        void onProtocolSendOrReceiveError();
    }

    class KageProtocolThreadHandler extends Handler {
        private static HandlerThread mHandlerThread;
        private static KageProtocolThreadHandler mHandler;

        public KageProtocolThreadHandler(Looper looper) {
            super(looper);
        }

        public static KageProtocolThreadHandler getInstance() {
            if (null == mHandler) {
                synchronized (KageProtocolThreadHandler.class) {
                    if (null == mHandler) {
                        if (null == mHandlerThread) {
                            mHandlerThread = new HandlerThread(KageProtocolThreadHandler.class.getSimpleName());
                            mHandlerThread.start();
                        }
                        mHandler = new KageProtocolThreadHandler(mHandlerThread.getLooper());
                    }
                }
            }
            return mHandler;
        }
    }
}

