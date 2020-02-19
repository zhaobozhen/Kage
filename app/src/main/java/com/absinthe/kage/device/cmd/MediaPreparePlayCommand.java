package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.Device;

public class MediaPreparePlayCommand extends Device.Command {

    public static final String TYPE_IMAGE = "IMAGE";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_MUSIC = "MUSIC";
    public static final int MIN_LENGTH = 2;

    public String type;

    public MediaPreparePlayCommand(String type) {
        cmd = IpMessageConst.MEDIA_PREPARE_PLAY;
        this.type = type;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(type)
                .build();
    }
}
