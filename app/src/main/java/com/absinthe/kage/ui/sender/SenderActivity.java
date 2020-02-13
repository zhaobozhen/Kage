package com.absinthe.kage.ui.sender;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivitySenderBinding;

public class SenderActivity extends AppCompatActivity {

    private ActivitySenderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}