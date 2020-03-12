package com.absinthe.kage.utils.timber

import timber.log.Timber.DebugTree

open class ThreadAwareDebugTree : DebugTree() {

    override fun createStackElementTag(element: StackTraceElement): String? {
        //日志显示行号
        return super.createStackElementTag(element) + " (Line ${element.lineNumber})"
    }
}