package com.absinthe.kage.device.cmd

import android.content.Intent
import android.text.TextUtils
import com.absinthe.kage.connect.Const
import com.absinthe.kage.connect.protocol.IpMessageConst
import com.absinthe.kage.connect.protocol.IpMessageProtocol
import com.absinthe.kage.device.Command
import com.absinthe.kage.device.CommandBuilder
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.device.model.AudioInfo
import com.absinthe.kage.media.audio.LocalMusic
import com.absinthe.kage.ui.media.MusicActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class PlayAudioListCommand : Command() {
    @JvmField
    var index = 0
    @JvmField
    var size = 0
    @JvmField
    var listInfo: String? = null

    override fun pack(): String {
        return CommandBuilder()
                .with(this)
                .append(index.toString())
                .append(size.toString())
                .append(listInfo)
                .build()
    }

    override fun doWork(client: Client, received: String) {
        if (parseReceived(received)) {
            if (!TextUtils.isEmpty(listInfo) && client.deviceInfo != null) {
                val localMusicList = Gson().fromJson<List<AudioInfo>>(listInfo, object : TypeToken<List<AudioInfo?>?>() {}.type)

                if (localMusicList != null && localMusicList.isNotEmpty()) {
                    val audioInfo = localMusicList[0]
                    val ip = client.deviceInfo!!.ip

                    if (!TextUtils.isEmpty(ip)) {
                        val localMusic = LocalMusic()
                        localMusic.title = audioInfo.name
                        localMusic.artist = audioInfo.artist
                        localMusic.album = audioInfo.album
                        localMusic.filePath = String.format(Const.HTTP_SERVER_FORMAT, ip) + audioInfo.url
                        localMusic.coverPath = String.format(Const.HTTP_SERVER_FORMAT, ip) + client.context.externalCacheDir + File.separator + localMusic.albumId + ".png"

                        val intent = Intent(client.context, MusicActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra(MusicActivity.EXTRA_MUSIC_INFO, localMusic)
                        intent.putExtra(MusicActivity.EXTRA_DEVICE_TYPE, MusicActivity.TYPE_RECEIVER)
                        client.context.startActivity(intent)
                    }
                }
            }
        }
    }

    override fun parseReceived(received: String): Boolean {
        val splits = received.split(IpMessageProtocol.DELIMITER).toTypedArray()

        return if (splits.size == LENGTH) {
            try {
                index = splits[1].toInt()
                size = splits[2].toInt()
                listInfo = splits[3]
                true
            } catch (e: NumberFormatException) {
                e.printStackTrace()
                false
            }
        } else {
            false
        }
    }

    companion object {
        const val LENGTH = 4
    }

    init {
        cmd = IpMessageConst.MEDIA_PLAY_AUDIO_LIST
    }
}