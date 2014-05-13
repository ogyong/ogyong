package com.nribeka.ogyong.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.nribeka.ogyong.R;
import com.nribeka.ogyong.activity.PostActivity;
import com.nribeka.ogyong.utils.AppConstants;

/**
 * Service that handles background notifications.
 * This Service will be started by the {@link com.nribeka.ogyong.receiver.StatusPostedReceiver}
 * when the Application isn't visible and trigger a Notification
 * telling the user that they have posted a new status in facebook or twitter.
 * <p/>
 */
public class StatusUpdateNotifierService extends IntentService {

    protected static String TAG = StatusUpdateNotifierService.class.getSimpleName();
    protected NotificationManager notificationManager;

    public StatusUpdateNotifierService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * {@inheritDoc}
     * Extract the name of the venue based on the ID specified in the broadcast Checkin Intent
     * and use it to display a Notification.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        String destination = intent.getStringExtra(AppConstants.INTENT_EXTRA_MESSAGE_DESTINATION);

        Intent resultIntent = new Intent(this, PostActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(PostActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ogyong);
        builder.setContentTitle("Ogyong");
        builder.setContentText("Music player status posted to " + destination + "!");
        builder.setContentIntent(resultPendingIntent);

        int notifyId = AppConstants.STATUS_NOTIFICATION;
        notificationManager.notify(notifyId, builder.build());
    }
}