package com.absinthe.kage.ui.receiver;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.absinthe.anywhere_.utils.AnimationUtil;
import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.databinding.ActivityReceiverBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class ReceiverActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_URI = "IMAGE_URI";
    public static final String EXTRA_FINISH = "FINISH";

    private ActivityReceiverBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        processIntent(intent);
        super.onNewIntent(intent);
    }

    private void initView() {
        Window window = getWindow();
        View view = window.getDecorView();
        view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
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
        showLoading();
        Glide.with(this)
                .load(imageUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .thumbnail(0.1f)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        binding.imageView.setImageDrawable(resource);
                        hideLoading();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void showLoading() {
        AnimationUtil.showAndHiddenAnimation(binding.layoutLoading.getRoot(), AnimationUtil.AnimationState.STATE_SHOW, 300);
    }

    private void hideLoading() {
        AnimationUtil.showAndHiddenAnimation(binding.layoutLoading.getRoot(), AnimationUtil.AnimationState.STATE_GONE, 300);
    }
}