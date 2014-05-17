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
import com.nribeka.ogyong.utils.LastLocationFinder;

/**
 * This Receiver class is used to listen for Broadcast Intents that announce
 * that a location change has occurred while this application isn't visible.
 * <p/>
 * Where possible, this is triggered by a Passive Location listener.
 */
public class LocationChangedPassiveReceiver extends BroadcastReceiver {

    protected static String TAG = LocationChangedPassiveReceiver.class.getSimpleName();

    /**
     * When a new location is received, extract it from the Intent and use
     * it to start the Service used to update the list of nearby places.
     * <p/>
     * This is the Passive receiver, used to receive Location updates from
     * third party apps when the Activity is not visible.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String key = LocationManager.KEY_LOCATION_CHANGED;
        Location location = null;

        if (intent.hasExtra(key)) {
            // This update came from Passive provider, so we can extract the location directly.
            location = (Location) intent.getExtras().get(key);
        } else {
            // This update came from a recurring alarm. We need to determine if there
            // has been a more recent Location received than the last location we used.
            // Get the best last location detected from the providers.
            LastLocationFinder lastLocationFinder = new LastLocationFinder(context);
            location = lastLocationFinder.getLastBestLocation(Constants.LOCATION_MAX_DISTANCE,
                    System.currentTimeMillis() - Constants.LOCATION_MAX_TIME);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            // Get the last location we used to get a listing.
            long lastTime = prefs.getLong(Constants.LAST_UPDATED_TIME, Long.MIN_VALUE);
            long lastLatitude = prefs.getLong(Constants.LAST_UPDATED_LATITUDE, Long.MIN_VALUE);
            long lastLongitude = prefs.getLong(Constants.LAST_UPDATED_LONGITUDE, Long.MIN_VALUE);
            Location lastLocation = new Location(Constants.CONSTRUCTED_LOCATION_PROVIDER);
            lastLocation.setLatitude(lastLatitude);
            lastLocation.setLongitude(lastLongitude);
            // Check if the last location detected from the providers is either too soon, or too close to the last
            // value we used. If it is within those thresholds we set the location to null to prevent the update
            // Service being run unnecessarily (and spending battery on data transfers).
            if ((lastTime > System.currentTimeMillis() - Constants.LOCATION_MAX_TIME) ||
                    (lastLocation.distanceTo(location) < Constants.LOCATION_MAX_DISTANCE))
                location = null;
        }

        // Start the Service used to find nearby points of interest based on the last detected location.
        if (location != null) {
            Log.d(TAG, "Passively updating place list.");
            Intent updateServiceIntent = new Intent(context, PlaceUpdaterService.class);
            updateServiceIntent.putExtra(Constants.INTENT_EXTRA_LOCATION, location);
            updateServiceIntent.putExtra(Constants.INTENT_EXTRA_RADIUS, Constants.LOCATION_DEFAULT_RADIUS);
            updateServiceIntent.putExtra(Constants.INTENT_EXTRA_FORCE_REFRESH, false);
            context.startService(updateServiceIntent);
        }
    }
}