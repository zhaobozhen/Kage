package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class VideoInfoCommand extends Command {

    public static final int LENGTH = 3;

    public String title;
    public String url;

    public VideoInfoCommand() {
        cmd = IpMessageConst.MEDIA_VIDEO_INFO;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(title)
                .append(url)
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
