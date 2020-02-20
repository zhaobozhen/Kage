package com.absinthe.kage.ui.receiver;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.R;
import com.absinthe.kage.databinding.ActivityReceiverBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class ReceiverActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_URI = "IMAGE_URI";
    public static final String EXTRA_FINISH = "FINISH";
    private static final int TITLE_STATE_LOADING = 0;
    private static final int TITLE_STATE_NORMAL = 1;

    private ActivityReceiverBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        processIntent(intent);
        super.onNewIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (intent != null) {
            String imageUri = intent.getStringExtra(EXTRA_IMAGE_URI);
            if (!TextUtils.isEmpty(imageUri)) {
                if (imageUri.equals(EXTRA_FINISH)) {
                    finish();
                } else {
                    loadImage(imageUri);
                }
            }
        }
    }

    private void loadImage(String imageUri) {
        setTitleState(TITLE_STATE_LOADING);
        Glide.with(this)
                .load(imageUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .thumbnail(0.1f)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        binding.imageView.setImageDrawable(resource);
                        setTitleState(TITLE_STATE_NORMAL);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void setTitleState(int state) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            return;
        }

        if (state == TITLE_STATE_LOADING) {
            actionBar.setTitle("Loading…");
        } else {
            actionBar.setTitle(R.string.receiver_label);
        }
    }
}