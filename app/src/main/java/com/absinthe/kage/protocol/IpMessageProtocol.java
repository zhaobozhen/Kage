package com.absinthe.kage.protocol;

import java.util.Date;

public class IpMessageProtocol {
    public static final String DELIMITER = "-->";

    private String version; //Version number
    private String packetNum;   //Packet number
    private String senderName;  //Sender name
    private int cmd;    //Command
    private String additionalSection;   //Additional data

    public IpMessageProtocol() {
        this.packetNum = getSeconds();
    }

    // 根据协议字符串初始化
    public IpMessageProtocol(String protocolString) {
        String[] args = protocolString.split(DELIMITER);
        version = args[0];
        packetNum = args[1];
        senderName = args[2];
        cmd = Integer.parseInt(args[3]);
        additionalSection = args.length >= 5 ? args[4] : "";    //是否有附加数据
        additionalSection = additionalSection.replace("\0", "");
    }

    public IpMessageProtocol(String senderName, int cmd, String additionalSection) {
        this.packetNum = getSeconds();
        this.senderName = senderName;
        this.cmd = cmd;
        this.additionalSection = additionalSection;
    }

    public String getAdditionalSection() {
        return additionalSection;
    }

    public int getCmd() {
        return cmd;
    }

    public String getPacketNum() {
        return packetNum;
    }

    //得到协议串
    public String getProtocolString() {
        return version + DELIMITER +
                packetNum + DELIMITER +
                senderName + DELIMITER +
                cmd + DELIMITER +
                additionalSection;
    }

    //得到数据包编号，毫秒数
    private String getSeconds() {
        Date date = new Date();
        return Long.toString(date.getTime());
    }

    public String getSenderName() {
        return senderName;
    }

    public String getVersion() {
        return version;
    }

    public void setAdditionalSection(String additionalSection) {
        this.additionalSection = additionalSection;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public void setPacketNum(String packetNum) {
        this.packetNum = packetNum;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
