package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.media.audio.AudioPlayer;

import java.io.IOException;

public class InquiryDurationCommand extends Command {

    public static final String INQUIRY_MESSAGE = "INQUIRY_DURATION";

    public InquiryDurationCommand() {
        cmd = IpMessageConst.MEDIA_GET_DURATION;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(INQUIRY_MESSAGE)
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        int duration = AudioPlayer.INSTANCE.getDuration();
        SetDurationCommand command = new SetDurationCommand();
        command.duration = duration;

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
