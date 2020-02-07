package com.absinthe.kage.connect.tcp;

import java.io.DataOutputStream;

public class PacketWriter extends AbstractPacketWriter {
    public PacketWriter(DataOutputStream out, KageSocket.ISocketCallback socketCallback) {
        super(out, socketCallback);
    }
}
