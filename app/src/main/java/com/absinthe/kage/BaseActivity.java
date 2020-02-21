package com.absinthe.kage;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.absinthe.kage.device.DeviceManager;
import com.absinthe.kage.device.cmd.DeviceRotationCommand;
import com.absinthe.kage.manager.ActivityStackManager;

import java.lang.ref.WeakReference;

public class BaseActivity extends AppCompatActivity {

    private WeakReference<BaseActivity> reference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reference = new WeakReference<>(this);
        ActivityStackManager.getInstance().addActivity(reference);
    }

    @Override
    protected void onDestroy() {
        ActivityStackManager.getInstance().removeActivity(reference);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DeviceManager deviceManager = DeviceManager.Singleton.INSTANCE.getInstance();
        if (deviceManager.isConnected()) {
            DeviceRotationCommand command = new DeviceRotationCommand();
            command.flag = newConfig.orientation;
            deviceManager.sendCommandToCurrentDevice(command);
        }
    }
}
