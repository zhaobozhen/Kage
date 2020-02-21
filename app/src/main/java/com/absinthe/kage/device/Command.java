package com.absinthe.kage.device;

import com.absinthe.kage.connect.protocol.IpMessageProtocol;
import com.absinthe.kage.device.client.Client;

public abstract class Command {

    protected static final String DELIMITER = IpMessageProtocol.DELIMITER;
    protected int cmd;

    protected Command() {}
    public abstract String pack();
    public abstract void doWork(Client client, String received);
    public abstract boolean parseReceived(String received);
}
