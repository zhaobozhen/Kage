package com.absinthe.kage.ui.receiver;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivityReceiverBinding;
import com.absinthe.kage.service.TCPService;
import com.bumptech.glide.Glide;

public class ReceiverActivity extends AppCompatActivity {

    private ActivityReceiverBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    private void initView() {
        binding.btnLoad.setOnClickListener(v -> {
            Glide.with(this)
                    .load("http://192.168.1.151:2026/storage/emulated/0/luv/-50fbe9c07bc85335_1571964428216.jpg")
                    .into(binding.imageView);
        });
    }
}