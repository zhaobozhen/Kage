package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class MediaPausePlayingCommand extends Command {

    public static final String PAUSE_MESSAGE = "PAUSE";

    public MediaPausePlayingCommand() {
        cmd = IpMessageConst.MEDIA_PAUSE;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(PAUSE_MESSAGE)
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
