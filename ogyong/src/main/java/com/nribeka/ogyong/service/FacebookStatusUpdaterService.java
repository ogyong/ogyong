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
        double latitude = Double.longBitsToDouble(preferences.getLong(Constants.LAST_UPDATED_LATITUDE, Long.MIN_VALUE));
        double longitude = Double.longBitsToDouble(preferences.getLong(Constants.LAST_UPDATED_LONGITUDE, Long.MIN_VALUE));
        String latLong = String.valueOf(latitude) + ", " + String.valueOf(longitude);
        String hashValue = OgyongUtils.generateHash(latLong);

        String statusMessage = OgyongUtils.generateStatus(getApplicationContext());
        Session session = Session.getActiveSession();
        if (session != null && session.isOpened()) {
            GraphPlace graphPlace = null;
            String key = "facebook:id:" + hashValue;

            String place = preferences.getString(key, Constants.EMPTY_STRING);
            boolean includeLocation = preferences.getBoolean(Constants.FACEBOOK_INCLUDE_LOCATION, false);
            if (includeLocation && !Constants.EMPTY_STRING.equals(place)) {
                graphPlace = new GraphPlaceImpl(place);
            }

            Request request = Request.newStatusUpdateRequest(
                    session, statusMessage, graphPlace,
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
                Intent notifierIntent = new Intent(Constants.INTENT_STATUS_POSTED_ACTION);
                notifierIntent.putExtra(Constants.INTENT_EXTRA_MESSAGE_DESTINATION, "facebook");
                context.sendBroadcast(notifierIntent);
            }
        }
    }
}
