package com.absinthe.kage.server;

import com.absinthe.kage.protocol.Config;

import fi.iki.elonen.NanoHTTPD;

public class KageServer extends NanoHTTPD {

    public KageServer() {
        super(Config.HTTP_SERVER_PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><body>");
        builder.append("<p>Hello! This is Kage HTTP Server.</p>");
        builder.append("<h1>:)</h1></body></html>\n");
        return newFixedLengthResponse(builder.toString());
    }
}
