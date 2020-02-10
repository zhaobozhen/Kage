package com.absinthe.kage.ui.sender;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.client.ConnectionClient;
import com.absinthe.kage.client.IRequestCallBack;
import com.absinthe.kage.databinding.ActivitySenderBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.IDeviceObserver;
import com.absinthe.kage.device.model.DeviceInfo;
import com.absinthe.kage.protocol.BaseProtocol;
import com.absinthe.kage.utils.ToastUtil;

public class SenderActivity extends AppCompatActivity {

    private ActivitySenderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ConnectionClient connectionClient = new ConnectionClient("192.168.1.113", new IRequestCallBack() {
            @Override
            public void onSuccess(BaseProtocol msg) {
                Log.d("sasa", "onSuccess");
            }

            @Override
            public void onFailed(int errorCode, String msg) {

            }
        });
    }
}