package com.nribeka.ogyong.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.nribeka.ogyong.service.PlaceUpdaterService;
import com.nribeka.ogyong.utils.AppConstants;

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
        String locationKey = LocationManager.KEY_LOCATION_CHANGED;
        String providerEnabledKey = LocationManager.KEY_PROVIDER_ENABLED;
        if (intent.hasExtra(providerEnabledKey)) {
            if (!intent.getBooleanExtra(providerEnabledKey, true)) {
                Intent providerDisabledIntent = new Intent(AppConstants.INTENT_LOCATION_PROVIDER_DISABLED);
                context.sendBroadcast(providerDisabledIntent);
            }
        }
        if (intent.hasExtra(locationKey)) {
            Location location = (Location) intent.getExtras().get(locationKey);
            Log.d(TAG, "Actively updating place list");
            Intent updateServiceIntent = new Intent(context, PlaceUpdaterService.class);
            updateServiceIntent.putExtra(AppConstants.INTENT_EXTRA_LOCATION, location);
            updateServiceIntent.putExtra(AppConstants.INTENT_EXTRA_RADIUS, AppConstants.LOCATION_DEFAULT_RADIUS);
            updateServiceIntent.putExtra(AppConstants.INTENT_EXTRA_FORCE_REFRESH, true);
            context.startService(updateServiceIntent);
        }
    }
}