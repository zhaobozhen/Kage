package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class PlayAudioListCommand extends Command {

    public int index;
    public int size;
    public String listInfo;

    public PlayAudioListCommand() {
        cmd = IpMessageConst.MEDIA_PLAY_AUDIO_LIST;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(String.valueOf(index))
                .append(String.valueOf(size))
                .append(listInfo)
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
