package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;

public class AudioInfoCommand extends Command {

    public static final int LENGTH = 6;

    public String url;//播放地址
    public String name;//歌曲名
    public String artist;//演唱者
    public String album;//专辑
    public String coverPath;//封面地址

    public AudioInfoCommand() {
        cmd = IpMessageConst.MEDIA_AUDIO_INFO;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(url)
                .append(name)
                .append(artist)
                .append(album)
                .append(coverPath)
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
