package com.absinthe.kage.client;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.absinthe.kage.protocol.DataProtocol;

public class ConnectionClient implements LifecycleObserver {

    private boolean isClosed;

    private ClientRequestTask mClientRequestTask;

    public ConnectionClient(String address, IRequestCallBack requestCallBack) {
        mClientRequestTask = new ClientRequestTask(address, requestCallBack);
        new Thread(mClientRequestTask).start();
    }

    public void addNewRequest(DataProtocol data) {
        if (mClientRequestTask != null && !isClosed)
            mClientRequestTask.addRequest(data);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void closeConnect() {
        isClosed = true;
        mClientRequestTask.stop();
    }
}
