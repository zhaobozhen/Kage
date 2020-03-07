package com.absinthe.kage.device.cmd;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;

import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.manager.ActivityStackManager;

public class DeviceRotationCommand extends Command {

    public static final int TYPE_LAND = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
    public static final int TYPE_PORT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    public static final int LENGTH = 2;
    public int flag = TYPE_LAND;

    public DeviceRotationCommand() {
        cmd = IpMessageConst.DEVICE_ROTATION;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(String.valueOf(flag))
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        if (parseReceived(received)) {
            BaseActivity topActivity = ActivityStackManager.INSTANCE.getTopActivity();
            if (topActivity != null) {
                topActivity.setRequestedOrientation(flag);
            }
        }
    }

    @Override
    public boolean parseReceived(String received) {
        String[] splits = received.split(IpMessageProtocol.DELIMITER);
        if (splits.length == LENGTH) {
            try {
                flag = Integer.parseInt(splits[1]);
                if (flag == Configuration.ORIENTATION_LANDSCAPE) {
                    flag = TYPE_LAND;
                } else {
                    flag = TYPE_PORT;
                }
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
