package com.absinthe.kage.media;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.blankj.utilcode.util.EncryptUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class LocalMedia implements Parcelable {
    private static final String TAG = LocalMedia.class.getSimpleName();

    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_AUDIO = 3;

    protected String title;
    protected String filePath;
    protected String date;
    protected String sortLetters;
    protected int type;
    protected long duration;
    protected float size;

    public LocalMedia() {
    }

    public LocalMedia(Parcel in) {
        title = in.readString();
        filePath = in.readString();
        date = in.readString();
        sortLetters = in.readString();
        type = in.readInt();
        duration = in.readLong();
        size = in.readFloat();
    }

    public String getMediaKey() {
        return EncryptUtils.encryptMD5ToString(filePath);
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getSize() {
        return this.size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getUrl() {
        return encodePath(filePath);
    }

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getSortLetters() {
        return this.sortLetters;
    }

    public void setSortLetters(String sortLetters) {
        this.sortLetters = sortLetters;
    }

    public int getType() {
        return this.type;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return this.date;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LocalMedia)) {
            return false;
        }
        return getMediaKey().equals(((LocalMedia) obj).getMediaKey());
    }

    public static List<MediaDirectory> getMediaDirectory(Context context, int type) {
        if (type == TYPE_VIDEO) {
            return getVideoDirectory(context);
        } else if (type == TYPE_IMAGE) {
            return getImageDirectory(context);
        }
        return null;
    }

    private static List<MediaDirectory> getImageDirectory(Context context) {
        List<MediaDirectory> result = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndex("title"));
                long dirId = cursor.getLong(cursor.getColumnIndex("bucket_id"));
                String dirName = cursor.getString(cursor.getColumnIndex("bucket_display_name"));
                String path = cursor.getString(cursor.getColumnIndex("_data"));
                if (!(TextUtils.isEmpty(title) || 0 == dirId || TextUtils.isEmpty(dirName))) {
                    String info = "Image dir: " + dirId +
                            ", name: " + dirName +
                            ", path: " + path;
                    Log.i(TAG, info);
                    LocalMedia media = new LocalMedia();
                    media.setTitle(title);
                    media.setFilePath(path);
                    media.setType(LocalMedia.TYPE_IMAGE);

                    boolean flag = false;
                    for (MediaDirectory directory : result) {
                        if (directory.getId() == dirId && directory.getName().equals(dirName)) {
                            directory.addMedia(media);
                            flag = true;
                        }
                    }
                    if (!flag) {
                        MediaDirectory directory = new MediaDirectory();
                        directory.setId(dirId);
                        directory.setName(dirName);
                        directory.setType(TYPE_IMAGE);
                        directory.addMedia(media);
                        result.add(directory);
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }

    private static List<MediaDirectory> getVideoDirectory(Context context) {
        List<MediaDirectory> result = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndex("title"));
                long dirId = cursor.getLong(cursor.getColumnIndex("bucket_id"));
                String dirName = cursor.getString(cursor.getColumnIndex("bucket_display_name"));
                String path = cursor.getString(cursor.getColumnIndex("_data"));
                if (!(TextUtils.isEmpty(title) || 0 == dirId || TextUtils.isEmpty(dirName))) {
                    String info = "Video dir: " + dirId +
                            ", name: " + dirName +
                            ", path: " + path;
                    Log.i(TAG, info);

                    LocalMedia media = new LocalMedia();
                    media.setTitle(title);
                    media.setType(TYPE_VIDEO);
                    media.setFilePath(path);

                    boolean flag = false;
                    for (MediaDirectory directory : result) {
                        if (directory.getId() == dirId && directory.getName().equals(dirName)) {
                            directory.addMedia(media);
                            flag = true;
                        }
                    }
                    if (!flag) {
                        MediaDirectory directory = new MediaDirectory();
                        directory.setId(dirId);
                        directory.setName(dirName);
                        directory.setType(TYPE_VIDEO);
                        directory.addMedia(media);
                        result.add(directory);
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }

    public static String millisecondToTimeString(int time) {
        time /= 1000;

        if (((long) time) == 0) {
            return "00:00";
        }

        long hours = time / 3600;
        long remainder = time % 3600;
        long minutes = remainder / 60;
        long secs = remainder % 60;

        StringBuilder stringBuilder = new StringBuilder();

        if (hours != 0) {
            stringBuilder.append(hours < 10 ? "0" : "");
            stringBuilder.append(hours);
            stringBuilder.append(":");
        }
        stringBuilder.append(minutes < 10 ? "0" : "");
        stringBuilder.append(minutes);
        stringBuilder.append(":");
        stringBuilder.append(secs < 10 ? "0" : "");
        stringBuilder.append(secs);

        return stringBuilder.toString();
    }

    private static String encodePath(String path) {
        String[] strs = path.split("/");
        StringBuilder builder = new StringBuilder();
        try {
            for (String str : strs) {
                if (!TextUtils.isEmpty(str)) {
                    builder.append("/");
                    builder.append(URLEncoder.encode(str, "utf-8"));
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static final Creator<LocalMedia> CREATOR = new Creator<LocalMedia>() {
        @Override
        public LocalMedia createFromParcel(Parcel in) {
            return new LocalMedia(in);
        }

        @Override
        public LocalMedia[] newArray(int size) {
            return new LocalMedia[size];
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
    }
}

