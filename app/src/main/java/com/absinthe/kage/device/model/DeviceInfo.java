package com.absinthe.kage.device.model;

import androidx.annotation.NonNull;

public class DeviceInfo {

    private String name;
    private String ip;
    private String protocolVersion;
    private String functionCode;
    private boolean isConnected;
    private int state = STATE_IDLE;

    public static final int STATE_IDLE = 0;//未连接
    public static final int STATE_CONNECTING = 1;//连接中
    public static final int STATE_CONNECTED = 2;//已连接

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public void setFunctionCode(String functionCode) {
        this.functionCode = functionCode;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public void setStateConnecting(){
        setState(STATE_CONNECTING);
    }

    @NonNull
    @Override
    public String toString() {
        return "DeviceInfo: Name = " + name + ", IP = " + ip;
    }
}
