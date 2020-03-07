package com.absinthe.kage.connect.tcp

import android.util.Log
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

open class Request : Packet() {

    private val responses = ArrayBlockingQueue<Response>(1)
    var id: String? = null

    fun setResponse(response: Response) {
        try {
            responses.put(response)
        } catch (e: InterruptedException) {
            Log.e(TAG, e.toString())
        }
    }

    fun waitResponse(timeout: Int): Response? {
        var response: Response? = null
        try {
            response = responses.poll(timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return response
    }

    companion object {
        private val TAG = Request::class.java.simpleName
    }
}