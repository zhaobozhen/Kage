package com.absinthe.kage.connect.tcp;

public interface IPacketReader {
    void addRequest(Request request);

    void shutdown();

}
