package com.absinthe.kage.device.cmd;

import android.content.Intent;

import com.absinthe.kage.connect.protocol.IpMessageConst;
import com.absinthe.kage.device.Command;
import com.absinthe.kage.device.CommandBuilder;
import com.absinthe.kage.device.client.Client;
import com.absinthe.kage.ui.receiver.ReceiverActivity;

public class StopCommand extends Command {

    private static final String STOP = "STOP";

    public StopCommand() {
        cmd = IpMessageConst.MEDIA_STOP;
    }

    @Override
    public String pack() {
        return new CommandBuilder()
                .with(this)
                .append(STOP)
                .build();
    }

    @Override
    public void doWork(Client client, String received) {
        Intent stopIntent = new Intent(client.getContext(), ReceiverActivity.class);
        stopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stopIntent.putExtra(ReceiverActivity.EXTRA_IMAGE_URI, ReceiverActivity.EXTRA_FINISH);
        client.getContext().startActivity(stopIntent);
    }

    @Override
    public boolean parseReceived(String received) {
        return true;
    }
}
