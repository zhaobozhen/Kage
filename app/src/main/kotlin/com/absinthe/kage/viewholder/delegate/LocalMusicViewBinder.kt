package com.absinthe.kage.viewholder.delegate

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.absinthe.kage.R
import com.absinthe.kage.media.audio.LocalMusic
import com.absinthe.kage.media.audio.MusicHelper
import com.absinthe.kage.ui.media.MusicActivity
import com.blankj.utilcode.util.ConvertUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.drakeet.multitype.ItemViewBinder

class LocalMusicViewBinder : ItemViewBinder<LocalMusic, LocalMusicViewBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root = inflater.inflate(R.layout.item_music, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: LocalMusic) {
        holder.musicName.text = item.title
        holder.artist.text = item.artist

        Glide.with(holder.itemView.context)
                .load(MusicHelper.getAlbumArt(item.albumId.toLong()))
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(RequestOptions.bitmapTransform(RoundedCorners(ConvertUtils.dp2px(3f))))
                .placeholder(R.drawable.ic_album)
                .into(holder.ivAlbum)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, MusicActivity::class.java).apply {
                putExtra(MusicActivity.EXTRA_MUSIC_INFO, item)
                putExtra(MusicActivity.EXTRA_DEVICE_TYPE, MusicActivity.TYPE_SENDER)
            }
            holder.itemView.context.startActivity(intent)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val musicName: TextView = itemView.findViewById(R.id.tv_music_name)
        val artist: TextView = itemView.findViewById(R.id.tv_artist)
        val ivAlbum: ImageView = itemView.findViewById(R.id.iv_album)
    }
}