package com.absinthe.kage.media.audio;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.absinthe.kage.media.LocalMedia;
import com.absinthe.kage.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class MusicHelper {

    public static List<LocalMusic> getAllLocalMusic(Context context) {
        List<LocalMusic> result = new ArrayList<>();
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                int albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                int artistId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));

                if (!(TextUtils.isEmpty(title))) {
                    Logger.d("Title: ", title, "path: ", path);
                    LocalMusic music = new LocalMusic();
                    music.setTitle(title);
                    music.setAlbum(album);
                    music.setAlbumId(albumId);
                    music.setArtist(artist);
                    music.setArtistId(artistId);
                    music.setFilePath(path);
                    music.setType(LocalMedia.TYPE_AUDIO);

                    result.add(music);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return result;
    }

    public static Uri getAlbumArt(Context context, long albumId) {
        String uriAlbums = "content://media/external/audio/albumart";
        return Uri.withAppendedPath(Uri.parse(uriAlbums), String.valueOf(albumId));
    }

}
