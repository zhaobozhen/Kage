package com.absinthe.kage.connect.tcp;

public interface IPacketWriter {
    void writePacket(Packet packet);

    void shutdown();
}
