package com.absinthe.kage

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.cmd.DeviceRotationCommand
import com.absinthe.kage.manager.ActivityStackManager
import com.absinthe.kage.utils.UiUtils
import com.absinthe.kage.utils.UiUtils.setDarkMode
import com.blankj.utilcode.util.BarUtils
import java.lang.ref.WeakReference

@SuppressLint("Registered")
abstract class BaseActivity : AppCompatActivity() {

    private lateinit var reference: WeakReference<BaseActivity>
    protected var mToolbar: Toolbar? = null

    protected abstract fun setViewBinding()
    protected abstract fun setToolbar()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reference = WeakReference(this)
        ActivityStackManager.addActivity(reference)
        setViewBinding()

        setDarkMode(this)
        UiUtils.setSystemBarTransparent(this)
        setToolbar()
        mToolbar?.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0)
    }

    override fun onDestroy() {
        ActivityStackManager.removeActivity(reference)
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (DeviceManager.isConnected) {
            val command = DeviceRotationCommand().apply {
                flag = newConfig.orientation
            }
            DeviceManager.sendCommandToCurrentDevice(command)
        }
    }
}