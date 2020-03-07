package com.absinthe.kage.manager

import android.app.Activity
import android.os.Process
import android.util.Log
import com.absinthe.kage.BaseActivity
import java.lang.ref.WeakReference
import java.util.*

object ActivityStackManager {

    private const val TAG = "ActivityStackManager"

    /***
     * Activity Stack
     *
     * @return Activity stack
     */
    private var stack: Stack<WeakReference<BaseActivity>> = Stack()

    /***
     * Size of Activities
     *
     * @return Size of Activities
     */
    val stackSize: Int
        get() {
            return stack.size
        }

    /***
     * Get top stack Activity
     *
     * @return Activity
     */
    val topActivity: BaseActivity?
        get() {
            return stack.lastElement().get()
        }

    /**
     * Add Activity to stack
     */
    fun addActivity(activity: WeakReference<BaseActivity>) {
        stack.add(activity)
    }

    /**
     * Delete Activity
     *
     * @param activity Weak Reference of Activity
     */
    fun removeActivity(activity: WeakReference<BaseActivity>) {
        stack.remove(activity)
    }

    /***
     * Get Activity by class
     *
     * @param cls Activity class
     * @return Activity
     */
    fun getActivity(cls: Class<*>): BaseActivity? {
        var returnActivity: BaseActivity? = null
        for (activity in stack) {
            if (activity.get()!!.javaClass == cls) {
                returnActivity = activity.get()
                break
            }
        }
        return returnActivity
    }

    /**
     * Kill top stack Activity
     */
    fun killTopActivity() {
        try {
            val activity = stack.lastElement()
            killActivity(activity)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, e.toString())
        }
    }

    /***
     * Kill Activity
     *
     * @param activity Activity want to kill
     */
    private fun killActivity(activity: WeakReference<BaseActivity>) {
        try {
            val iterator = stack.iterator()
            while (iterator.hasNext()) {
                val stackActivity = iterator.next()
                if (stackActivity.get() == null) {
                    iterator.remove()
                    continue
                }
                if (stackActivity.get()!!.javaClass.name == activity.get()!!.javaClass.name) {
                    iterator.remove()
                    stackActivity.get()!!.finish()
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    /***
     * Kill Activity by class
     *
     * @param cls class
     */
    fun killActivity(cls: Class<*>) {
        try {
            val listIterator = stack.listIterator()
            while (listIterator.hasNext()) {
                val activity: Activity? = listIterator.next().get()
                if (activity == null) {
                    listIterator.remove()
                    continue
                }
                if (activity.javaClass == cls) {
                    listIterator.remove()
                    activity.finish()
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Kill all Activity
     */
    private fun killAllActivity() {
        try {
            val listIterator = stack.listIterator()
            while (listIterator.hasNext()) {
                val activity = listIterator.next().get()
                activity?.finish()
                listIterator.remove()
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    /**
     * Exit application
     */
    fun exitApp() {
        killAllActivity()
        Process.killProcess(Process.myPid())
    }
}