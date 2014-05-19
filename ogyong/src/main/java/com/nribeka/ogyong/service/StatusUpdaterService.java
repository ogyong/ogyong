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
    protected void onHandleIntent(final Intent intent) {
        Log.i(TAG, "Executing service ...");

        int updateDestination = 0;
        if (intent.hasExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION)) {
            updateDestination = intent.getIntExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION, 0);
        }

        String statusMessage = Constants.BLANK;
        if (intent.hasExtra(Constants.INTENT_EXTRA_STATUS_MESSAGE)) {
            statusMessage = intent.getStringExtra(Constants.INTENT_EXTRA_STATUS_MESSAGE);
        }

        switch (updateDestination) {
            case Constants.TWITTER_UPDATE_DESTINATION:
                sendTwitterUpdate(statusMessage);
                break;
            case Constants.FACEBOOK_UPDATE_DESTINATION:
                sendFacebookUpdate(statusMessage);
                break;
            default:
                sendTwitterUpdate(statusMessage);
                sendFacebookUpdate(statusMessage);
                break;
        }
    }

    private void sendFacebookUpdate(final String statusMessage) {
        Intent updateServiceIntent = new Intent(this, FacebookStatusUpdaterService.class);
        if (!Constants.BLANK.equals(statusMessage)) {
            updateServiceIntent.putExtra(Constants.INTENT_EXTRA_STATUS_MESSAGE, statusMessage);
        }
        startService(updateServiceIntent);
    }

    private void sendTwitterUpdate(final String statusMessage) {
        Intent updateServiceIntent = new Intent(this, TwitterStatusUpdaterService.class);
        if (!Constants.BLANK.equals(statusMessage)) {
            updateServiceIntent.putExtra(Constants.INTENT_EXTRA_STATUS_MESSAGE, statusMessage);
        }
        startService(updateServiceIntent);
    }
}
