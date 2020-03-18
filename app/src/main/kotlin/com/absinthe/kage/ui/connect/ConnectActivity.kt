package com.absinthe.kage.ui.connect

import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.kage.BaseActivity
import com.absinthe.kage.R
import com.absinthe.kage.databinding.ActivityConnectBinding
import com.absinthe.kage.device.DeviceManager
import com.absinthe.kage.device.DeviceObserverImpl
import com.absinthe.kage.device.IDeviceObserver
import com.absinthe.kage.device.model.DeviceInfo
import com.absinthe.kage.utils.ToastUtil.makeText
import com.absinthe.kage.viewholder.DeviceInfoItemViewBinder
import com.drakeet.multitype.MultiTypeAdapter
import timber.log.Timber

class ConnectActivity : BaseActivity() {

    private lateinit var mBinding: ActivityConnectBinding
    private lateinit var mObserver: IDeviceObserver
    private var mAdapter = MultiTypeAdapter()
    private var mItems = ArrayList<Any>()
    private var mDeviceManager: DeviceManager = DeviceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initView()
        initObserver()
    }

    override fun onDestroy() {
        mDeviceManager.unregister(mObserver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        mAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initView() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mBinding.vfContainer.setInAnimation(this, R.anim.anim_fade_in)
        mBinding.vfContainer.setOutAnimation(this, R.anim.anim_fade_out)

        mAdapter.register(DeviceInfoItemViewBinder())
        mBinding.rvDevices.adapter = mAdapter
        mBinding.rvDevices.layoutManager = LinearLayoutManager(this)

        mItems.clear()
        mItems.addAll(mDeviceManager.deviceInfoList)
        if (mItems.isNotEmpty()) {
            mAdapter.items = mItems
            switchContainer(VF_DEVICE_LIST)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initObserver() {
        mObserver = object : DeviceObserverImpl() {

            override fun onFindDevice(deviceInfo: DeviceInfo) {
                Timber.d("onFindDevice: $deviceInfo")
                mItems.add(deviceInfo)
                mAdapter.items = mItems
                mAdapter.notifyItemInserted(mAdapter.itemCount - 1)
                switchContainer(VF_DEVICE_LIST)
            }

            override fun onLostDevice(deviceInfo: DeviceInfo) {
                Timber.d("onLostDevice: $deviceInfo")
                mItems.remove(deviceInfo)
                mAdapter.items = mItems
                mAdapter.notifyItemRangeRemoved(0, mAdapter.itemCount - 1)
                if (mAdapter.itemCount == 0) {
                    switchContainer(VF_EMPTY)
                }
            }

            override fun onDeviceConnected(deviceInfo: DeviceInfo) {
                Timber.d("onDeviceConnected")
                mAdapter.notifyDataSetChanged()
                finish()
                makeText(R.string.toast_connected)
            }

            override fun onDeviceConnecting(deviceInfo: DeviceInfo) {
                Timber.d("onDeviceConnecting")
                mAdapter.notifyDataSetChanged()
            }

            override fun onDeviceDisConnect(deviceInfo: DeviceInfo) {
                Timber.d("onDeviceDisConnect")
                mAdapter.notifyDataSetChanged()
            }

            override fun onDeviceConnectFailed(deviceInfo: DeviceInfo, errorCode: Int, errorMessage: String?) {
                Timber.d("onDeviceConnectFailed")
            }
        }

        mDeviceManager.register(mObserver)
    }

    private fun switchContainer(flag: Int) {
        if (flag == VF_EMPTY) {
            if (mBinding.vfContainer.displayedChild == VF_DEVICE_LIST) {
                mBinding.vfContainer.displayedChild = VF_EMPTY
            }
        } else {
            if (mBinding.vfContainer.displayedChild == VF_EMPTY) {
                mBinding.vfContainer.displayedChild = VF_DEVICE_LIST
            }
        }
    }

    companion object {
        private const val VF_EMPTY = 0
        private const val VF_DEVICE_LIST = 1
    }
}