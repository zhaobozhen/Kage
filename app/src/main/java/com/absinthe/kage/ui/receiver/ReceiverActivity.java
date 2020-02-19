package com.absinthe.kage.ui.receiver;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.R;
import com.absinthe.kage.databinding.ActivityReceiverBinding;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class ReceiverActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "IMAGE_URI";
    public static final String EXTRA_FINISH = "FINISH";

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
            Log.d("sasa",imageUri);
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
        getSupportActionBar().setTitle("Loading…");
        Glide.with(this)
                .load(imageUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        binding.imageView.setImageDrawable(resource);
                        getSupportActionBar().setTitle(R.string.receiver_label);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }
}