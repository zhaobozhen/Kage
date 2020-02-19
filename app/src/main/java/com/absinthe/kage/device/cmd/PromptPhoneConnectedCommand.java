package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.Device;

public class PromptPhoneConnectedCommand extends Device.Command {

    public static final int LENGTH = 3;

    public String phoneName;
    public String localIp;

    public PromptPhoneConnectedCommand() {
        cmd = IpMessageConst.PROMPT_PHONE_CONNECT;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(phoneName)
                .append(localIp)
                .build();
    }
}
