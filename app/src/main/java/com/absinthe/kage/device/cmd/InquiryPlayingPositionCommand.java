package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.media.audio.AudioPlayer;

import java.io.IOException;

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
        int position = AudioPlayer.INSTANCE.getCurrentPosition();

        SetPlayingPositionCommand command = new SetPlayingPositionCommand();
        command.position = position;

        try {
            client.writeToStream(command.pack());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean parseReceived(String received) {
        return false;
    }
}
