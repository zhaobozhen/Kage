package com.absinthe.kage.server;

import com.absinthe.kage.protocol.DataProtocol;

public interface IResponseCallback {

    void targetIsOffline(DataProtocol receiveMsg);

    void targetIsOnline(String clientIp);
}
