package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class SetPlayStatusCommand extends Command {

    public int statusCode;

    public SetPlayStatusCommand() {
        cmd = IpMessageConst.MEDIA_SET_PLAYER_STATUS;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(String.valueOf(statusCode))
                .build();
    }

    @Override
    public void doWork(Client client, String received) {

    }

    @Override
    public boolean parseReceived(String received) {
        return false;
    }
}
