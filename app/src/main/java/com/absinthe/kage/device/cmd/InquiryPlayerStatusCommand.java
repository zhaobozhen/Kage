package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.Device;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.media.audio.AudioPlayer;

import java.io.IOException;

public class InquiryPlayerStatusCommand extends Command {

    public InquiryPlayerStatusCommand() {
        cmd = IpMessageConst.MEDIA_GET_PLAYER_STATUS;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        SetPlayStatusCommand command = new SetPlayStatusCommand();
        command.statusCode = AudioPlayer.getInstance(client.getContext()).getPlayState();

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
