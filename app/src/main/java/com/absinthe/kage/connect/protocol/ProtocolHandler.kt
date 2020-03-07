package com.absinthe.kage.connect.protocol

import android.util.Log
import com.absinthe.kage.connect.protocol.IProtocolHandler.IProtocolHandleCallback
import com.absinthe.kage.connect.protocol.IProtocolHandler.KageProtocolThreadHandler.Companion.instance
import com.absinthe.kage.connect.tcp.KageSocket
import com.absinthe.kage.device.Device
import com.absinthe.kage.device.cmd.InquiryDeviceInfoCommand
import com.absinthe.kage.device.model.DeviceConfig

class ProtocolHandler(
        private val mDevice: Device,
        private val mConfig: DeviceConfig?,
        private val mCallback: IProtocolHandleCallback?) : IProtocolHandler {

    private var hasHandShake = false

    override fun handleSocketConnectedEvent() {
        val inquiryDeviceInfoCommand = InquiryDeviceInfoCommand()
        inquiryDeviceInfoCommand.phoneName = mConfig?.name ?: "Unknown"
        mDevice.sendCommand(inquiryDeviceInfoCommand)
    }

    override fun handleSocketMassage(msg: String) {
        val split = msg.split(IpMessageProtocol.DELIMITER).toTypedArray()
        try {
            when (split[0].toInt()) {
                IpMessageConst.GET_DEVICE_INFO -> {
                    hasHandShake = true
                    instance!!.post {
                        mCallback?.onProtocolConnected()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "protocol invalid: $e")
        }
    }

    override fun handleSocketDisConnectEvent() {
        if (!hasHandShake) {
            // 握手期间断开了连接判定为连接失败
            instance!!.post {
                mCallback?.onProtocolConnectedFailed(KageSocket.ISocketCallback.CONNECT_ERROR_CODE_HAND_SHAKE_NOT_COMPLETE, null)
            }
            return
        }
        hasHandShake = false
        instance!!.post {
            mCallback?.onProtocolDisConnect()
        }
    }

    override fun handleSocketConnectFail(errorCode: Int, e: Exception) {
        instance!!.post {
            mCallback?.onProtocolConnectedFailed(errorCode, e)
        }
    }

    override fun handleSocketSendOrReceiveError() {
        instance!!.post {
            mCallback?.onProtocolSendOrReceiveError()
        }
    }

    companion object {
        private val TAG = ProtocolHandler::class.java.simpleName
    }

}