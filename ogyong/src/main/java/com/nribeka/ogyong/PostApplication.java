package com.nribeka.ogyong;

import android.app.Application;
import android.os.StrictMode;

/**
 */
public class PostApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (Constants.DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build());
        }
    }
}
