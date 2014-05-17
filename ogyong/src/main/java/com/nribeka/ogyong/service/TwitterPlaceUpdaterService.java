package com.nribeka.ogyong.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.utils.OgyongUtils;

import java.util.Random;

import twitter4j.GeoLocation;
import twitter4j.GeoQuery;
import twitter4j.Place;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

/**
 */
public class TwitterPlaceUpdaterService extends IntentService {

    private static final String TAG = FacebookPlaceUpdaterService.class.getSimpleName();

    protected Context context;
    protected SharedPreferences preferences;
    protected SharedPreferences.Editor editor;

    public TwitterPlaceUpdaterService() {
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
        boolean inTwitter = preferences.getBoolean(Constants.TWITTER_LOGGED_IN, false);
        boolean includeLocation = preferences.getBoolean(Constants.TWITTER_INCLUDE_LOCATION, false);
        boolean randomize = preferences.getBoolean(Constants.FACEBOOK_RANDOMIZE_LOCATION, false);

        Location location;
        if (intent.hasExtra(Constants.INTENT_EXTRA_LOCATION)) {
            location = (Location) (intent.getExtras().get(Constants.INTENT_EXTRA_LOCATION));

            String latLong = String.valueOf(location.getLatitude()) + ", " + String.valueOf(location.getLongitude());
            String hashValue = OgyongUtils.generateHash(latLong);

            if (inTwitter && includeLocation) {
                String placeId = preferences.getString("twitter:id:" + hashValue, Constants.EMPTY_STRING);
                String placeName = preferences.getString("twitter:name:" + hashValue, Constants.EMPTY_STRING);
                if (Constants.EMPTY_STRING.equals(placeId)) {
                    String token = preferences.getString(Constants.TWITTER_ACCESS_TOKEN, Constants.EMPTY_STRING);
                    String tokenSecret = preferences.getString(Constants.TWITTER_ACCESS_TOKEN_SECRET, Constants.EMPTY_STRING);

                    AccessToken accessToken = new AccessToken(token, tokenSecret);
                    Twitter twitter = OgyongUtils.getTwitterInstance();
                    twitter.setOAuthAccessToken(accessToken);
                    try {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        GeoQuery geoQuery = new GeoQuery(new GeoLocation(latitude, longitude));
                        ResponseList<Place> places = twitter.searchPlaces(geoQuery);
                        if (!places.isEmpty()) {
                            Place place = places.get(getSelection(places.size(), randomize));
                            saveTwitterLocation(hashValue, place);
                        }
                    } catch (TwitterException e) {
                        Log.e(TAG, "Unable to fetch places based on the current location.", e);
                    }
                } else {
                    Log.i(TAG, "Twitter place found in cache: " + placeId + " -> " + placeName);
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

    private void saveTwitterLocation(String hashValue, Place place) {
        // we need to remove the oldest location
        int locationCount = preferences.getInt(Constants.TWITTER_LOCATION_COUNT, 0);
        // when it's empty, this will be initialized with hash value of the current location
        String locationHashes = preferences.getString(Constants.TWITTER_LOCATION_HASHES, hashValue);
        if (locationCount > 10) {
            int indexOfSeparator = locationHashes.indexOf("|");
            String theFirstHash = locationHashes.substring(0, indexOfSeparator);
            locationHashes = locationHashes.substring(indexOfSeparator + 1);
            // remove it from the preferences
            editor.remove("twitter:id:" + theFirstHash);
            editor.remove("twitter:name:" + theFirstHash);
        }
        // assuming the first one is the closest match, let's just persist just one
        editor.putString("twitter:id:" + hashValue, place.getId());
        editor.putString("twitter:name:" + hashValue, place.getFullName());
        editor.putInt(Constants.TWITTER_LOCATION_COUNT, locationCount + 1);
        if (locationCount > 0) {
            locationHashes = locationHashes + "|" + hashValue;
        }
        editor.putString(Constants.TWITTER_LOCATION_HASHES, locationHashes);
    }
}
