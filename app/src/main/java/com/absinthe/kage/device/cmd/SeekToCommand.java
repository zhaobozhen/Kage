package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class SeekToCommand extends Command {

    public int position;

    public SeekToCommand() {
        cmd = IpMessageConst.MEDIA_SEEK_TO;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(String.valueOf(position))
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
