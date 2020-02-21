package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.device.model.DeviceInfo;

public class PromptPhoneConnectedCommand extends Command {

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

    @Override
    public void doWork(Client client, String received) {
        if (client.getDeviceInfo() == null) {
            client.setDeviceInfo(new DeviceInfo());
        }
        if (parseReceived(received)) {
            client.getDeviceInfo().setName(phoneName);
            client.getDeviceInfo().setIp(localIp);
        }
    }

    @Override
    public boolean parseReceived(String received) {
        String[] splits = received.split(IpMessageProtocol.DELIMITER);
        if (splits.length == LENGTH) {
            phoneName = splits[1];
            localIp = splits[2];
            return true;
        } else {
            return false;
        }
    }
}
