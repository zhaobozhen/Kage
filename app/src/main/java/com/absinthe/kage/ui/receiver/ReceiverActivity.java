package com.absinthe.kage.ui.receiver;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivityReceiverBinding;
import com.absinthe.kage.server.ConnectionServer;
import com.absinthe.kage.service.TCPService;
import com.absinthe.kage.utils.NotificationUtils;

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
            ConnectionServer server = new ConnectionServer();
            server.start();
        });
        binding.btnStopService.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiverActivity.this, TCPService.class);
            stopService(intent);
        });
    }
}