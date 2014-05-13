package com.nribeka.ogyong.receiver;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.preference.PreferenceManager;

import com.nribeka.ogyong.utils.AppConstants;
import com.nribeka.ogyong.utils.LocationUpdateRequester;

/**
 * This Receiver class is designed to listen for system boot.
 * <p/>
 * If the app has been run at least once, the passive location
 * updates should be enabled after a reboot.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean runOnce = preferences.getBoolean(AppConstants.SP_RUN_ONCE, false);

        if (runOnce) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            // This will be used to request location updates.
            LocationUpdateRequester locationUpdateRequester = new LocationUpdateRequester(locationManager);
            // Check the Shared Preferences to see if the user is wants to use location.
            boolean facebookIncludeLocation = preferences.getBoolean(AppConstants.SP_FACEBOOK_INCLUDE_LOCATION, false);
            boolean facebookRandomizeLocation = preferences.getBoolean(AppConstants.SP_FACEBOOK_RANDOMIZE_LOCATION, false);
            boolean twitterIncludeLocation = preferences.getBoolean(AppConstants.SP_TWITTER_INCLUDE_LOCATION, false);
            boolean twitterRandomizeLocation = preferences.getBoolean(AppConstants.SP_TWITTER_RANDOMIZE_LOCATION, false);
            if ((facebookIncludeLocation && !facebookRandomizeLocation)
                    || (twitterIncludeLocation && !twitterRandomizeLocation)) {
                // Passive location updates from 3rd party apps when the Activity isn't visible.
                Intent passiveIntent = new Intent(context, LocationChangedPassiveReceiver.class);
                PendingIntent locationListenerPassivePendingIntent = PendingIntent.getActivity(context, 0, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                locationUpdateRequester.requestPassiveLocationUpdates(AppConstants.LOCATION_PASSIVE_MAX_TIME, AppConstants.LOCATION_PASSIVE_MAX_DISTANCE, locationListenerPassivePendingIntent);
            }
        }
    }
}