package com.absinthe.kage.media.audio;

import android.os.Parcel;
import android.os.Parcelable;

import com.absinthe.kage.media.LocalMedia;

public class LocalMusic extends LocalMedia implements Parcelable {

    private String album;
    private int albumId;
    private String artist;
    private int artistId;
    private String coverPath;

    public LocalMusic() {}

    public LocalMusic(Parcel in) {
        title = in.readString();
        filePath = in.readString();
        date = in.readString();
        sortLetters = in.readString();
        type = in.readInt();
        duration = in.readLong();
        size = in.readFloat();
        album = in.readString();
        albumId = in.readInt();
        artist = in.readString();
        artistId = in.readInt();
        coverPath = in.readString();
    }

    public static final Creator<LocalMusic> CREATOR = new Creator<LocalMusic>() {
        @Override
        public LocalMusic createFromParcel(Parcel in) {
            return new LocalMusic(in);
        }

        @Override
        public LocalMusic[] newArray(int size) {
            return new LocalMusic[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(filePath);
        dest.writeString(date);
        dest.writeString(sortLetters);
        dest.writeInt(type);
        dest.writeLong(duration);
        dest.writeFloat(size);
        dest.writeString(album);
        dest.writeInt(albumId);
        dest.writeString(artist);
        dest.writeInt(artistId);
        dest.writeString(coverPath);
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getAlbumId() {
        return albumId;
    }

    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public int getArtistId() {
        return artistId;
    }

    public void setArtistId(int artistId) {
        this.artistId = artistId;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }
}
