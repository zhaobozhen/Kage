package com.absinthe.kage.utils.timber

import timber.log.Timber.DebugTree

open class ThreadAwareDebugTree : DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        var newTag = tag
        if (newTag != null) {
            val threadName = Thread.currentThread().name
            newTag = "<$threadName> $tag"
        }
        super.log(priority, newTag, message, t)
    }

    override fun createStackElementTag(element: StackTraceElement): String? {
        //日志显示行号
        return super.createStackElementTag(element) + " (Line ${element.lineNumber})"
    }
}