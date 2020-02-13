package com.absinthe.kage.ui.sender;

import android.Manifest;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivitySenderBinding;
import com.tbruyelle.rxpermissions2.RxPermissions;

public class SenderActivity extends AppCompatActivity {

    private ActivitySenderBinding binding;
    private final RxPermissions rxPermissions = new RxPermissions(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    private void initView() {
        binding.btnCastImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rxPermissions
                        .request(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        });
    }
}