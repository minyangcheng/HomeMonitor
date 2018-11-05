package com.min.home.monitor;

import android.app.Application;

import com.blankj.utilcode.util.Utils;

/**
 * Created by minych on 18-11-5.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
    }
}
