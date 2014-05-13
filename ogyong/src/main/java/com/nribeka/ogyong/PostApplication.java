package com.nribeka.ogyong;

import android.app.Application;
import android.os.StrictMode;

import com.nribeka.ogyong.utils.AppConstants;

/**
 */
public class PostApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (AppConstants.DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
        }
    }
}
