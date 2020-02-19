package com.absinthe.kage.ui.connect;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.absinthe.kage.R;
import com.absinthe.kage.adapter.DeviceAdapter;
import com.absinthe.kage.databinding.ActivityConnectBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.DeviceObserverImpl;
import com.absinthe.kage.device.IDeviceObserver;
import com.absinthe.kage.device.model.DeviceInfo;
import com.absinthe.kage.utils.ToastUtil;

import java.util.List;

public class ConnectActivity extends AppCompatActivity {

    private static final String TAG = ConnectActivity.class.getSimpleName();
    private ActivityConnectBinding binding;
    private DeviceAdapter mAdapter;
    private DeviceManager mDeviceManager;
    private IDeviceObserver mObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
        initObserver();
    }

    @Override
    protected void onDestroy() {
        mDeviceManager.unregister(mObserver);
        super.onDestroy();
    }

    private void initView() {
        mDeviceManager = DeviceManager.Singleton.INSTANCE.getInstance();

        mAdapter = new DeviceAdapter();
        binding.rvDevices.setAdapter(mAdapter);
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        List<DeviceInfo> list = mDeviceManager.getDeviceInfoList();
        if (list != null) {
            mAdapter.setNewData(list);
        }
    }

    private void initObserver() {
        mObserver = new DeviceObserverImpl() {
            @Override
            public void onFindDevice(DeviceInfo deviceInfo) {
                Log.d(TAG, "onFindDevice");
                mAdapter.addData(deviceInfo);
            }

            @Override
            public void onLostDevice(DeviceInfo deviceInfo) {
                Log.d(TAG, "onLostDevice");
                mAdapter.remove(deviceInfo);
            }

            @Override
            public void onDeviceConnected(DeviceInfo deviceInfo) {
                Log.d(TAG, "onDeviceConnected");
                mAdapter.notifyDataSetChanged();
                finish();
                ToastUtil.makeText("Connected");
            }

            @Override
            public void onDeviceConnecting(DeviceInfo deviceInfo) {
                Log.d(TAG, "onDeviceConnecting");
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDeviceDisConnect(DeviceInfo deviceInfo) {
                Log.d(TAG, "onDeviceDisConnect");
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onDeviceConnectFailed(DeviceInfo deviceInfo, int errorCode, String errorMessage) {
                Log.d(TAG, "onDeviceConnectFailed");
            }
        };
        mDeviceManager.register(mObserver);

        mAdapter.setOnItemChildClickListener((adapter, view, position) -> {
            if (view.getId() == R.id.btn_connect) {
                DeviceInfo deviceInfo = mAdapter.getItem(position);
                if (deviceInfo != null) {
                    if (!deviceInfo.isConnected()) {
                        mDeviceManager.onlineDevice(deviceInfo);
                        mDeviceManager.connectDevice(deviceInfo);
                    } else {
                        mDeviceManager.disConnectDevice();
                    }
                }
            }
        });
    }
}