package com.absinthe.kage.protocol;

public class IpMessageConst {
    public static final int VERSION = 0x001;        // 版本号

    public static final int IP_MSG_BR_ENTRY = 0x00000001;    //上线
    public static final int IP_MSG_BR_EXIT = 0x00000002;    //下线
    public static final int IP_MSG_ANS_ENTRY = 0x00000003;//上线应答
    public static final int IP_MSG_BR_ABSENCE = 0x00000004;    //

    public static final int IS_ONLINE = 0x00000096;
    public static final int GET_CLIENTTYPE = 0x0000009F;//获取机芯类型
    public static final int PROMPT_PHONE_CONNECT = 0x000000A0;//获取机芯类型


}
