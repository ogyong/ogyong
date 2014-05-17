package com.nribeka.ogyong.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphPlace;
import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.utils.OgyongUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Random;

/**
 */
public class FacebookPlaceUpdaterService extends IntentService {

    private static final String TAG = FacebookPlaceUpdaterService.class.getSimpleName();

    protected Context context;
    protected SharedPreferences preferences;
    protected SharedPreferences.Editor editor;
    protected int searchRadius = 50;
    protected int searchSelection = 10;

    public FacebookPlaceUpdaterService() {
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
        Session session = Session.getActiveSession();
        boolean inFacebook = (session != null && session.isOpened());
        boolean includeLocation = preferences.getBoolean(Constants.FACEBOOK_INCLUDE_LOCATION, false);

        boolean randomize = preferences.getBoolean(Constants.FACEBOOK_RANDOMIZE_LOCATION, false);
        if (randomize) {
            searchRadius = 200;
            searchSelection = 100;
        }

        Location location;
        if (intent.hasExtra(Constants.INTENT_EXTRA_LOCATION)) {
            location = (Location) (intent.getExtras().get(Constants.INTENT_EXTRA_LOCATION));

            // we will store key pair of the location --> places
            // id:hash(location) --> place id
            // name:hash(location) --> place name
            String latLong = String.valueOf(location.getLatitude()) + ", " + String.valueOf(location.getLongitude());
            String hashValue = OgyongUtils.generateHash(latLong);

            if (inFacebook && includeLocation) {
                String facebookPlace = preferences.getString("facebook:name:" + hashValue, Constants.EMPTY_STRING);
                String facebookPlaceId = preferences.getString("facebook:id:" + hashValue, Constants.EMPTY_STRING);
                if (Constants.EMPTY_STRING.equals(facebookPlaceId)) {
                    int count = 0;
                    boolean locationFound = false;
                    while (!locationFound && count < 5) {
                        try {
                            Request request = Request.newPlacesSearchRequest(
                                    session, location, searchRadius + (searchRadius * count), searchSelection,
                                    Constants.EMPTY_STRING, new Request.GraphPlaceListCallback() {
                                        @Override
                                        public void onCompleted(List<GraphPlace> places, Response response) {
                                            Log.i(TAG, "Facebook request executed!");
                                        }
                                    }
                            );
                            Response response = request.executeAndWait();
                            GraphObject graphObject = response.getGraphObject();
                            JSONArray jsonArray = graphObject.getInnerJSONObject().getJSONArray("data");
                            if (jsonArray.length() > 0) {
                                JSONObject jsonObject = jsonArray.getJSONObject(getSelection(jsonArray.length(), randomize));
                                saveFacebookLocation(hashValue, jsonObject);
                                locationFound = true;
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Unable to fetch data from json based on the current location.", e);
                        }
                        count++;
                    }
                } else {
                    Log.i(TAG, "Facebook place found in cache: " + facebookPlaceId + " -> " + facebookPlace);
                }
            }

            Intent updatePlaceIntent = new Intent();
            updatePlaceIntent.setAction(Constants.INTENT_LOCATION_UPDATED);
            sendBroadcast(updatePlaceIntent);
        }
    }

    private int getSelection(int selectionSize, boolean randomize) {
        int selection = 0;
        if (randomize) {
            Random random = new Random();
            selection = random.nextInt(selectionSize);
        }
        return selection;
    }

    private void saveFacebookLocation(String hashValue, JSONObject jsonObject) throws JSONException {
        // we need to remove the oldest location
        int locationCount = preferences.getInt(Constants.FACEBOOK_LOCATION_COUNT, 0);
        // when it's empty, this will be initialized with hash value of the current location
        String locationHashes = preferences.getString(Constants.FACEBOOK_LOCATION_HASHES, hashValue);
        if (locationCount > 10) {
            int indexOfSeparator = locationHashes.indexOf("|");
            String theFirstHash = locationHashes.substring(0, indexOfSeparator);
            locationHashes = locationHashes.substring(indexOfSeparator + 1);
            // remove it from the preferences
            editor.remove("facebook:id:" + theFirstHash);
            editor.remove("facebook:name:" + theFirstHash);
        }
        editor.putString("facebook:id:" + hashValue, jsonObject.getString("id"));
        editor.putString("facebook:name:" + hashValue, jsonObject.getString("name"));
        editor.putInt(Constants.FACEBOOK_LOCATION_COUNT, locationCount + 1);
        if (locationCount > 0) {
            locationHashes = locationHashes + "|" + hashValue;
        }
        editor.putString(Constants.FACEBOOK_LOCATION_HASHES, locationHashes);
    }
}
