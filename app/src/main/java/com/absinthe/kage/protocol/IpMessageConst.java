package com.absinthe.kage.protocol;

public class IpMessageConst {
    public static final int VERSION = 0x001;    // Version

    public static final int IP_MSG_BR_ENTRY = 0x00000001;   //Online
    public static final int IP_MSG_BR_EXIT = 0x00000002;    //Offline
    public static final int IP_MSG_ANS_ENTRY = 0x00000003;  //Online response
    public static final int IP_MSG_BR_ABSENCE = 0x00000004;

    public static final int IS_ONLINE = 0x00000001; //Heartbeat
    public static final int GET_DEVICE_INFO = 0x00000002;   //Get device info
    public static final int PROMPT_PHONE_CONNECT = 0x00000003;  //Device connected response

}
