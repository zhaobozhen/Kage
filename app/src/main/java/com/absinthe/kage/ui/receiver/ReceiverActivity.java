package com.absinthe.kage.ui.receiver;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.databinding.ActivityReceiverBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.model.DeviceInfo;
import com.absinthe.kage.service.TCPService;
import com.absinthe.kage.utils.NotificationUtils;
import com.absinthe.kage.utils.ScanDeviceTool;

import java.util.List;

public class ReceiverActivity extends AppCompatActivity {

    private ActivityReceiverBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();

        new Thread(() -> {
            ScanDeviceTool tool = new ScanDeviceTool();
            tool.scan();
        }).start();
    }

    private void initView() {
        binding.btnStartService.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiverActivity.this, TCPService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationUtils.createTCPChannel(ReceiverActivity.this);
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            List<DeviceInfo> list = DeviceManager.Singleton.INSTANCE.getInstance().getDeviceInfoList();
            for (DeviceInfo info : list) {
                Log.d("sasa", info.getName());
            }
        });
        binding.btnStopService.setOnClickListener(v -> {
            Intent intent = new Intent(ReceiverActivity.this, TCPService.class);
            stopService(intent);
        });
    }
}