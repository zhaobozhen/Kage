package com.absinthe.kage.utils

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object StorageUtils {

    @JvmStatic
    fun saveBitmap(bmp: Bitmap, file: File) {
        try {
            val out = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}