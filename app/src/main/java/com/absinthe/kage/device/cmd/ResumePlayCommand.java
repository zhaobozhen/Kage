package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.media.audio.AudioPlayer;

public class ResumePlayCommand extends Command {

    public ResumePlayCommand() {
        cmd = IpMessageConst.MEDIA_RESUME_PLAY;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        AudioPlayer.INSTANCE.play();
    }

    @Override
    public boolean parseReceived(String received) {
        return false;
    }
}
