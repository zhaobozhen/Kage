package com.absinthe.kage.connect.proxy

import android.view.KeyEvent
import com.absinthe.kage.device.cmd.RemoteControlKeyCommand

object RemoteControlProxy : BaseProxy() {

    fun sendVolumeUpKeyAction() {
        sendKeyAction(KeyEvent.KEYCODE_VOLUME_UP)
    }

    fun sendVolumeDownKeyAction() {
        sendKeyAction(KeyEvent.KEYCODE_VOLUME_DOWN)
    }

    private fun sendKeyAction(key: Int) {
        if (mDevice == null || !mDevice!!.isConnected) {
            return
        }
        val controlKeyCmd = RemoteControlKeyCommand().apply {
            keyCode = key
        }
        mDevice?.sendCommand(controlKeyCmd)
    }
}