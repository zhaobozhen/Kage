package com.absinthe.kage.media;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class MediaDirectory implements Parcelable {
    private long id;
    private List<LocalMedia> mediaList;
    private String name;
    private int type;

    MediaDirectory() {
    }

    private MediaDirectory(Parcel in) {
        id = in.readLong();
        mediaList = in.createTypedArrayList(LocalMedia.CREATOR);
        name = in.readString();
        type = in.readInt();
    }

    public static final Creator<MediaDirectory> CREATOR = new Creator<MediaDirectory>() {
        @Override
        public MediaDirectory createFromParcel(Parcel in) {
            return new MediaDirectory(in);
        }

        @Override
        public MediaDirectory[] newArray(int size) {
            return new MediaDirectory[size];
        }
    };

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getItemCount() {
        List<LocalMedia> list = mediaList;
        if (list != null) {
            return list.size();
        }
        return 0;
    }

    public void addMedia(LocalMedia media) {
        if (mediaList == null) {
            mediaList = new ArrayList<>();
        }
        mediaList.add(media);
    }

    public List<LocalMedia> getMediaList() {
        return mediaList;
    }

    public String getFirstMediaPath() {
        List<LocalMedia> list = mediaList;
        if (list == null) {
            return null;
        } else {
            return mediaList.get(0).getFilePath();
        }
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MediaDirectory)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        MediaDirectory directory = (MediaDirectory) obj;
        return directory.id == this.id && directory.name.equals(this.name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeTypedList(mediaList);
        dest.writeString(name);
        dest.writeInt(type);
    }
}

