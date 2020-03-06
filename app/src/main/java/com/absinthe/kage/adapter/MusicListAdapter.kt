package com.absinthe.kage.adapter

import android.widget.ImageView
import com.absinthe.kage.R
import com.absinthe.kage.media.audio.LocalMusic
import com.absinthe.kage.media.audio.MusicHelper
import com.blankj.utilcode.util.ConvertUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder

class MusicListAdapter : BaseQuickAdapter<LocalMusic, BaseViewHolder>(R.layout.item_music) {

    override fun convert(helper: BaseViewHolder, item: LocalMusic) {
        helper.setText(R.id.tv_music_name, item.title)
        helper.setText(R.id.tv_artist, item.artist)
        val ivAlbum: ImageView = helper.getView(R.id.iv_album)
        Glide.with(context)
                .load(MusicHelper.getAlbumArt(context, item.albumId.toLong()))
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.bitmapTransform(RoundedCorners(ConvertUtils.dp2px(3f))))
                .placeholder(R.drawable.ic_album)
                .into(ivAlbum)
    }
}