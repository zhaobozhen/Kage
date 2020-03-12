package com.absinthe.kage.connect.tcp

import timber.log.Timber
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

open class Request : Packet() {

    private val responses = ArrayBlockingQueue<Response>(1)
    var id: String? = null

    fun setResponse(response: Response) {
        try {
            responses.put(response)
        } catch (e: InterruptedException) {
            Timber.e(e)
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
}