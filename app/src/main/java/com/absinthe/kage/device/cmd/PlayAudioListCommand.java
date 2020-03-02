package com.absinthe.kage.device.cmd;

import android.content.Intent;
import android.text.TextUtils;

import com.absinthe.kage.connect.Const;
import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.device.model.AudioInfo;
import com.absinthe.kage.media.audio.LocalMusic;
import com.absinthe.kage.ui.sender.MusicActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.List;

public class PlayAudioListCommand extends Command {

    public static final int LENGTH = 4;

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
        if (parseReceived(received)) {
            if (!TextUtils.isEmpty(listInfo) && client.getDeviceInfo() != null) {
                List<AudioInfo> localMusicList = new Gson().fromJson(listInfo, new TypeToken<List<AudioInfo>>(){}.getType());
                if (localMusicList != null && localMusicList.size() > 0) {
                    AudioInfo audioInfo = localMusicList.get(0);
                    String ip = client.getDeviceInfo().getIp();
                    if (!TextUtils.isEmpty(ip)) {
                        LocalMusic localMusic = new LocalMusic();
                        localMusic.setTitle(audioInfo.getName());
                        localMusic.setArtist(audioInfo.getArtist());
                        localMusic.setAlbum(audioInfo.getAlbum());
                        localMusic.setFilePath(String.format(Const.HTTP_SERVER_FORMAT, ip) + audioInfo.getUrl());
                        localMusic.setCoverPath(String.format(Const.HTTP_SERVER_FORMAT, ip)
                                + client.getContext().getExternalFilesDir(null) + File.separator + Const.albumName);

                        Intent intent = new Intent(client.getContext(), MusicActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(MusicActivity.EXTRA_MUSIC_INFO, localMusic);
                        intent.putExtra(MusicActivity.EXTRA_DEVICE_TYPE, MusicActivity.TYPE_RECEIVER);
                        client.getContext().startActivity(intent);
                    }
                }
            }
        }
    }

    @Override
    public boolean parseReceived(String received) {
        String[] splits = received.split(IpMessageProtocol.DELIMITER);
        if (splits.length == LENGTH) {
            try {
                index = Integer.parseInt(splits[1]);
                size = Integer.parseInt(splits[2]);
                listInfo = splits[3];
                return true;
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }
}
