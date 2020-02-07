package com.absinthe.kage.device.heartbeat;

import com.absinthe.kage.connect.tcp.Request;
import com.absinthe.kage.protocol.IpMessageConst;
import com.absinthe.kage.protocol.IpMessageProtocol;

public class HeartbeatRequest extends Request {
    public HeartbeatRequest() {
        String data = IpMessageConst.IS_ONLINE + IpMessageProtocol.DELIMITER;
        setData(data);
    }
}
