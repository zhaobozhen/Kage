package com.absinthe.kage.connect.protocol

import com.absinthe.kage.connect.protocol.IProtocolHandler.IProtocolHandleCallback
import com.absinthe.kage.connect.tcp.KageSocket
import com.absinthe.kage.device.Device
import com.absinthe.kage.device.cmd.InquiryDeviceInfoCommand
import com.absinthe.kage.device.model.DeviceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

class ProtocolHandler(
        private val mDevice: Device,
        private val mConfig: DeviceConfig,
        private val mCallback: IProtocolHandleCallback) : IProtocolHandler {

    private var hasHandShake = false

    override fun handleSocketConnectedEvent() {
        val inquiryDeviceInfoCommand = InquiryDeviceInfoCommand().apply {
            phoneName = mConfig.name
        }
        mDevice.sendCommand(inquiryDeviceInfoCommand)
    }

    override fun handleSocketMassage(msg: String) {
        val split = msg.split(IpMessageProtocol.DELIMITER).toTypedArray()
        try {
            when (split[0].toInt()) {
                IpMessageConst.GET_DEVICE_INFO -> {
                    hasHandShake = true
                    GlobalScope.launch(Dispatchers.Main) {
                        mCallback.onProtocolConnected()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("protocol invalid: $e")
        }
    }

    override fun handleSocketDisConnectEvent() {
        if (!hasHandShake) {
            // 握手期间断开了连接判定为连接失败
            GlobalScope.launch(Dispatchers.Main) {
                mCallback.onProtocolConnectedFailed(KageSocket.ISocketCallback.CONNECT_ERROR_CODE_HAND_SHAKE_NOT_COMPLETE, null)
            }
            return
        }
        hasHandShake = false
        GlobalScope.launch(Dispatchers.Main) {
            mCallback.onProtocolDisConnect()
        }
    }

    override fun handleSocketConnectFail(errorCode: Int, e: Exception) {
        GlobalScope.launch(Dispatchers.Main) {
            mCallback.onProtocolConnectedFailed(errorCode, e)
        }
    }

    override fun handleSocketSendOrReceiveError() {
        GlobalScope.launch(Dispatchers.Main) {
            mCallback.onProtocolSendOrReceiveError()
        }
    }
}