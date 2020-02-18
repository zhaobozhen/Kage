package com.absinthe.kage.server;

import android.os.Environment;
import android.util.Log;

import com.absinthe.kage.protocol.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class KageServer extends NanoHTTPD {

    private static final String TAG = KageServer.class.getSimpleName();
    private static final String ROOT_DIR = "/";

    public KageServer() {
        super(Config.HTTP_SERVER_PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String filepath = session.getUri().trim();
        File rootDir = Environment.getExternalStorageDirectory();
        File[] filesList;
        Log.d(TAG, "Session Uri = " + filepath);
        Log.d(TAG, "Environment.getExternalStorageDirectory() = " + Environment.getExternalStorageDirectory().getAbsolutePath());

        if (filepath.trim().isEmpty() || filepath.trim().equals(ROOT_DIR)) {
            filepath = rootDir.getAbsolutePath();
        }
        filesList = new File(filepath).listFiles();

        if (filesList != null) {
            return responsePage(filesList);
        } else {
            return responseFile(filepath);
        }
    }

    //对于请求目录的，返回文件列表
    public Response responsePage(File[] filesList) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPER html><html><body>");
        builder.append("<ol>");
        for (File detailsOfFiles : filesList) {
            builder.append("<a href=\"").append(detailsOfFiles.getAbsolutePath()).append("\" alt = \"\">").append(detailsOfFiles.getAbsolutePath()).append("</a><br>");
        }
        builder.append("</ol>");
        builder.append("</body></html>\n");
        //回送应答
        return newFixedLengthResponse(String.valueOf(builder));
    }

    //对于请求文件的，返回下载的文件
    public Response responseFile(String uri) {
        try {
            //文件输入流
            FileInputStream fis = new FileInputStream(uri);
            // 返回OK，同时传送文件，为了安全这里应该再加一个处理，即判断这个文件是否是我们所分享的文件，避免客户端访问了其他个人文件
            return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", fis, fis.available());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response404(uri);
    }

    //页面不存在，或者文件不存在时
    public Response response404(String url) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html>body>");
        builder.append("Sorry,Can't Found").append(url).append(" !");
        builder.append("</body></html>\n");
        return newFixedLengthResponse(builder.toString());
    }
}
