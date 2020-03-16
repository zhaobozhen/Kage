package com.absinthe.kage.device.server

import android.os.Environment
import com.absinthe.kage.connect.protocol.Config
import fi.iki.elonen.NanoHTTPD
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class KageServer : NanoHTTPD(Config.HTTP_SERVER_PORT) {

    override fun serve(session: IHTTPSession): Response {
        val rootDir = Environment.getExternalStorageDirectory()
        val filesList: Array<File>?
        val userAgent = session.headers["user-agent"] ?: ""
        var filepath = session.uri.trim { it <= ' ' }

        Timber.d("Session Uri = $filepath")
        Timber.d("header = $userAgent")

        if (!userAgent.contains("Dalvik")) {
            return responseNoAccess()
        }

        if (filepath.isEmpty() || filepath == ROOT_DIR) {
            filepath = rootDir.absolutePath
        }
        filesList = File(filepath).listFiles()
        return filesList?.let { responsePage(it) } ?: responseFile(filepath)
    }

    //对于请求目录的，返回文件列表
    private fun responsePage(filesList: Array<File>): Response {
        val builder = StringBuilder()
        builder.append("<!DOCTYPER html><html><body>")
        builder.append("<ol>")
        for (detailsOfFiles in filesList) {
            builder.append("<a href=\"")
                    .append(detailsOfFiles.absolutePath).append("\" alt = \"\">")
                    .append(detailsOfFiles.absolutePath).append("</a><br>")
        }
        builder.append("</ol>")
        builder.append("</body></html>\n")
        //回送应答
        return newFixedLengthResponse(builder.toString())
    }

    //对于请求文件的，返回下载的文件
    private fun responseFile(uri: String): Response {
        try {
            //文件输入流
            val fis = FileInputStream(uri)
            // 返回OK，同时传送文件
            return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", fis, fis.available().toLong())
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return responseNotExist(uri)
    }

    //页面不存在，或者文件不存在时
    private fun responseNotExist(url: String): Response {
        val builder = StringBuilder()
        builder.append("<!DOCTYPE html><html><body>")
        builder.append("Sorry,Can't Found").append(url).append(" !")
        builder.append("</body></html>\n")

        return newFixedLengthResponse(builder.toString())
    }

    //非客户端禁止访问
    private fun responseNoAccess(): Response {
        val builder = StringBuilder()
        builder.append("<!DOCTYPE html><html><body>")
        builder.append("No Access!")
        builder.append("</body></html>\n")

        return newFixedLengthResponse(builder.toString())
    }

    companion object {
        private const val ROOT_DIR = "/"
    }
}