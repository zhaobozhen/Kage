package com.absinthe.kage.device.cmd;

import android.content.Intent;
import android.text.TextUtils;

import com.absinthe.kage.connect.Const;
import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.ui.receiver.ReceiverActivity;

public class ImageInfoCommand extends Command {

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

    @Override
    public void doWork(Client client, String received) {
        if (parseReceived(received)) {
            if (!TextUtils.isEmpty(info) && client.getDeviceInfo() != null) {
                String imageUri = info;
                String ip = client.getDeviceInfo().getIp();
                if (!TextUtils.isEmpty(ip)) {
                    imageUri = String.format(Const.HTTP_SERVER_FORMAT, ip) + imageUri;

                    Intent intent = new Intent(client.getContext(), ReceiverActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.putExtra(ReceiverActivity.EXTRA_IMAGE_URI, imageUri);
                    client.getContext().startActivity(intent);
                }
            }
        }
    }

    @Override
    public boolean parseReceived(String received) {
        String[] splits = received.split(IpMessageProtocol.DELIMITER);
        if (splits.length == LENGTH) {
            info = splits[1];
            return true;
        } else {
            return false;
        }
    }
}
