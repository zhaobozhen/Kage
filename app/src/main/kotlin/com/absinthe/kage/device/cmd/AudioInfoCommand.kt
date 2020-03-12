package com.absinthe.kage.device.cmd

import android.content.Intent
import android.text.TextUtils
import com.absinthe.kage.connect.Const
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.media.audio.LocalMusic
import com.absinthe.kage.ui.media.MusicActivity

class AudioInfoCommand : Command() {
    var url: String? = null //播放地址
    var name: String? = null //歌曲名
    var artist: String? = null //演唱者
    var album: String? = null //专辑
    var coverPath: String? = null //封面地址

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(url)
                .append(name)
                .append(artist)
                .append(album)
                .append(coverPath)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (parseReceived(received)) {
            if (!TextUtils.isEmpty(url) && client.deviceInfo != null) {
                val localMusic = LocalMusic()
                val ip = client.deviceInfo!!.ip

                if (!TextUtils.isEmpty(ip)) {
                    localMusic.filePath = String.format(Const.HTTP_SERVER_FORMAT, ip) + url
                    localMusic.title = name
                    localMusic.album = album
                    localMusic.artist = artist
                    localMusic.coverPath = coverPath

                    val intent = Intent(client.context, MusicActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra(MusicActivity.EXTRA_MUSIC_INFO, localMusic)
                    intent.putExtra(MusicActivity.EXTRA_DEVICE_TYPE, MusicActivity.TYPE_RECEIVER)
                    client.context.startActivity(intent)
                }
            }
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            url = splits[1]
            name = splits[2]
            artist = splits[3]
            album = splits[4]
            coverPath = splits[5]
            true
        } else {
            false
        }
    }

    companion object {
        const val LENGTH = 6
    }

    init {
        cmd = IpMessageConst.MEDIA_AUDIO_INFO
    }
}