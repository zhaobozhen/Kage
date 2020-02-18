package com.absinthe.kage.ui.sender;

import android.Manifest;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivitySenderBinding;
import com.absinthe.kage.utils.ToastUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;

public class SenderActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CHOOSE = 1001;
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
        binding.btnCastImage.setOnClickListener(v -> rxPermissions
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(grant -> {
                    if (grant) {
                        Matisse.from(SenderActivity.this)
                                .choose(MimeType.ofImage())
                                .countable(false)
                                .maxSelectable(1)
                                .imageEngine(new GlideEngine())
                                .forResult(REQUEST_CODE_CHOOSE);
                    } else {
                        ToastUtil.makeText("Please grant permissions");
                    }
                }).dispose());
    }
}