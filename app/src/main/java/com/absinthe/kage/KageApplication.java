package com.absinthe.kage;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.absinthe.kage.utils.Logger;

public class KageApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    public static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        Logger.setDebugMode(BuildConfig.DEBUG);
    }
}
