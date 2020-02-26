package com.absinthe.kage.ui.sender;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.databinding.ActivityMusicBinding;
import com.absinthe.kage.media.audio.LocalMusic;
import com.absinthe.kage.media.audio.MusicHelper;
import com.blankj.utilcode.util.ImageUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class MusicActivity extends BaseActivity {

    public static final String EXTRA_MUSIC_INFO = "MUSIC_INFO";

    private ActivityMusicBinding mBinding;
    private LocalMusic mLocalMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMusicBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        Intent intent = getIntent();
        if (intent != null) {
            getMusicInfo(intent);
            initView();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            getMusicInfo(intent);
        }
    }

    private void getMusicInfo(@NonNull Intent intent) {
        LocalMusic localMusic = intent.getParcelableExtra(EXTRA_MUSIC_INFO);
        if (localMusic != null) {
            mLocalMusic = localMusic;
            mBinding.tvMusicName.setText(mLocalMusic.getTitle());
            mBinding.tvArtist.setText(mLocalMusic.getArtist());
        }
    }

    private void initView() {
        Window window = getWindow();
        View view = window.getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        window.setStatusBarColor(Color.TRANSPARENT);

        Glide.with(this)
                .asBitmap()
                .load(MusicHelper.getAlbumArt(this, mLocalMusic.getAlbumId()))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Bitmap result = ImageUtils.renderScriptBlur(resource, 25);
                        mBinding.ivBack.setImageBitmap(result);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }
}
