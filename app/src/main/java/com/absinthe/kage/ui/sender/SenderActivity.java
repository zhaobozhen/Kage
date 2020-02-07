package com.absinthe.kage.ui.sender;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.client.ConnectionClient;
import com.absinthe.kage.client.IRequestCallBack;
import com.absinthe.kage.databinding.ActivitySenderBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.protocol.BaseProtocol;
import com.absinthe.kage.protocol.DataProtocol;

public class SenderActivity extends AppCompatActivity {

    private ActivitySenderBinding binding;
    private ConnectionClient mClient;
    private int count = 0;
    private DeviceManager mDeviceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
        mDeviceManager = DeviceManager.Singleton.INSTANCE.getInstance();
        mDeviceManager.init();
        mDeviceManager.startMonitorDevice();
    }

    private void initView() {
        binding.btnConnect.setOnClickListener(v -> {
            mClient = new ConnectionClient(binding.etIpAddress.getText().toString(), new IRequestCallBack() {
                @Override
                public void onSuccess(BaseProtocol msg) {
                    Toast.makeText(SenderActivity.this, "Success:" + new String(msg.genContentData()), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailed(int errorCode, String msg) {
                    Toast.makeText(SenderActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                }
            });
            getLifecycle().addObserver(mClient);
        });
        binding.btnSend.setOnClickListener(v -> {
            DataProtocol dataProtocol = new DataProtocol();
            dataProtocol.setDtype(1);
            dataProtocol.setPattion(2);
            dataProtocol.setMsgId(count++);
            dataProtocol.setData("Send data Absinthe");
            mClient.addNewRequest(dataProtocol);
        });
    }
}