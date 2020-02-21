package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

import java.io.IOException;

public class HeartbeatCommand extends Command {

    public static final String HEARTBEAT_MESSAGE = "HEARTBEAT";
    public static final int LENGTH = 2;

    public HeartbeatCommand() {
        cmd = IpMessageConst.IS_ONLINE;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(HEARTBEAT_MESSAGE)
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        try {
            client.writeToStream(pack());
        } catch (IOException e) {
            e.printStackTrace();
            client.offline();
        }
    }

    @Override
    public boolean parseReceived(String received) {
        String[] splits = received.split(IpMessageProtocol.DELIMITER);
        if (splits.length == LENGTH) {
            try {
                return Integer.parseInt(splits[0]) == cmd;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }
}
