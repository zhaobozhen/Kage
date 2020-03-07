package com.absinthe.kage.utils

import android.widget.Toast
import androidx.annotation.StringRes
import com.absinthe.kage.KageApplication

object ToastUtil {

    /**
     * make a toast via a string
     *
     * @param text a string text
     */
    fun makeText(text: String?) {
        Toast.makeText(KageApplication.sContext, text, Toast.LENGTH_SHORT).show()
    }

    /**
     * make a toast via a resource id
     *
     * @param resId a string resource id
     */
    @JvmStatic
    fun makeText(@StringRes resId: Int) {
        Toast.makeText(KageApplication.sContext, KageApplication.sContext.getText(resId), Toast.LENGTH_SHORT).show()
    }
}