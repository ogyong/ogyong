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
import android.preference.PreferenceManager;
import android.util.Log;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.receiver.ConnectivityChangedReceiver;
import com.nribeka.ogyong.receiver.LocationChangedActiveReceiver;
import com.nribeka.ogyong.receiver.LocationChangedPassiveReceiver;

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

    protected int destination;
    protected Location location;

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
        Log.i(TAG, "Executing service ...");
        // Check if we're running in the foreground, if not, check if we have permission to do background updates.
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean backgroundAllowed = connectivityManager.getBackgroundDataSetting();
        boolean networkConnected = (networkInfo != null && networkInfo.isConnected());
        boolean inBackground = preferences.getBoolean(Constants.APP_IN_BACKGROUND, false);
        if (inBackground && (!backgroundAllowed || !networkConnected)) return;

        // Extract the location and radius around which to conduct our search.
        location = new Location(Constants.CONSTRUCTED_LOCATION_PROVIDER);
        if (intent.hasExtra(Constants.INTENT_EXTRA_LOCATION)) {
            location = (Location) intent.getExtras().get(Constants.INTENT_EXTRA_LOCATION);
        }

        if (intent.hasExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION)) {
            destination = intent.getIntExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION, 0);
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
            boolean doUpdate = intent.getBooleanExtra(Constants.INTENT_EXTRA_FORCE_REFRESH, false);

            // If it's not a forced update (for example from the Activity being restarted) then
            // check to see if we've moved far enough, or there's been a long enough delay since
            // the last update and if so, enforce a new update.
            if (!doUpdate) {
                // Retrieve the last update time and place.
                long lastTime = preferences.getLong(Constants.LAST_UPDATED_TIME, Long.MIN_VALUE);
                double lastLatitude = Double.longBitsToDouble(
                        preferences.getLong(Constants.LAST_UPDATED_LATITUDE, Long.MIN_VALUE));
                double lastLongitude = Double.longBitsToDouble(
                        preferences.getLong(Constants.LAST_UPDATED_LONGITUDE, Long.MIN_VALUE));
                Location lastLocation = new Location(Constants.CONSTRUCTED_LOCATION_PROVIDER);
                lastLocation.setLatitude(lastLatitude);
                lastLocation.setLongitude(lastLongitude);

                // If update time and distance bounds have been passed, do an update.
                if ((lastTime < System.currentTimeMillis() - Constants.LOCATION_MAX_TIME) ||
                        (lastLocation.distanceTo(location) > Constants.LOCATION_MAX_DISTANCE))
                    doUpdate = true;
            }

            if (doUpdate) {
                refreshPlaces();
            }
        }
    }

    /**
     * Polls the underlying service to return a list of places within the specified
     * radius of the specified Location.
     */
    protected void refreshPlaces() {
        // Save the last update time and place to the Shared Preferences.
        editor.putLong(Constants.LAST_UPDATED_TIME, System.currentTimeMillis());
        editor.putLong(Constants.LAST_UPDATED_LATITUDE, Double.doubleToLongBits(location.getLatitude()));
        editor.putLong(Constants.LAST_UPDATED_LONGITUDE, Double.doubleToLongBits(location.getLongitude()));
        editor.commit();

        Log.i(TAG, location.getProvider() + "->" + location.getLatitude() + ", " + location.getLongitude());

        switch (destination) {
            case Constants.TWITTER_UPDATE_DESTINATION:
                requestTwitterPlaceUpdate();
                break;
            case Constants.FACEBOOK_UPDATE_DESTINATION:
                requestFacebookPlaceUpdate();
                break;
            default:
                requestTwitterPlaceUpdate();
                requestFacebookPlaceUpdate();
                break;
        }
    }

    private void requestFacebookPlaceUpdate() {
        Intent updateServiceIntent = new Intent(this, FacebookPlaceUpdaterService.class);
        updateServiceIntent.putExtra(Constants.INTENT_EXTRA_LOCATION, location);
        startService(updateServiceIntent);
    }

    private void requestTwitterPlaceUpdate() {
        Intent updateServiceIntent = new Intent(this, TwitterPlaceUpdaterService.class);
        updateServiceIntent.putExtra(Constants.INTENT_EXTRA_LOCATION, location);
        startService(updateServiceIntent);
    }
}