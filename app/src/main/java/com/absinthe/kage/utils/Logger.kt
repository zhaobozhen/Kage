package com.absinthe.kage.utils

import android.util.Log

/**
 * Logger
 */
object Logger {

    var isDebugMode = false

    fun v(vararg contents: Any?): Int {
        val sb = StringBuilder()
        for (obj in contents) {
            if (obj == null) {
                sb.append("NULL").append(" ")
            } else {
                sb.append(obj.toString()).append(" ")
            }
        }
        var clsName = Throwable().stackTrace[1].className
        clsName = clsName.substring(clsName.lastIndexOf(".") + 1)
        val tag = "|" + clsName + "#" + Throwable().stackTrace[1].methodName + "()|"

        return if (isDebugMode) Log.v(tag, sb.toString()) else -1
    }

    @JvmStatic
    fun d(vararg contents: Any?): Int {
        val sb = StringBuilder()
        for (obj in contents) {
            if (obj == null) {
                sb.append("NULL").append(" ")
            } else {
                sb.append(obj.toString()).append(" ")
            }
        }
        var clsName = Throwable().stackTrace[1].className
        clsName = clsName.substring(clsName.lastIndexOf(".") + 1)
        val tag = "|" + clsName + "#" + Throwable().stackTrace[1].methodName + "()|"

        return if (isDebugMode) Log.d(tag, sb.toString()) else -1
    }

    fun i(vararg contents: Any?): Int {
        val sb = StringBuilder()
        for (obj in contents) {
            if (obj == null) {
                sb.append("NULL").append(" ")
            } else {
                sb.append(obj.toString()).append(" ")
            }
        }
        var clsName = Throwable().stackTrace[1].className
        clsName = clsName.substring(clsName.lastIndexOf(".") + 1)
        val tag = "|" + clsName + "#" + Throwable().stackTrace[1].methodName + "()|"

        return Log.i(tag, sb.toString())
    }

    fun e(vararg contents: Any?): Int {
        val sb = StringBuilder()
        for (obj in contents) {
            if (obj == null) {
                sb.append("NULL").append(" ")
            } else {
                sb.append(obj.toString()).append(" ")
            }
        }
        var clsName = Throwable().stackTrace[1].className
        clsName = clsName.substring(clsName.lastIndexOf(".") + 1)
        val tag = "|" + clsName + "#" + Throwable().stackTrace[1].methodName + "()|"

        return Log.e(tag, sb.toString())
    }

    fun w(vararg contents: Any?): Int {
        val sb = StringBuilder()
        for (obj in contents) {
            if (obj == null) {
                sb.append("NULL").append(" ")
            } else {
                sb.append(obj.toString()).append(" ")
            }
        }
        var clsName = Throwable().stackTrace[1].className
        clsName = clsName.substring(clsName.lastIndexOf(".") + 1)
        val tag = "|" + clsName + "#" + Throwable().stackTrace[1].methodName + "()|"

        return if (isDebugMode) Log.w(tag, sb.toString()) else -1
    }

    fun runningHere(): Int {
        var clsName = Throwable().stackTrace[1].className
        clsName = clsName.substring(clsName.lastIndexOf(".") + 1)
        val tag = "|" + clsName + "#" + Throwable().stackTrace[1].methodName + "()|"

        return if (isDebugMode) Log.d(tag, " is running here") else -1
    }
}