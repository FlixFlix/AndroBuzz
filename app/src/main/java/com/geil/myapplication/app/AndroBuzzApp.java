package com.geil.myapplication.app;

import android.app.Application;

import com.androbuzz.android.BuildConfig;

import timber.log.Timber;

public class AndroBuzzApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPrefs.getInstance().init(this);

        if(BuildConfig.DEBUG){
            Timber.plant(new Timber.DebugTree());
        }
    }
}
