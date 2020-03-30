package com.absinthe.kage.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.OnLifecycleEvent
import com.absinthe.kage.R
import com.absinthe.kage.connect.protocol.Config
import com.absinthe.kage.connect.proxy.AudioProxy
import com.absinthe.kage.connect.proxy.ImageProxy
import com.absinthe.kage.connect.proxy.RemoteControlProxy
import com.absinthe.kage.connect.proxy.VideoProxy
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.DeviceObserverImpl
import com.absinthe.kage.device.client.Client
import com.absinthe.kage.device.model.DeviceInfo
import com.absinthe.kage.device.server.KageServer
import com.absinthe.kage.manager.ActivityStackManager
import com.absinthe.kage.ui.main.MainActivity
import com.absinthe.kage.utils.NotificationUtils
import com.absinthe.kage.utils.NotificationUtils.createTCPChannel
import com.zhihu.matisse.internal.entity.SelectionSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket

class TCPService : LifecycleService() {

    private lateinit var mServerSocket: ServerSocket
    private val deviceObserver = object : DeviceObserverImpl() {
        override fun onDeviceConnected(deviceInfo: DeviceInfo) {
            SelectionSpec.getInstance().isConnect = true
        }

        override fun onDeviceDisConnect(deviceInfo: DeviceInfo) {
            SelectionSpec.getInstance().isConnect = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, notificationInstance)

        DeviceManager.apply {
            init()
            addProxy(this)
            scanPeriod = 2000
            startMonitorDevice()
            lifecycle.addObserver(this)
            register(deviceObserver)
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                mServerSocket = ServerSocket(Config.PORT)

                while (true) {
                    val socket = mServerSocket.accept()
                    val dis = DataInputStream(socket.getInputStream())
                    val dos = DataOutputStream(socket.getOutputStream())
                    Client(this@TCPService, socket, dis, dos).start()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        val kageServer = KageServer()
        try {
            kageServer.start()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onStart(intent: Intent?, startId: Int) {
        super.onStart(intent, startId)
        startForeground(1, notificationInstance)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val activity = ActivityStackManager.topActivity
        if (activity is MainActivity) {
            activity.viewModel.isServiceRunning.value = true
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        DeviceManager.unregister(deviceObserver)

        try {
            val activity = ActivityStackManager.topActivity
            if (activity is MainActivity) {
                activity.viewModel.isServiceRunning.value = false
            }
        } catch (e: NoSuchElementException) {
            Timber.e(e)
        }

        super.onDestroy()
    }

    private val notificationInstance: Notification
        get() {
            val notifyIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0,
                    notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            return NotificationCompat.Builder(this, NotificationUtils.TCP_CHANNEL_ID)
                    .setContentTitle(getText(R.string.notification_channel_tcp))
                    .setContentText(getText(R.string.kage_service_notification_content))
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_stat_logo)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build()
        }

    private fun addProxy(deviceManager: DeviceManager) {
        deviceManager.apply {
            addProxy(ImageProxy)
            addProxy(AudioProxy)
            addProxy(VideoProxy)
            addProxy(RemoteControlProxy)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun removeProxy(deviceManager: DeviceManager) {
        deviceManager.apply {
            removeProxy(ImageProxy)
            removeProxy(AudioProxy)
            removeProxy(VideoProxy)
            removeProxy(RemoteControlProxy)
        }
    }

    companion object {

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun start(context: Context) {
            val intent = Intent(context, TCPService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createTCPChannel(context)
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun stop(context: Context) {
            context.stopService(
                    Intent(context, TCPService::class.java)
            )
        }
    }
}