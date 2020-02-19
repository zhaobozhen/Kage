package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.Device;

public class InquiryPlayStatusCommand extends Device.Command {

    public InquiryPlayStatusCommand() {
        cmd = IpMessageConst.MEDIA_GET_PLAY_STATUS;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .build();
    }
}
