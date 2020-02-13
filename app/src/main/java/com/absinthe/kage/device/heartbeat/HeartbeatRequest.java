package com.absinthe.kage.device.heartbeat;

import com.absinthe.kage.connect.tcp.Request;
import com.absinthe.kage.protocol.IpMessageConst;
import com.absinthe.kage.protocol.IpMessageProtocol;

public class HeartbeatRequest extends Request {

    public static final String HEARTBEAT_MESSAGE = "HEARTBEAT";

    public HeartbeatRequest() {
        String data = IpMessageConst.IS_ONLINE + IpMessageProtocol.DELIMITER + HEARTBEAT_MESSAGE;
        setData(data);
    }
}
