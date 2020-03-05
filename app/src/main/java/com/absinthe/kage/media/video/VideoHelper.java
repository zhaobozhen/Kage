package com.absinthe.kage.media.video;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;

import com.blankj.utilcode.util.FileUtils;

public class VideoHelper {

    public static Bitmap getVideoCoverImage(String filePath) {
        if (FileUtils.isFileExists(filePath)) {
            return ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);
        }
        return null;
    }

}
