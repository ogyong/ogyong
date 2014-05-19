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
import com.facebook.model.GraphPlace;
import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.utils.OgyongUtils;

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
    protected List<GraphPlace> graphPlaces;

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
        Log.i(TAG, "Executing service ...");

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

            Log.i(TAG, "Location received: " + location.getProvider() + " -> " + latLong);

            if (inFacebook && includeLocation) {
                String placeId = preferences.getString("facebook:id:" + hashValue, Constants.BLANK);
                String placeName = preferences.getString("facebook:name:" + hashValue, Constants.BLANK);
                if (OgyongUtils.isBlank(placeId)) {
                    int count = 0;
                    boolean locationFound = false;
                    while (!locationFound && count < 5) {
                        Request request = Request.newPlacesSearchRequest(
                                session, location, searchRadius + (searchRadius * count), searchSelection,
                                Constants.BLANK, new Request.GraphPlaceListCallback() {
                                    @Override
                                    public void onCompleted(List<GraphPlace> places, Response response) {
                                        graphPlaces = places;
                                    }
                                }
                        );
                        Response response = request.executeAndWait();
                        if (response != null && graphPlaces != null && !graphPlaces.isEmpty()) {
                            GraphPlace graphPlace = graphPlaces.get(getSelection(graphPlaces.size(), randomize));
                            saveFacebookPlace(hashValue, graphPlace);
                            locationFound = true;
                        }
                        count++;
                    }
                } else {
                    Log.i(TAG, "Facebook place found in cache: " + placeId + " -> " + placeName);
                }

                // Save the last update time and place to the Shared Preferences.
                editor.putLong(Constants.FACEBOOK_LATITUDE, Double.doubleToLongBits(location.getLatitude()));
                editor.putLong(Constants.FACEBOOK_LONGITUDE, Double.doubleToLongBits(location.getLongitude()));
                editor.commit();
            }

            Intent updatePlaceIntent = new Intent();
            updatePlaceIntent.setAction(Constants.INTENT_FACEBOOK_LOCATION_UPDATED);
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

    private void saveFacebookPlace(final String hashValue, final GraphPlace graphPlace) {
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
        editor.putString("facebook:id:" + hashValue, graphPlace.getId());
        editor.putString("facebook:name:" + hashValue, graphPlace.getName());
        editor.putInt(Constants.FACEBOOK_LOCATION_COUNT, locationCount + 1);
        if (locationCount > 0) {
            locationHashes = locationHashes + "|" + hashValue;
        }
        editor.putString(Constants.FACEBOOK_LOCATION_HASHES, locationHashes);
        editor.commit();
    }
}
