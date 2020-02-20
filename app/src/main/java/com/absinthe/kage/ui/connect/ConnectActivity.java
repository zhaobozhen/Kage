package com.absinthe.kage.ui.connect;

import android.os.Bundle;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.absinthe.kage.BaseActivity;
import com.absinthe.kage.R;
import com.absinthe.kage.adapter.DeviceAdapter;
import com.absinthe.kage.databinding.ActivityConnectBinding;
import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.DeviceObserverImpl;
import com.absinthe.kage.device.IDeviceObserver;
import com.absinthe.kage.device.model.DeviceInfo;
import com.absinthe.kage.utils.ToastUtil;

import java.util.List;

public class ConnectActivity extends BaseActivity {

    private static final String TAG = ConnectActivity.class.getSimpleName();
    private static final int VF_EMPTY = 0;
    private static final int VF_DEVICE_LIST = 1;

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

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    private void initView() {
        mDeviceManager = DeviceManager.Singleton.INSTANCE.getInstance();

        binding.vfContainer.setInAnimation(this, R.anim.anim_fade_in);
        binding.vfContainer.setOutAnimation(this, R.anim.anim_fade_out);
        mAdapter = new DeviceAdapter();
        binding.rvDevices.setAdapter(mAdapter);
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        List<DeviceInfo> list = mDeviceManager.getDeviceInfoList();
        if (list != null && list.size() > 0) {
            mAdapter.setNewData(list);
            switchContainer(VF_DEVICE_LIST);
        }
    }

    private void initObserver() {
        mObserver = new DeviceObserverImpl() {
            @Override
            public void onFindDevice(DeviceInfo deviceInfo) {
                Log.d(TAG, "onFindDevice: " + deviceInfo);
                mAdapter.addData(deviceInfo);
                switchContainer(VF_DEVICE_LIST);
            }

            @Override
            public void onLostDevice(DeviceInfo deviceInfo) {
                Log.d(TAG, "onLostDevice: " + deviceInfo);
                mAdapter.remove(deviceInfo);
                if (mAdapter.getItemCount() == 0) {
                    switchContainer(VF_EMPTY);
                }
            }

            @Override
            public void onDeviceConnected(DeviceInfo deviceInfo) {
                Log.d(TAG, "onDeviceConnected");
                mAdapter.notifyDataSetChanged();
                finish();
                ToastUtil.makeText(R.string.toast_connected);
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

    private void switchContainer(int flag) {
        if (flag == VF_EMPTY) {
            if (binding.vfContainer.getDisplayedChild() == VF_DEVICE_LIST) {
                binding.vfContainer.setDisplayedChild(VF_EMPTY);
            }
        } else {
            if (binding.vfContainer.getDisplayedChild() == VF_EMPTY) {
                binding.vfContainer.setDisplayedChild(VF_DEVICE_LIST);
            }
        }
    }
}