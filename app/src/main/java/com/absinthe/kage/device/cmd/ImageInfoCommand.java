package com.absinthe.kage.device.cmd;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.Device;

public class ImageInfoCommand extends Device.Command {

    public static final int LENGTH = 2;

    public String info;

    public ImageInfoCommand() {
        cmd = IpMessageConst.MEDIA_IMAGE_INFO;
        info = "";
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(info)
                .build();
    }
}
