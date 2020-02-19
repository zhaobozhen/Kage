package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.Device;

public class InquiryPlayStateCommand extends Device.Command {

    public InquiryPlayStateCommand() {
        cmd = IpMessageConst.MEDIA_GET_PLAY_STATE;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .build();
    }
}
