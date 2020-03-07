package com.absinthe.kage

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.cmd.DeviceRotationCommand
import com.absinthe.kage.manager.ActivityStackManager
import java.lang.ref.WeakReference

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {

    private lateinit var reference: WeakReference<BaseActivity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reference = WeakReference(this)
        ActivityStackManager.addActivity(reference)
    }

    override fun onDestroy() {
        ActivityStackManager.removeActivity(reference)
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val deviceManager = DeviceManager.Singleton.INSTANCE.instance
        if (deviceManager.isConnected) {
            val command = DeviceRotationCommand()
            command.flag = newConfig.orientation
            deviceManager.sendCommandToCurrentDevice(command)
        }
    }
}