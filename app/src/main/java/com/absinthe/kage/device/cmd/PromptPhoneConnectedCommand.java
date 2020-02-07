package com.absinthe.kage.device.cmd;

import com.absinthe.kage.device.Device;
import com.absinthe.kage.protocol.IpMessageConst;
import com.absinthe.kage.protocol.IpMessageProtocol;

public class PromptPhoneConnectedCommand extends Device.Command {

    public String phoneName;
    public String uuid;

    public PromptPhoneConnectedCommand() {
        cmd = IpMessageConst.PROMPT_PHONE_CONNECT;
    }

    @Override
    public String pack() {
        return cmd + IpMessageProtocol.DELIMITER
                + phoneName + IpMessageProtocol.DELIMITER
                + uuid;
    }
}
