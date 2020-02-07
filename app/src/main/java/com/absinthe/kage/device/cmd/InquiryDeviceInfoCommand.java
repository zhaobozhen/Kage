package com.absinthe.kage.device.cmd;

import com.absinthe.kage.device.Device;
import com.absinthe.kage.protocol.IpMessageConst;
import com.absinthe.kage.protocol.IpMessageProtocol;

public class InquiryDeviceInfoCommand extends Device.Command {

    public String phoneName;

    public InquiryDeviceInfoCommand() {
        cmd = IpMessageConst.GET_CLIENTTYPE;
    }

    @Override
    public String pack() {
        return cmd + IpMessageProtocol.DELIMITER + phoneName;
    }
}
