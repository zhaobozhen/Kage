package com.absinthe.kage.ui.receiver;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivityReceiverBinding;
import com.absinthe.kage.service.TCPService;

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
        binding.btnStartService.setOnClickListener(v -> {

        });
        binding.btnStopService.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiverActivity.this, TCPService.class);
            stopService(intent);
        });
    }
}