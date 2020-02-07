package com.absinthe.kage.ui.sender;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.client.ConnectionClient;
import com.absinthe.kage.client.IRequestCallBack;
import com.absinthe.kage.databinding.ActivitySenderBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.IDeviceObserver;
import com.absinthe.kage.device.model.DeviceInfo;
import com.absinthe.kage.protocol.BaseProtocol;
import com.absinthe.kage.protocol.DataProtocol;

public class SenderActivity extends AppCompatActivity {

    private ActivitySenderBinding binding;
    private ConnectionClient mClient;
    private DeviceManager mDeviceManager;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySenderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
        mDeviceManager = DeviceManager.Singleton.INSTANCE.getInstance();
        mDeviceManager.init();
        mDeviceManager.startMonitorDevice();
        initObserver();
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

    private void initObserver() {
        getLifecycle().addObserver(mDeviceManager);
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
        mDeviceManager.register(observer);
    }
}