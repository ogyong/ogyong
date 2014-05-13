package com.nribeka.ogyong.service;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.nribeka.ogyong.receiver.ConnectivityChangedReceiver;
import com.nribeka.ogyong.receiver.LocationChangedActiveReceiver;
import com.nribeka.ogyong.receiver.LocationChangedPassiveReceiver;
import com.nribeka.ogyong.utils.AppConstants;
import com.nribeka.ogyong.utils.AppUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.GeoLocation;
import twitter4j.GeoQuery;
import twitter4j.Place;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;

/**
 * Service that requests a list of nearby locations from the underlying web service.
 */
public class PlaceUpdaterService extends IntentService {

    protected static String TAG = PlaceUpdaterService.class.getSimpleName();

    protected SharedPreferences preferences;
    protected Editor editor;
    protected ConnectivityManager connectivityManager;
    protected boolean lowBattery = false;
    protected boolean mobileData = false;

    public PlaceUpdaterService() {
        super(TAG);
        setIntentRedeliveryMode(false);
    }

    /**
     * Set the Intent Redelivery mode to true to ensure the Service starts "Sticky"
     * Defaults to "true" on legacy devices.
     */
    protected void setIntentRedeliveryMode(boolean enable) {
    }

    /**
     * Returns battery status. True if less than 10% remaining.
     *
     * @param battery Battery Intent
     * @return Battery is low
     */
    protected boolean isLowBattery(Intent battery) {
        float percentLevel = (float) battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 1) /
                battery.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
        return percentLevel < 0.15;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        editor = preferences.edit();
    }

    /**
     * {@inheritDoc}
     * Checks the battery and connectivity state before removing stale venues
     * and initiating a server poll for new venues around the specified
     * location within the given radius.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        // Check if we're running in the foreground, if not, check if we have permission to do background updates.
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean backgroundAllowed = connectivityManager.getBackgroundDataSetting();
        boolean networkConnected = (networkInfo != null && networkInfo.isConnected());
        boolean inBackground = preferences.getBoolean(AppConstants.SP_APP_IN_BACKGROUND, false);

        if (inBackground && (!backgroundAllowed || !networkConnected)) return;

        // Extract the location and radius around which to conduct our search.
        Location location = new Location(AppConstants.LOCATION_PROVIDER_DUMMY);

        Bundle extras = intent.getExtras();
        if (intent.hasExtra(AppConstants.INTENT_EXTRA_LOCATION)) {
            location = (Location) (extras.get(AppConstants.INTENT_EXTRA_LOCATION));
        }

        // Check if we're in a low battery situation.
        IntentFilter batIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent battery = registerReceiver(null, batIntentFilter);
        lowBattery = isLowBattery(battery);

        // Check if we're connected to a data network, and if so - if it's a mobile network.
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        mobileData = activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;

        // If we're not connected, enable the connectivity receiver and disable the location receiver.
        // There's no point trying to poll the server for updates if we're not connected, and the
        // connectivity receiver will turn the location-based updates back on once we have a connection.
        if (!isConnected) {
            PackageManager pm = getPackageManager();

            ComponentName connectivityReceiver = new ComponentName(this, ConnectivityChangedReceiver.class);
            ComponentName locationReceiver = new ComponentName(this, LocationChangedActiveReceiver.class);
            ComponentName passiveLocationReceiver = new ComponentName(this, LocationChangedPassiveReceiver.class);

            pm.setComponentEnabledSetting(connectivityReceiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            pm.setComponentEnabledSetting(locationReceiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

            pm.setComponentEnabledSetting(passiveLocationReceiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        } else {
            // If we are connected check to see if this is a forced update (typically triggered
            // when the location has changed).
            boolean doUpdate = intent.getBooleanExtra(AppConstants.INTENT_EXTRA_FORCE_REFRESH, false);

            // If it's not a forced update (for example from the Activity being restarted) then
            // check to see if we've moved far enough, or there's been a long enough delay since
            // the last update and if so, enforce a new update.
            if (!doUpdate) {
                // Retrieve the last update time and place.
                long lastTime = preferences.getLong(AppConstants.SP_LAST_UPDATED_TIME, Long.MIN_VALUE);
                double lastLatitude = Double.longBitsToDouble(
                        preferences.getLong(AppConstants.SP_LAST_UPDATED_LATITUDE, Long.MIN_VALUE));
                double lastLongitude = Double.longBitsToDouble(
                        preferences.getLong(AppConstants.SP_LAST_UPDATED_LONGITUDE, Long.MIN_VALUE));
                Location lastLocation = new Location(AppConstants.LOCATION_PROVIDER_DUMMY);
                lastLocation.setLatitude(lastLatitude);
                lastLocation.setLongitude(lastLongitude);

                // If update time and distance bounds have been passed, do an update.
                if ((lastTime < System.currentTimeMillis() - AppConstants.LOCATION_MAX_TIME) ||
                        (lastLocation.distanceTo(location) > AppConstants.LOCATION_MAX_DISTANCE))
                    doUpdate = true;
            }

            if (doUpdate) {
                refreshPlaces(location);
            }
        }
        Log.d(TAG, "Place list download service complete.");
        Intent updatePlaceIntent = new Intent();
        updatePlaceIntent.setAction(AppConstants.INTENT_LOCATION_UPDATED);
        sendBroadcast(updatePlaceIntent);
    }

    /**
     * Polls the underlying service to return a list of places within the specified
     * radius of the specified Location.
     *
     * @param location Location
     */
    protected void refreshPlaces(Location location) {
        Session session = Session.getActiveSession();
        boolean inFacebook = (session != null && session.isOpened());
        boolean inTwitter = preferences.getBoolean(AppConstants.SP_TWITTER_LOGGED_IN, false);

        Log.i(TAG, "Lat: " + location.getLatitude() + ", Long: " + location.getLongitude());

        boolean facebookIncludeLocation = preferences.getBoolean(AppConstants.SP_FACEBOOK_INCLUDE_LOCATION, false);
        boolean twitterIncludeLocation = preferences.getBoolean(AppConstants.SP_TWITTER_INCLUDE_LOCATION, false);

        // need to check whether we already have the location in the cache (shared prefs)
        // we will store key pair of the location --> places
        // id:hash(location) --> place id
        // name:hash(location) --> place name

        String latLong = String.valueOf(location.getLatitude()) + ", " + String.valueOf(location.getLongitude());
        String hashValue = AppUtils.generateHash(latLong);

        if (inTwitter && twitterIncludeLocation) {
            String placeId = preferences.getString("twitter:id:" + hashValue, AppConstants.EMPTY_STRING);
            String placeName = preferences.getString("twitter:name:" + hashValue, AppConstants.EMPTY_STRING);
            if (AppConstants.EMPTY_STRING.equals(placeId)) {
                String token = preferences.getString(
                        AppConstants.SP_TWITTER_ACCESS_TOKEN, AppConstants.EMPTY_STRING);
                String tokenSecret = preferences.getString(
                        AppConstants.SP_TWITTER_ACCESS_TOKEN_SECRET, AppConstants.EMPTY_STRING);
                AccessToken accessToken = new AccessToken(token, tokenSecret);
                Twitter twitter = AppUtils.getTwitterInstance();
                twitter.setOAuthAccessToken(accessToken);
                try {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    GeoLocation geoLocation = new GeoLocation(latitude, longitude);
                    ResponseList<Place> places = twitter.searchPlaces(new GeoQuery(geoLocation));
                    if (!places.isEmpty()) {
                        // we need to remove the oldest location
                        int locationCount = preferences.getInt(AppConstants.SP_TWITTER_LOCATION_COUNT, 0);
                        // when it's empty, this will be initialized with hash value of the current location
                        String locationHashes = preferences.getString(AppConstants.SP_TWITTER_LOCATION_HASHES, hashValue);
                        if (locationCount > 10) {
                            int indexOfSeparator = locationHashes.indexOf("|");
                            String theFirstHash = locationHashes.substring(0, indexOfSeparator);
                            locationHashes = locationHashes.substring(indexOfSeparator + 1);
                            // remove it from the preferences
                            editor.remove("twitter:id:" + theFirstHash);
                            editor.remove("twitter:name:" + theFirstHash);
                        }
                        // assuming the first one is the closest match, let's just persist just one
                        Place place = places.get(0);
                        editor.putString("twitter:id:" + hashValue, place.getId());
                        editor.putString("twitter:name:" + hashValue, place.getFullName());
                        editor.putInt(AppConstants.SP_TWITTER_LOCATION_COUNT, locationCount + 1);
                        if (locationCount > 0) {
                            locationHashes = locationHashes + "|" + hashValue;
                        }
                        editor.putString(AppConstants.SP_TWITTER_LOCATION_HASHES, locationHashes);
                    }
                } catch (TwitterException e) {
                    Log.e(TAG, "Unable to fetch places based on the current location.", e);
                }
            } else {
                Log.i(TAG, "Twitter place already in cache: " + placeId + " -> " + placeName);
            }
        }

        if (inFacebook && facebookIncludeLocation) {
            String facebookPlace = preferences.getString("facebook:name:" + hashValue, AppConstants.EMPTY_STRING);
            String facebookPlaceId = preferences.getString("facebook:id:" + hashValue, AppConstants.EMPTY_STRING);
            if (AppConstants.EMPTY_STRING.equals(facebookPlaceId)) {
                Request request = Request.newPlacesSearchRequest(session, location, 50, 10, AppConstants.EMPTY_STRING, null);
                Response response = request.executeAndWait();
                GraphObject graphObject = response.getGraphObject();
                if (graphObject != null && graphObject.getInnerJSONObject() != null) {
                    try {
                        JSONArray jsonArray = graphObject.getInnerJSONObject().getJSONArray("data");
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        if (jsonObject != null) {
                            // we need to remove the oldest location
                            int locationCount = preferences.getInt(AppConstants.SP_FACEBOOK_LOCATION_COUNT, 0);
                            // when it's empty, this will be initialized with hash value of the current location
                            String locationHashes = preferences.getString(AppConstants.SP_FACEBOOK_LOCATION_HASHES, hashValue);
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
                            editor.putInt(AppConstants.SP_FACEBOOK_LOCATION_COUNT, locationCount + 1);
                            if (locationCount > 0) {
                                locationHashes = locationHashes + "|" + hashValue;
                            }
                            editor.putString(AppConstants.SP_FACEBOOK_LOCATION_HASHES, locationHashes);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Unable to fetch data from json based on the current location.", e);
                    }
                }
            } else {
                Log.i(TAG, "Facebook place already in cache: " + facebookPlaceId + " -> " + facebookPlace);
            }
        }
        // Save the last update time and place to the Shared Preferences.
        editor.putLong(AppConstants.SP_LAST_UPDATED_LATITUDE, Double.doubleToLongBits(location.getLatitude()));
        editor.putLong(AppConstants.SP_LAST_UPDATED_LONGITUDE, Double.doubleToLongBits(location.getLongitude()));
        editor.putLong(AppConstants.SP_LAST_UPDATED_TIME, System.currentTimeMillis());
        editor.commit();
    }
}