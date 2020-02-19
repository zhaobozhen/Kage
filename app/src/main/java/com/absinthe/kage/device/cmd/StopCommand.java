package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.Device;

public class StopCommand extends Device.Command {

    private static final String STOP = "STOP";

    public StopCommand() {
        cmd = IpMessageConst.MEDIA_STOP;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(STOP)
                .build();
    }
}
