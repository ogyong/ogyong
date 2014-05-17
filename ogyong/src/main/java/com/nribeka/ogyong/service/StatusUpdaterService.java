package com.nribeka.ogyong.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.nribeka.ogyong.Constants;

/**
 */
public class StatusUpdaterService extends IntentService {

    private static final String TAG = StatusUpdaterService.class.getSimpleName();

    public StatusUpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Executing service ...");

        int updateDestination = 0;
        if (intent.hasExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION)) {
            updateDestination = intent.getIntExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION, 0);
        }

        switch (updateDestination) {
            case Constants.TWITTER_UPDATE_DESTINATION:
                sendTwitterUpdate();
                break;
            case Constants.FACEBOOK_UPDATE_DESTINATION:
                sendFacebookUpdate();
                break;
            default:
                sendTwitterUpdate();
                sendFacebookUpdate();
                break;
        }
    }

    private void sendFacebookUpdate() {
        Intent updateServiceIntent = new Intent(this, FacebookStatusUpdaterService.class);
        startService(updateServiceIntent);
    }

    private void sendTwitterUpdate() {
        Intent updateServiceIntent = new Intent(this, TwitterStatusUpdaterService.class);
        startService(updateServiceIntent);
    }
}
