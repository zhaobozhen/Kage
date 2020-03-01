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
    public static final int MEDIA_SET_PLAYER_STATUS = 0x00000004;   //Set player status
    public static final int MEDIA_SET_PLAYING_STATE = 0x00000005;   //Set playing state
    public static final int MEDIA_PLAY_PREVIOUS = 0x00000006;   //Play previous media
    public static final int MEDIA_PLAY_NEXT = 0x00000007;   //Play next media
    public static final int MEDIA_STOP = 0x00000008;    //Stop media
    public static final int MEDIA_PREPARE_PLAY = 0x00000009;    //Prepare play media
    public static final int MEDIA_IMAGE_INFO = 0x0000000A;  //Get image info like URI etc.
    public static final int MEDIA_GET_PLAYING_STATE = 0x0000000B;   //Get player status
    public static final int MEDIA_GET_PLAYER_STATUS = 0x0000000C;   //Get playing state
    public static final int DEVICE_ROTATION = 0x0000000D;   //Device rotate
    public static final int MEDIA_AUDIO_INFO = 0x0000000E;  //Get audio info like URI etc.
    public static final int MEDIA_PAUSE = 0x0000000F;   //Pause playing media
    public static final int MEDIA_SEEK_TO = 0x00000010; //Media seek to position
    public static final int MEDIA_GET_DURATION = 0x00000011;    //Get media duration
    public static final int RESPONSE_SET_PLAYBACK_PROGRESS = 0x00000012; //Set media playback progress
    public static final int RESPONSE_SET_AUDIO_DURATION = 0x00000013;   //Set audio duration
    public static final int RESPONSE_PLAYING_INDEX = 0x00000014;
    public static final int RESPONSE_SET_AUDIO_MODE = 0x00000015;
    public static final int MEDIA_GET_PLAYING_POSITION = 0x00000016;
    public static final int MEDIA_PLAY_AUDIO_LIST = 0x00000017;
    public static final int MEDIA_SET_AUDIO_MODE = 0x00000018;
    public static final int MEDIA_SET_PLAY_INDEX = 0x00000019;

}
