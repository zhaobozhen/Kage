package com.absinthe.kage.connect.tcp

import com.absinthe.kage.device.heartbeat.ErrorResponse
import timber.log.Timber
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

open class Request : Packet() {

    var id: String = System.currentTimeMillis().toString()
    private val responses = ArrayBlockingQueue<Response>(1)

    fun setResponse(response: Response) {
        try {
            responses.put(response)
        } catch (e: InterruptedException) {
            Timber.e(e)
        }
    }

    fun waitResponse(timeout: Int): Response {
        return try {
            responses.poll(timeout.toLong(), TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
            ErrorResponse()
        }
    }
}