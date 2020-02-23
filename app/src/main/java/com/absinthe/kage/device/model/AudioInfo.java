package com.absinthe.kage.device.model;

import com.google.gson.annotations.SerializedName;

public class AudioInfo {

    @SerializedName("url")
    private String url;//播放地址

    @SerializedName("name")
    private String name;//歌曲名

    @SerializedName("artist")
    private String artist;//演唱者

    @SerializedName("album")
    private String album;//专辑

    @SerializedName("coverPath")
    private String coverPath;//封面地址

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }
}
