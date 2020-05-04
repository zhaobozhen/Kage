package com.absinthe.kage.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.R
import com.absinthe.kage.databinding.ActivityMainBinding
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.DeviceObserverImpl
import com.absinthe.kage.device.model.DeviceInfo
import com.absinthe.kage.manager.GlobalManager
import com.absinthe.kage.service.TCPService
import com.absinthe.kage.ui.about.AboutActivity
import com.absinthe.kage.viewholder.SpacesItemDecoration
import com.absinthe.kage.viewholder.delegate.CastItemViewBinder
import com.absinthe.kage.viewholder.delegate.ConnectItemViewBinder
import com.absinthe.kage.viewholder.delegate.DeviceItemViewBinder
import com.absinthe.kage.viewholder.delegate.ServiceRunningItemViewBinder
import com.absinthe.kage.viewholder.model.CastItem
import com.absinthe.kage.viewholder.model.ConnectItem
import com.absinthe.kage.viewholder.model.DeviceItem
import com.absinthe.kage.viewholder.model.ServiceRunningItem
import com.blankj.utilcode.util.ServiceUtils
import com.drakeet.multitype.MultiTypeAdapter

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private var deviceItem = DeviceItem()

    private val items = ArrayList<Any>()
    private val adapter = MultiTypeAdapter(items)
    private val deviceObserver = object : DeviceObserverImpl() {

        override fun onDeviceConnected(deviceInfo: DeviceInfo) {
            deviceItem = DeviceItem(deviceInfo.name, deviceInfo.ip)
            items.add(1, deviceItem)
            adapter.notifyItemInserted(1)
        }

        override fun onDeviceDisConnect(deviceInfo: DeviceInfo) {
            items.remove(deviceItem)
            adapter.notifyItemRemoved(1)
        }
    }

    override fun setViewBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setToolbar() {
        mToolbar = binding.toolbar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DeviceManager.register(deviceObserver)

        initView()

        if (!ServiceUtils.isServiceRunning(TCPService::class.java)) {
            TCPService.start(this)
        }
    }

    override fun onDestroy() {
        TCPService.stop(this)
        DeviceManager.unregister(deviceObserver)
        super.onDestroy()
    }

    private fun initView() {
        setSupportActionBar(binding.toolbar)

        adapter.apply {
            register(ServiceRunningItemViewBinder())
            register(CastItemViewBinder())
            register(ConnectItemViewBinder())
            register(DeviceItemViewBinder())
        }

        binding.recyclerview.apply {
            adapter = this@MainActivity.adapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(SpacesItemDecoration(resources.getDimension(R.dimen.main_card_padding).toInt()))
        }

        val serviceRunning = ServiceRunningItem(true)
        val castItem = CastItem()
        val connectItem = ConnectItem()

        items.apply {
            add(serviceRunning)
            add(castItem)
            add(connectItem)
        }

        adapter.items = items
        adapter.notifyDataSetChanged()

        GlobalManager.isServiceRunning.observe(this, Observer {
            if (items.isEmpty()) {
                return@Observer
            }

            (items[0] as ServiceRunningItem).isServiceRunning = it

            var result = false
            if (!it) {
                result = items.remove(deviceItem)
            }

            adapter.items = items
            adapter.notifyItemChanged(0)

            if (!it && result) {
                adapter.notifyItemRemoved(1)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_about) {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }
}