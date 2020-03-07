package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.client.Client;

import java.io.IOException;

public class InquiryDeviceInfoCommand extends Command {

    public String phoneName;

    public InquiryDeviceInfoCommand() {
        cmd = IpMessageConst.GET_DEVICE_INFO;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(phoneName)
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        try {
            phoneName = DeviceManager.INSTANCE.getConfig().name;
            client.writeToStream(pack());
        } catch (IOException e) {
            e.printStackTrace();
            client.offline();
        }
    }

    @Override
    public boolean parseReceived(String received) {
        return false;
    }
}
