package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class InquiryPlayingPositionCommand extends Command {

    public static final String MESSAGE = "INQUIRY_PLAYING_POSITION";

    public InquiryPlayingPositionCommand() {
        cmd = IpMessageConst.MEDIA_GET_PLAYING_POSITION;
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
