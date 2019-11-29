package com.absinthe.kage.client;

import com.absinthe.kage.protocol.DataProtocol;

public class ConnectionClient {

    private boolean isClosed;

    private ClientRequestTask mClientRequestTask;

    public ConnectionClient(IRequestCallBack requestCallBack) {
        mClientRequestTask = new ClientRequestTask(requestCallBack);
        new Thread(mClientRequestTask).start();
    }

    public void addNewRequest(DataProtocol data) {
        if (mClientRequestTask != null && !isClosed)
            mClientRequestTask.addRequest(data);
    }

    public void closeConnect() {
        isClosed = true;
        mClientRequestTask.stop();
    }
}
