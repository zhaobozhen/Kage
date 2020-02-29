package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class SetPlayStateCommand extends Command {

    public int stateCode;

    public SetPlayStateCommand() {
        cmd = IpMessageConst.MEDIA_SET_PLAYING_STATE;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(String.valueOf(stateCode))
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
