package com.absinthe.kage.device.heartbeat;

import com.absinthe.kage.connect.tcp.KageSocket;
import com.absinthe.kage.connect.tcp.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeartbeatSender {
    private static final String TAG = HeartbeatSender.class.getSimpleName();

    private ExecutorService mExecutorService = Executors.newCachedThreadPool();
    private List<HeartbeatTask> mHeartbeatTaskList = new ArrayList<>();
    private KageSocket mSocket;
    private boolean isInit = false;

    public HeartbeatSender(KageSocket socket) {
        mSocket = socket;
    }

    public void beat(String heartbeatId, int timeout, final IHeartbeatCallback callback) {
        final HeartbeatTask heartbeatTask = new HeartbeatTask(heartbeatId, timeout);
        heartbeatTask.setHeartbeatCallback(new IHeartbeatCallback() {
            @Override
            public void onBeatSuccess(String heartbeatId) {
                synchronized (HeartbeatSender.class) {
                    mHeartbeatTaskList.remove(heartbeatTask);
                }
                if (null != callback) {
                    callback.onBeatSuccess(heartbeatId);
                }
            }

            @Override
            public void onBeatTimeout(String heartbeatId) {
                synchronized (HeartbeatSender.class) {
                    mHeartbeatTaskList.remove(heartbeatTask);
                }
                if (null != callback) {
                    callback.onBeatTimeout(heartbeatId);
                }
            }

            @Override
            public void onBeatCancel(String heartbeatId) {
                synchronized (HeartbeatSender.class) {
                    mHeartbeatTaskList.remove(heartbeatTask);
                }
                if (null != callback) {
                    callback.onBeatCancel(heartbeatId);
                }
            }
        });
        synchronized (HeartbeatSender.class) {
            if (isInit) {
                mHeartbeatTaskList.add(heartbeatTask);
                mExecutorService.submit(heartbeatTask);
                return;
            }
        }
        if (null != callback) {
            callback.onBeatCancel(heartbeatId);
        }
    }

    public void release() {
        HeartbeatTask[] tempList;
        synchronized (HeartbeatSender.class) {
            int size = mHeartbeatTaskList.size();
            tempList = new HeartbeatTask[size];
            mHeartbeatTaskList.toArray(tempList);
            isInit = false;
        }
        for (HeartbeatTask heartbeatTask : tempList) {
            heartbeatTask.releaseBeat();
        }
    }

    public void init() {
        synchronized (HeartbeatSender.class) {
            isInit = true;
        }
    }

    private class HeartbeatTask implements Runnable {
        private String mId;
        private int mTimeout;
        private IHeartbeatCallback mCallback;
        private final HeartbeatRequest mHeartbeatRequest;

        private HeartbeatTask(String heartbeatId, int mTimeout) {
            this.mId = heartbeatId;
            this.mTimeout = mTimeout;
            mHeartbeatRequest = new HeartbeatRequest();
            mHeartbeatRequest.setId(mId);
        }

        @Override
        public void run() {
            mSocket.send(mHeartbeatRequest);
            Response response = mHeartbeatRequest.waitResponse(mTimeout);
            if (null == response) {
                if (null != mCallback) {
                    mCallback.onBeatTimeout(mId);
                }
            } else if (response instanceof CancelBeatResponse) {
                if (null != mCallback) {
                    mCallback.onBeatCancel(mId);
                }
            } else {
                if (null != mCallback) {
                    mCallback.onBeatSuccess(mId);
                }
            }
        }

        public void releaseBeat() {
            mHeartbeatRequest.setResponse(new CancelBeatResponse());
        }

        public void setHeartbeatCallback(IHeartbeatCallback callback) {
            mCallback = callback;
        }
    }

    public interface IHeartbeatCallback {
        void onBeatSuccess(String heartbeatId);

        void onBeatTimeout(String heartbeatId);

        void onBeatCancel(String heartbeatId);
    }
}
