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
        mDevice?.let {
            it.sendCommand(RemoteControlKeyCommand().apply {
                keyCode = key
            })
        }
    }
}