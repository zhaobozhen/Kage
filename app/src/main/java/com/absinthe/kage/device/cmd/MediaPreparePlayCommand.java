package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class MediaPreparePlayCommand extends Command {

    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_MUSIC = "MUSIC";
    public static final int MIN_LENGTH = 2;

    public String type = "UNKNOWN";

    public MediaPreparePlayCommand() {
        cmd = IpMessageConst.MEDIA_PREPARE_PLAY;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(type)
                .build();
    }

    @Override
    public void doWork(Client client, String received) {

    }

    @Override
    public boolean parseReceived(String received) {
        String[] splits = received.split(IpMessageProtocol.DELIMITER);
        if (splits.length >= MIN_LENGTH) {
            type = splits[1];
            return true;
        } else {
            return false;
        }
    }
}
