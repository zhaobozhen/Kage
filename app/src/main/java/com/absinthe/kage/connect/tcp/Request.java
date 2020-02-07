package com.absinthe.kage.connect.tcp;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Request extends Packet {
    private static final String TAG = Request.class.getSimpleName();
    private String id;
    private ArrayBlockingQueue<Response> responses = new ArrayBlockingQueue<Response>(1);

    public void setResponse(Response response) {
        try {
            responses.put(response);
        } catch (InterruptedException e) {
            Log.e(TAG, e.toString());
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Response waitResponse(int timeout) {
        Response response = null;
        try {
            response = responses.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return response;
    }
}
