package com.absinthe.kage.device.cmd;

import android.content.Intent;
import android.text.TextUtils;

import com.absinthe.kage.connect.Const;
import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.media.audio.LocalMusic;
import com.absinthe.kage.ui.sender.MusicActivity;

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
        if (parseReceived(received)) {
            if (!TextUtils.isEmpty(url) && client.getDeviceInfo() != null) {
                LocalMusic localMusic = new LocalMusic();
                String ip = client.getDeviceInfo().getIp();
                if (!TextUtils.isEmpty(ip)) {
                    localMusic.setFilePath(String.format(Const.HTTP_SERVER_FORMAT, ip) + url);
                    localMusic.setTitle(name);
                    localMusic.setAlbum(album);
                    localMusic.setArtist(artist);
                    localMusic.setCoverPath(coverPath);

                    Intent intent = new Intent(client.getContext(), MusicActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(MusicActivity.EXTRA_MUSIC_INFO, localMusic);
                    intent.putExtra(MusicActivity.EXTRA_DEVICE_TYPE, MusicActivity.TYPE_RECEIVER);
                    client.getContext().startActivity(intent);
                }
            }
        }
    }

    @Override
    public boolean parseReceived(String received) {
        String[] splits = received.split(IpMessageProtocol.DELIMITER);
        if (splits.length == LENGTH) {
            url = splits[1];
            name = splits[2];
            artist = splits[3];
            album = splits[4];
            coverPath = splits[5];
            return true;
        } else {
            return false;
        }
    }
}
