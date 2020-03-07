package com.absinthe.kage.device.model

import com.google.gson.annotations.SerializedName

class AudioInfo {

    @SerializedName("url")
    var url: String? = null //播放地址

    @SerializedName("name")
    var name: String? = null //歌曲名

    @SerializedName("artist")
    var artist: String? = null //演唱者

    @SerializedName("album")
    var album: String? = null //专辑

    @SerializedName("coverPath")
    var coverPath: String? = null //封面地址

}