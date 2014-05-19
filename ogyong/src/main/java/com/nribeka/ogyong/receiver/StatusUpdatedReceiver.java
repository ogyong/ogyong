package com.nribeka.ogyong.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.service.StatusUpdateNotifierService;

/**
 * Manifest Receiver that listens for broadcasts announcing a successful status posting.
 * This class starts the StatusPostedNotificationService that will trigger a notification
 * announcing the successful status posting. We don't want notifications for this app to
 * be announced while the app is running, so this receiver is disabled whenever the
 * main Activity is visible.
 */
public class StatusUpdatedReceiver extends BroadcastReceiver {

    protected static String TAG = StatusUpdatedReceiver.class.getSimpleName();

    /**
     * When a successful status posted is announced, extract the unique ID of the place
     * that's been checked in to, and pass this value to the StatusPostedNotificationService
     * when you start it.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Executing receiver ...");
        String destination = intent.getStringExtra(Constants.INTENT_EXTRA_MESSAGE_DESTINATION);
        int destinationCode = intent.getIntExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION, 0);
        if (destination != null) {
            Intent serviceIntent = new Intent(context, StatusUpdateNotifierService.class);
            serviceIntent.putExtra(Constants.INTENT_EXTRA_MESSAGE_DESTINATION, destination);
            serviceIntent.putExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION, destinationCode);
            context.startService(serviceIntent);
        }
    }
}