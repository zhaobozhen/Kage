package com.absinthe.kage.ui.sender;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivitySenderBinding;
import com.absinthe.kage.utils.ToastUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;

import java.util.List;

public class SenderActivity extends AppCompatActivity {

    private static final String TAG = SenderActivity.class.getSimpleName();
    private static final int REQUEST_CODE_CHOOSE = 1001;
    private ActivitySenderBinding binding;
    private List<String> mSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    private void initView() {
        binding.btnCastImage.setOnClickListener(v ->
                new RxPermissions(this).request(Manifest.permission.READ_EXTERNAL_STORAGE)
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK && data != null) {
            mSelected = Matisse.obtainPathResult(data);
            Log.d(TAG, "mSelected: " + mSelected);
        }
    }
}