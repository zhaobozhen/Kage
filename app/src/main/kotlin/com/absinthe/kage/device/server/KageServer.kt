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

        if (userAgent.contains("Mozilla/5.0")) {
            return responseNoAccess()
        }

        if (filepath.isEmpty() || filepath == ROOT_DIR) {
            filepath = rootDir.absolutePath
        }
        filesList = File(filepath).listFiles()
        return filesList?.let { responsePage(it) } ?: responseFile(filepath)
    }

    /**
     * Return files list if request a directory
     *
     * @param filesList Files list
     */
    private fun responsePage(filesList: Array<File>): Response {
        val builder = StringBuilder().apply {
            append("<!DOCTYPER html><html><body>")
            append("<ol>")

            for (detailsOfFiles in filesList) {
                append("<a href=\"")
                append(detailsOfFiles.absolutePath).append("\" alt = \"\">")
                append(detailsOfFiles.absolutePath).append("</a><br>")
            }

            append("</ol>")
            append("</body></html>\n")
        }

        //Response
        return newFixedLengthResponse(builder.toString())
    }

    /**
     * Return itself if request a file
     *
     * @param uri URI of the file
     */
    private fun responseFile(uri: String): Response {
        try {
            val fis = FileInputStream(uri)
            // Response OK and transfer the file
            return newFixedLengthResponse(Response.Status.OK,
                    "application/octet-stream", fis, fis.available().toLong())
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return responseNotExist(uri)
    }

    /**
     * Return Error info if request an invalid path
     *
     * @param url Invalid URL
     */
    private fun responseNotExist(url: String): Response {
        val builder = StringBuilder().apply {
            append("<!DOCTYPE html><html><body>")
            append("Sorry,Can't Found").append(url).append(" !")
            append("</body></html>\n")
        }

        return newFixedLengthResponse(builder.toString())
    }

    /**
     * Refuse to access the server if it is not a client
     */
    private fun responseNoAccess(): Response {
        val builder = StringBuilder().apply {
            append("<!DOCTYPE html><html><body>")
            append("No Access!")
            append("</body></html>\n")
        }

        return newFixedLengthResponse(builder.toString())
    }

    companion object {
        private const val ROOT_DIR = "/"
    }
}