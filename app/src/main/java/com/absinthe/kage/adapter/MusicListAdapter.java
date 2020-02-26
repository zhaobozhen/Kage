package com.absinthe.kage.adapter;

import android.widget.ImageView;

import com.absinthe.kage.R;
import com.absinthe.kage.media.audio.LocalMusic;
import com.absinthe.kage.media.audio.MusicHelper;
import com.blankj.utilcode.util.ConvertUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MusicListAdapter extends BaseQuickAdapter<LocalMusic, BaseViewHolder> {

    public MusicListAdapter() {
        super(R.layout.item_music);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder baseViewHolder, @Nullable LocalMusic localMusic) {
        if (localMusic != null) {
            baseViewHolder.setText(R.id.tv_music_name, localMusic.getTitle());
            baseViewHolder.setText(R.id.tv_artist, localMusic.getArtist());

            ImageView ivAlbum = baseViewHolder.findView(R.id.iv_album);
            if (ivAlbum != null) {
                Glide.with(getContext())
                        .load(MusicHelper.getAlbumArt(getContext(), localMusic.getAlbumId()))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(ConvertUtils.dp2px(3))))
                        .placeholder(R.drawable.ic_album)
                        .into(ivAlbum);
            }
        }
    }
}
