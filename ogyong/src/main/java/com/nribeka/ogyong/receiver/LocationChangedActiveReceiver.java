package com.nribeka.ogyong.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.service.PlaceUpdaterService;
import com.nribeka.ogyong.utils.OgyongUtils;

/**
 * This Receiver class is used to listen for Broadcast Intents that announce
 * that a location change has occurred. This is used instead of a LocationListener
 * within an Activity is our only action is to start a service.
 */
public class LocationChangedActiveReceiver extends BroadcastReceiver {

    protected static String TAG = LocationChangedActiveReceiver.class.getSimpleName();

    /**
     * When a new location is received, extract it from the Intent and use
     * it to start the Service used to update the list of nearby places.
     * <p/>
     * This is the Active receiver, used to receive Location updates when
     * the Activity is visible.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Executing receiver ...");
        String locationKey = LocationManager.KEY_LOCATION_CHANGED;
        if (intent.hasExtra(locationKey)) {
            Location location = (Location) intent.getExtras().get(locationKey);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean facebookIncludeLocation = preferences.getBoolean(Constants.FACEBOOK_INCLUDE_LOCATION, false);
            if (facebookIncludeLocation) {
                boolean facebookRandomizeLocation = preferences.getBoolean(Constants.FACEBOOK_RANDOMIZE_LOCATION, false);
                if (facebookRandomizeLocation) {
                    location = OgyongUtils.getRandomLocation(context);
                }
                Log.d(TAG, "Starting service from active receiver to update facebook place list!");
                Intent updateServiceIntent = new Intent(context, PlaceUpdaterService.class);
                updateServiceIntent.putExtra(Constants.INTENT_EXTRA_LOCATION, location);
                updateServiceIntent.putExtra(Constants.INTENT_EXTRA_RADIUS, Constants.LOCATION_DEFAULT_RADIUS);
                updateServiceIntent.putExtra(
                        Constants.INTENT_EXTRA_UPDATE_DESTINATION, Constants.FACEBOOK_UPDATE_DESTINATION);
                updateServiceIntent.putExtra(Constants.INTENT_EXTRA_FORCE_REFRESH, true);
                context.startService(updateServiceIntent);
            }

            boolean twitterIncludeLocation = preferences.getBoolean(Constants.TWITTER_INCLUDE_LOCATION, false);
            if (twitterIncludeLocation) {
                boolean twitterRandomizeLocation = preferences.getBoolean(Constants.TWITTER_RANDOMIZE_LOCATION, false);
                if (twitterRandomizeLocation) {
                    location = OgyongUtils.getRandomLocation(context);
                }
                Log.d(TAG, "Starting service from active receiver to update twitter place list!");
                Intent updateServiceIntent = new Intent(context, PlaceUpdaterService.class);
                updateServiceIntent.putExtra(Constants.INTENT_EXTRA_LOCATION, location);
                updateServiceIntent.putExtra(Constants.INTENT_EXTRA_RADIUS, Constants.LOCATION_DEFAULT_RADIUS);
                updateServiceIntent.putExtra(
                        Constants.INTENT_EXTRA_UPDATE_DESTINATION, Constants.TWITTER_UPDATE_DESTINATION);
                updateServiceIntent.putExtra(Constants.INTENT_EXTRA_FORCE_REFRESH, true);
                context.startService(updateServiceIntent);
            }
        }
    }
}