package com.absinthe.kage.device.heartbeat;

import com.absinthe.kage.connect.tcp.Request;
import com.absinthe.kage.device.cmd.HeartbeatCommand;

public class HeartbeatRequest extends Request {

    public HeartbeatRequest() {
        setData(new HeartbeatCommand().pack());
    }
}
