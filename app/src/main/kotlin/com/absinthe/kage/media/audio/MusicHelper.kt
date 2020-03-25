package com.absinthe.kage.media.audio

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.absinthe.kage.media.TYPE_AUDIO
import java.util.*

object MusicHelper {

    fun getAllLocalMusic(context: Context): List<LocalMusic> {
        val result: MutableList<LocalMusic> = ArrayList()
        val cursor = context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER)
        if (cursor != null && cursor.moveToFirst()) {
            do {
                val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val albumId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                val artistId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID))

                if (title.isNotEmpty()) {
                    val music = LocalMusic().apply {
                        this.title = title
                        this.album = album
                        this.albumId = albumId
                        this.artist = artist
                        this.artistId = artistId
                        this.filePath = path
                        this.type = TYPE_AUDIO
                    }
                    result.add(music)
                }
            } while (cursor.moveToNext())
            cursor.close()
        }
        return result
    }

    fun getAlbumArt(albumId: Long): Uri {
        val uriAlbums = "content://media/external/audio/albumart"
        return Uri.withAppendedPath(Uri.parse(uriAlbums), albumId.toString())
    }
}