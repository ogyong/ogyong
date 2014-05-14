package com.nribeka.ogyong.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphPlace;
import com.facebook.model.GraphUser;
import com.nribeka.ogyong.utils.AppConstants;
import com.nribeka.ogyong.utils.AppUtils;
import com.nribeka.ogyong.utils.GraphPlaceImpl;

import java.util.Collections;

import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

/**
 */
public class StatusUpdaterService extends IntentService {

    private static final String TAG = StatusUpdaterService.class.getSimpleName();

    protected Context context;
    protected SharedPreferences preferences;
    protected SharedPreferences.Editor editor;

    public StatusUpdaterService() {
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
    protected void onHandleIntent(Intent intent) {
        Session session = Session.getActiveSession();
        boolean inFacebook = (session != null && session.isOpened());
        boolean inTwitter = preferences.getBoolean(AppConstants.SP_TWITTER_LOGGED_IN, false);

        boolean twitterIncludeLocation = preferences.getBoolean(AppConstants.SP_TWITTER_INCLUDE_LOCATION, false);
        boolean facebookIncludeLocation = preferences.getBoolean(AppConstants.SP_FACEBOOK_INCLUDE_LOCATION, false);

        double latitude = Double.longBitsToDouble(preferences.getLong(AppConstants.SP_LAST_UPDATED_LATITUDE, Long.MIN_VALUE));
        double longitude = Double.longBitsToDouble(preferences.getLong(AppConstants.SP_LAST_UPDATED_LONGITUDE, Long.MIN_VALUE));
        String latLong = String.valueOf(latitude) + ", " + String.valueOf(longitude);
        String hashValue = AppUtils.generateHash(latLong);

        String statusMessage = AppUtils.generateStatus(getApplicationContext());
        String twitterPlace = preferences.getString("twitter:id:" + hashValue, AppConstants.EMPTY_STRING);
        String facebookPlace = preferences.getString("facebook:id:" + hashValue, AppConstants.EMPTY_STRING);

        if (inTwitter) {
            String token = preferences.getString(
                    AppConstants.SP_TWITTER_ACCESS_TOKEN, AppConstants.EMPTY_STRING);
            String tokenSecret = preferences.getString(
                    AppConstants.SP_TWITTER_ACCESS_TOKEN_SECRET, AppConstants.EMPTY_STRING);
            AccessToken accessToken = new AccessToken(token, tokenSecret);
            Twitter twitter = AppUtils.getTwitterInstance();
            twitter.setOAuthAccessToken(accessToken);

            try {
                boolean hasMessage = (statusMessage.length() > 0);
                while (hasMessage) {
                    String twitterMessage;
                    if (statusMessage.length() > 140) {
                        statusMessage = statusMessage.substring(130);
                        twitterMessage = statusMessage.substring(0, 130) + "... (cont)";
                    } else {
                        twitterMessage = statusMessage;
                        hasMessage = false;
                    }
                    StatusUpdate statusUpdate = new StatusUpdate(twitterMessage);
                    if (twitterIncludeLocation && !AppConstants.EMPTY_STRING.equals(twitterPlace)) {
                        statusUpdate.setPlaceId(twitterPlace);
                        statusUpdate.setLocation(new GeoLocation(latitude, longitude));
                    }
                    twitter.updateStatus(statusUpdate);
                }
                Intent notifierIntent = new Intent(AppConstants.INTENT_STATUS_POSTED_ACTION);
                notifierIntent.putExtra(AppConstants.INTENT_EXTRA_MESSAGE_DESTINATION, "twitter");
                context.sendBroadcast(notifierIntent);
            } catch (TwitterException e) {
                Log.e(TAG, "Unable to send status update due to: " + e.getLocalizedMessage(), e);
            }
        }

        if (inFacebook) {
            GraphPlace graphPlace = null;
            if (facebookIncludeLocation) {
                graphPlace = new GraphPlaceImpl(facebookPlace);
            }
            Request request = Request.newStatusUpdateRequest(session, statusMessage, graphPlace,
                    Collections.<GraphUser>emptyList(),
                    new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            Log.i(TAG, "Posting status succeed!");
                        }
                    }
            );
            Response response = request.executeAndWait();
            if (response != null) {
                Intent notifierIntent = new Intent(AppConstants.INTENT_STATUS_POSTED_ACTION);
                notifierIntent.putExtra(AppConstants.INTENT_EXTRA_MESSAGE_DESTINATION, "facebook");
                context.sendBroadcast(notifierIntent);
            }
        }
    }
}
