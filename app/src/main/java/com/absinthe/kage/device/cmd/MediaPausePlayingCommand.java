package com.absinthe.kage.device.cmd;

import android.media.session.PlaybackState;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.media.audio.AudioPlayer;

public class MediaPausePlayingCommand extends Command {

    public static final String PAUSE_MESSAGE = "PAUSE";

    public MediaPausePlayingCommand() {
        cmd = IpMessageConst.MEDIA_PAUSE;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(PAUSE_MESSAGE)
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        if (AudioPlayer.INSTANCE.getPlayState() == PlaybackState.STATE_PLAYING) {
            AudioPlayer.INSTANCE.pause();
        }
    }

    @Override
    public boolean parseReceived(String received) {
        return false;
    }
}
