package com.absinthe.kage.client;

import com.absinthe.kage.protocol.BaseProtocol;

public interface IRequestCallBack {

    void onSuccess(BaseProtocol msg);

    void onFailed(int errorCode, String msg);
}