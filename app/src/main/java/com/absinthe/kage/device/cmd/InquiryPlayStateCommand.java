package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.media.audio.AudioPlayer;

import java.io.IOException;

public class InquiryPlayStateCommand extends Command {

    public InquiryPlayStateCommand() {
        cmd = IpMessageConst.MEDIA_GET_PLAYING_STATE;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        SetPlayStateCommand command = new SetPlayStateCommand();
        command.stateCode = AudioPlayer.INSTANCE.getPlayState();

        try {
            client.writeToStream(command.pack());
        } catch (IOException e) {
            e.printStackTrace();
            client.offline();
        }
    }

    @Override
    public boolean parseReceived(String received) {
        return true;
    }
}
