package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class PlayPreviousCommand extends Command {

    public static final String MESSAGE = "PLAY_PRE";

    public PlayPreviousCommand() {
        cmd = IpMessageConst.MEDIA_PLAY_PREVIOUS;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(MESSAGE)
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
