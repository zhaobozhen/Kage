package com.absinthe.kage.connect.protocol;

public class IpMessageConst {
    public static final int VERSION = 0x001;    // Version

    public static final int IP_MSG_BR_ENTRY = 0x00000001;   //Online
    public static final int IP_MSG_BR_EXIT = 0x00000002;    //Offline
    public static final int IP_MSG_ANS_ENTRY = 0x00000003;  //Online response
    public static final int IP_MSG_BR_ABSENCE = 0x00000004;

    public static final int IS_ONLINE = 0x00000001; //Heartbeat
    public static final int GET_DEVICE_INFO = 0x00000002;   //Get device info
    public static final int PROMPT_PHONE_CONNECT = 0x00000003;  //Device connected response
    public static final int MEDIA_SET_PLAYING_STATUS = 0x00000004;
    public static final int MEDIA_SET_PLAYING_STATE = 0x00000005;
    public static final int MEDIA_PLAY_PREVIOUS = 0x00000006;
    public static final int MEDIA_PLAY_NEXT = 0x00000007;
    public static final int MEDIA_STOP = 0x00000008;
    public static final int MEDIA_PREPARE_PLAY = 0x00000009;
    public static final int MEDIA_IMAGE_INFO = 0x0000000A;  //Get image info like URI etc.
    public static final int MEDIA_GET_PLAYING_STATE = 0x0000000B;
    public static final int MEDIA_GET_PLAYING_STATUS = 0x0000000C;
    public static final int DEVICE_ROTATION = 0x0000000D;   //Device rotate
    public static final int MEDIA_AUDIO_INFO = 0x0000000E;  //Get audio info like URI etc.
    public static final int MEDIA_PAUSE = 0x0000000F;
    public static final int MEDIA_SEEK_TO = 0x00000010;
    public static final int MEDIA_GET_DURATION = 0x00000011;

}
