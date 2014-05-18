package com.nribeka.ogyong.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.utils.OgyongUtils;

import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

/**
 */
public class TwitterStatusUpdaterService extends IntentService {

    private static final String TAG = TwitterStatusUpdaterService.class.getSimpleName();

    private static final int TWITTER_MAX_LENGTH = 140;
    private static final int SUBSTRING_LENGTH = 130;
    protected Context context;
    protected SharedPreferences preferences;
    protected SharedPreferences.Editor editor;

    public TwitterStatusUpdaterService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        Log.i(TAG, "Executing service ...");

        double latitude = Double.longBitsToDouble(preferences.getLong(Constants.TWITTER_LATITUDE, Long.MIN_VALUE));
        double longitude = Double.longBitsToDouble(preferences.getLong(Constants.TWITTER_LONGITUDE, Long.MIN_VALUE));
        String latLong = String.valueOf(latitude) + ", " + String.valueOf(longitude);
        String hashValue = OgyongUtils.generateHash(latLong);

        String statusMessage = OgyongUtils.generateStatus(getApplicationContext());

        boolean inTwitter = preferences.getBoolean(Constants.TWITTER_LOGGED_IN, false);
        if (inTwitter) {
            String token = preferences.getString(Constants.TWITTER_ACCESS_TOKEN, Constants.EMPTY_STRING);
            String tokenSecret = preferences.getString(Constants.TWITTER_ACCESS_TOKEN_SECRET, Constants.EMPTY_STRING);
            AccessToken accessToken = new AccessToken(token, tokenSecret);
            Twitter twitter = OgyongUtils.getTwitterInstance();
            twitter.setOAuthAccessToken(accessToken);

            try {
                String key = "twitter:id:" + hashValue;
                String place = preferences.getString(key, Constants.EMPTY_STRING);
                boolean includeLocation = preferences.getBoolean(Constants.TWITTER_INCLUDE_LOCATION, false);

                int count = 0;
                boolean hasMessage = statusMessage.length() > 0;
                while (hasMessage) {
                    int start = SUBSTRING_LENGTH * count;
                    int end = SUBSTRING_LENGTH * ++count;
                    hasMessage = statusMessage.length() > TWITTER_MAX_LENGTH || statusMessage.length() > end;
                    String message = hasMessage ? statusMessage.substring(start, end) + "... (cont)" : statusMessage;
                    StatusUpdate statusUpdate = new StatusUpdate(message);
                    if (includeLocation) {
                        statusUpdate.setLocation(new GeoLocation(latitude, longitude));
                    }
                    twitter.updateStatus(statusUpdate);
                }
                Intent notifierIntent = new Intent(Constants.INTENT_STATUS_UPDATED);
                notifierIntent.putExtra(Constants.INTENT_EXTRA_MESSAGE_DESTINATION, "twitter");
                notifierIntent.putExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION, Constants.TWITTER_UPDATE_DESTINATION);
                context.sendBroadcast(notifierIntent);
            } catch (TwitterException e) {
                Log.e(TAG, "Unable to send status update due to: " + e.getLocalizedMessage(), e);
            }
        }
    }
}
