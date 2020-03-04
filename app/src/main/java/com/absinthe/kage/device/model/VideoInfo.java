package com.absinthe.kage.device.model;

import com.absinthe.kage.connect.protocol.IpMessageProtocol;

public class VideoInfo {
    private String url;
    private String title;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getInfo() {
        return url + IpMessageProtocol.DELIMITER + title;
    }
}
