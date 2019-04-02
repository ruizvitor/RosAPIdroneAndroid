package com.dji.RosAPI;

import android.app.Application;
import android.content.Context;

import com.secneo.sdk.Helper;


public class MApplication extends Application {

    private RosAPIApplication rosAPIApplication;

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        Helper.install(MApplication.this);
        if (rosAPIApplication == null) {
            rosAPIApplication = new RosAPIApplication();
            rosAPIApplication.setContext(this);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        rosAPIApplication.onCreate();
    }

}