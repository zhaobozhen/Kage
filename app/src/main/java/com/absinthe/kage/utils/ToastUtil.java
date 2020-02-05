package com.absinthe.kage.utils;

import android.widget.Toast;

import androidx.annotation.StringRes;

import com.absinthe.kage.KageApplication;

public class ToastUtil {
    /**
     * make a toast via a string
     *
     * @param text a string text
     */
    public static void makeText(String text) {
        Toast.makeText(KageApplication.sContext, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * make a toast via a resource id
     *
     * @param resId a string resource id
     */
    public static void makeText(@StringRes int resId) {
        Toast.makeText(KageApplication.sContext, KageApplication.sContext.getText(resId), Toast.LENGTH_SHORT).show();
    }
}

