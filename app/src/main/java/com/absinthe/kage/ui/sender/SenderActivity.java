package com.absinthe.kage.ui.sender;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.R;
import com.absinthe.kage.connect.proxy.ImageProxy;
import com.absinthe.kage.databinding.ActivitySenderBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.ui.connect.ConnectActivity;
import com.absinthe.kage.utils.ToastUtil;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.listener.OnChooseItemListener;

public class SenderActivity extends BaseActivity {

    private static final String TAG = SenderActivity.class.getSimpleName();
    private static final int REQUEST_CODE_CHOOSE = 1001;
    private ActivitySenderBinding binding;
    private OnChooseItemListener mListener;
    private final RxPermissions rxPermissions = new RxPermissions(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
    }

    @Override
    protected void onDestroy() {
        ImageProxy.getInstance().close();
        super.onDestroy();
    }

    private void initView() {
        mListener = new OnChooseItemListener() {
            @Override
            public void onChoose(String itemUri) {
                if (DeviceManager.Singleton.INSTANCE.getInstance().isConnected()) {
                    ImageProxy.getInstance().cast(itemUri);
                } else {
                    startActivity(new Intent(SenderActivity.this, ConnectActivity.class));
                    finish();
                }
            }

            @Override
            public void onStop() {
                if (DeviceManager.Singleton.INSTANCE.getInstance().isConnected()) {
                    ImageProxy.getInstance().stop();
                } else {
                    startActivity(new Intent(SenderActivity.this, ConnectActivity.class));
                    finish();
                }
            }

            @Override
            public void onPreview() {
                Log.d(TAG, "onPreview()");
            }

            @Override
            public void onNext() {
                Log.d(TAG, "onNext()");
            }
        };

        binding.btnCastImage.setOnClickListener(v ->
                rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .subscribe(grant -> {
                            if (grant) {
                                Matisse.from(SenderActivity.this)
                                        .choose(MimeType.ofImage())
                                        .countable(false)
                                        .maxSelectable(1)
                                        .theme(com.zhihu.matisse.R.style.Matisse_Dracula)
                                        .setOnChooseItemListener(mListener)
                                        .imageEngine(new GlideEngine())
                                        .forResult(REQUEST_CODE_CHOOSE);
                            } else {
                                ToastUtil.makeText(R.string.toast_grant_storage_perm);
                            }
                        }));
        binding.btnCastVideo.setOnClickListener(v ->
                rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .subscribe(grant -> {
                            if (grant) {
                                Matisse.from(SenderActivity.this)
                                        .choose(MimeType.ofVideo())
                                        .countable(false)
                                        .maxSelectable(1)
                                        .theme(com.zhihu.matisse.R.style.Matisse_Dracula)
                                        .setOnChooseItemListener(mListener)
                                        .imageEngine(new GlideEngine())
                                        .forResult(REQUEST_CODE_CHOOSE);
                            } else {
                                ToastUtil.makeText(R.string.toast_grant_storage_perm);
                            }
                        }));
        binding.btnCastMusic.setOnClickListener(v ->
                rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .subscribe(grant -> {
                            if (grant) {
                                startActivity(new Intent(this, MusicListActivity.class));
                            } else {
                                ToastUtil.makeText(R.string.toast_grant_storage_perm);
                            }
                        }));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CHOOSE && resultCode == RESULT_OK && data != null) {
            Log.d(TAG, "mSelected: " + Matisse.obtainPathResult(data));
        }
    }
}