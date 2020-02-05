package com.absinthe.kage.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivityMainBinding;
import com.absinthe.kage.ui.receiver.ReceiverActivity;
import com.absinthe.kage.ui.sender.SenderActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    private void initView() {
        binding.btnSender.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SenderActivity.class)));
        binding.btnReceiver.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ReceiverActivity.class)));
    }
}
