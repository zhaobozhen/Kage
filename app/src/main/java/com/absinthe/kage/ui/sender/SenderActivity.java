package com.absinthe.kage.ui.sender;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivitySenderBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.IDeviceObserver;
import com.absinthe.kage.device.model.DeviceInfo;

public class SenderActivity extends AppCompatActivity {

    private ActivitySenderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
        initObserver();
    }

    private void initView() {

    }

    private void initObserver() {
        IDeviceObserver observer = new IDeviceObserver() {
            @Override
            public void onFindDevice(DeviceInfo deviceInfo) {
                binding.etIpAddress.setText(deviceInfo.getIp());
            }

            @Override
            public void onLostDevice(DeviceInfo deviceInfo) {

            }

            @Override
            public void onDeviceConnected(DeviceInfo deviceInfo) {

            }

            @Override
            public void onDeviceDisConnect(DeviceInfo deviceInfo) {

            }

            @Override
            public void onDeviceConnectFailed(DeviceInfo deviceInfo, int errorCode, String errorMessage) {

            }

            @Override
            public void onDeviceInfoChanged(DeviceInfo deviceInfo) {

            }

            @Override
            public void onDeviceNotice(DeviceInfo deviceInfo) {

            }

            @Override
            public void onDeviceConnecting(DeviceInfo deviceInfo) {

            }
        };
        DeviceManager.Singleton.INSTANCE.getInstance().register(observer);
    }
}