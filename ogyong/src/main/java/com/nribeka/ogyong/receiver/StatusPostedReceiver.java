package com.nribeka.ogyong.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.nribeka.ogyong.service.StatusUpdateNotifierService;
import com.nribeka.ogyong.utils.AppConstants;

/**
 * Manifest Receiver that listens for broadcasts announcing a successful status posting.
 * This class starts the StatusPostedNotificationService that will trigger a notification
 * announcing the successful status posting. We don't want notifications for this app to
 * be announced while the app is running, so this receiver is disabled whenever the
 * main Activity is visible.
 */
public class StatusPostedReceiver extends BroadcastReceiver {

    protected static String TAG = StatusPostedReceiver.class.getSimpleName();

    /**
     * When a successful status posted is announced, extract the unique ID of the place
     * that's been checked in to, and pass this value to the StatusPostedNotificationService
     * when you start it.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String destination = intent.getStringExtra(AppConstants.INTENT_EXTRA_MESSAGE_DESTINATION);
        if (destination != null) {
            Intent serviceIntent = new Intent(context, StatusUpdateNotifierService.class);
            serviceIntent.putExtra(AppConstants.INTENT_EXTRA_MESSAGE_DESTINATION, destination);
            context.startService(serviceIntent);
        }
    }
}