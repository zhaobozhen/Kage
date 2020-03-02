package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.media.audio.AudioPlayer;

public class SeekToCommand extends Command {

    public static final int LENGTH = 2;

    public int position;

    public SeekToCommand() {
        cmd = IpMessageConst.MEDIA_SEEK_TO;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(String.valueOf(position))
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        if (parseReceived(received)) {
            AudioPlayer.getInstance(client.getContext()).seekTo(position);
        }
    }

    @Override
    public boolean parseReceived(String received) {
        String[] splits = received.split(IpMessageProtocol.DELIMITER);
        if (splits.length == LENGTH) {
            try {
                position = Integer.parseInt(splits[1]);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            return false;
        }
    }
}
