package com.nribeka.ogyong.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.facebook.FacebookRequestError;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphPlace;
import com.facebook.model.GraphUser;
import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.model.GraphPlaceImpl;
import com.nribeka.ogyong.utils.OgyongUtils;

import java.util.Collections;

/**
 */
public class FacebookStatusUpdaterService extends IntentService {

    private static final String TAG = FacebookStatusUpdaterService.class.getSimpleName();

    protected Context context;
    protected SharedPreferences preferences;
    protected SharedPreferences.Editor editor;

    public FacebookStatusUpdaterService() {
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

        double latitude = Double.longBitsToDouble(preferences.getLong(Constants.FACEBOOK_LATITUDE, Long.MIN_VALUE));
        double longitude = Double.longBitsToDouble(preferences.getLong(Constants.FACEBOOK_LONGITUDE, Long.MIN_VALUE));
        String latLong = String.valueOf(latitude) + ", " + String.valueOf(longitude);
        String hashValue = OgyongUtils.generateHash(latLong);

        String statusMessage = null;
        if (intent.hasExtra(Constants.INTENT_EXTRA_STATUS_MESSAGE)) {
            statusMessage = intent.getStringExtra(Constants.INTENT_EXTRA_STATUS_MESSAGE);
        }

        if (OgyongUtils.isBlank(statusMessage)) {
            statusMessage = OgyongUtils.generateStatus(getApplicationContext());
        }

        Session session = Session.getActiveSession();
        boolean hasMessage = statusMessage != null && statusMessage.length() > 0;
        if (session != null && session.isOpened() && hasMessage) {
            GraphPlace graphPlace = null;
            String key = "facebook:id:" + hashValue;

            String place = preferences.getString(key, Constants.BLANK);
            boolean includeLocation = preferences.getBoolean(Constants.FACEBOOK_INCLUDE_LOCATION, false);
            if (includeLocation && !OgyongUtils.isBlank(place)) {
                graphPlace = new GraphPlaceImpl(place);
            }

            Request request = Request.newStatusUpdateRequest(
                    session, statusMessage, graphPlace, Collections.<GraphUser>emptyList(),
                    new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            if (response != null && response.getError() == null) {
                                Log.i(TAG, "Facebook status update posted!");
                            }
                        }
                    }
            );
            Response response = request.executeAndWait();
            // if there's a response and the error in the response is null
            if (response != null) {
                FacebookRequestError facebookRequestError = response.getError();
                if (facebookRequestError == null) {
                    Intent notifierIntent = new Intent(Constants.INTENT_STATUS_UPDATED);
                    notifierIntent.putExtra(Constants.INTENT_EXTRA_MESSAGE_DESTINATION, "facebook");
                    notifierIntent.putExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION, Constants.FACEBOOK_UPDATE_DESTINATION);
                    context.sendBroadcast(notifierIntent);
                } else {
                    Intent notifierIntent = new Intent(Constants.INTENT_STATUS_UPDATE_FAILED);
                    notifierIntent.putExtra(Constants.INTENT_EXTRA_MESSAGE_DESTINATION, "facebook");
                    context.sendBroadcast(notifierIntent);
                }
            }
        }
    }
}
