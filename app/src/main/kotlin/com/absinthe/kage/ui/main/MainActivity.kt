package com.absinthe.kage.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.R
import com.absinthe.kage.viewholder.SpacesItemDecoration
import com.absinthe.kage.databinding.ActivityMainBinding
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.DeviceObserverImpl
import com.absinthe.kage.device.model.DeviceInfo
import com.absinthe.kage.service.TCPService
import com.absinthe.kage.ui.about.AboutActivity
import com.absinthe.kage.viewholder.delegate.CastItemViewBinder
import com.absinthe.kage.viewholder.delegate.ConnectItemViewBinder
import com.absinthe.kage.viewholder.delegate.DeviceItemViewBinder
import com.absinthe.kage.viewholder.delegate.ServiceRunningItemViewBinder
import com.absinthe.kage.viewholder.model.CastItem
import com.absinthe.kage.viewholder.model.ConnectItem
import com.absinthe.kage.viewholder.model.DeviceItem
import com.absinthe.kage.viewholder.model.ServiceRunningItem
import com.absinthe.kage.viewmodel.MainViewModel
import com.blankj.utilcode.util.ServiceUtils
import com.drakeet.multitype.MultiTypeAdapter

class MainActivity : BaseActivity() {

    lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private val adapter = MultiTypeAdapter()
    private val items = ArrayList<Any>()
    private val deviceObserver = object : DeviceObserverImpl() {

        override fun onDeviceConnected(deviceInfo: DeviceInfo) {
            val deviceItem = DeviceItem(deviceInfo.name, deviceInfo.ip)
            items.add(1, deviceItem)
            adapter.notifyItemInserted(1)
        }

        override fun onDeviceDisConnect(deviceInfo: DeviceInfo) {
            items.removeAt(1)
            adapter.notifyItemRemoved(1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
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

        adapter.register(ServiceRunningItemViewBinder())
        adapter.register(CastItemViewBinder())
        adapter.register(ConnectItemViewBinder())
        adapter.register(DeviceItemViewBinder())

        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        binding.recyclerview.addItemDecoration(SpacesItemDecoration(resources.getDimension(R.dimen.main_card_padding).toInt()))

        val serviceRunning = ServiceRunningItem(true)
        val castItem = CastItem()
        val connectItem = ConnectItem()

        items.add(serviceRunning)
        items.add(castItem)
        items.add(connectItem)

        adapter.items = items
        adapter.notifyDataSetChanged()

        viewModel.isServiceRunning.observe(this, Observer {
            (items[0] as ServiceRunningItem).isServiceRunning = it
            adapter.items = items
            adapter.notifyItemChanged(0)
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