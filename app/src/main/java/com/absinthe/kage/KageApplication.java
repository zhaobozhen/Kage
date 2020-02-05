package com.absinthe.kage;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class KageApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    public static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
    }
}
