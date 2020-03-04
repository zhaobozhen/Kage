package com.absinthe.kage.connect;

import com.absinthe.kage.connect.protocol.Config;

public class Const {
    public static final String LOCAL_IP_IN_AP = "192.168.43.1";
    public static final String BROADCAST_IP_IN_AP = "192.168.43.255";
    public static final String BROADCAST_IP_IN_WIFI = "255.255.255.255";

    public static final String HTTP_SERVER_FORMAT = "http://%s:" + Config.HTTP_SERVER_PORT;
}
