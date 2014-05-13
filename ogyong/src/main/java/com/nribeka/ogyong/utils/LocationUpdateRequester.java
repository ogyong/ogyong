package com.nribeka.ogyong.utils;

import android.app.PendingIntent;
import android.location.Criteria;
import android.location.LocationManager;

/**
 * Provides support for initiating active and passive location updates
 * optimized for the Froyo release. Includes use of the Passive Location Provider.
 * <p/>
 * Uses broadcast Intents to notify the app of location changes.
 */
public class LocationUpdateRequester {

    protected LocationManager locationManager;

    public LocationUpdateRequester(LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    public void requestLocationUpdates(long minTime, long minDistance, Criteria criteria, PendingIntent pendingIntent) {
        locationManager.requestLocationUpdates(minTime, minDistance, criteria, pendingIntent);
    }

    public void requestPassiveLocationUpdates(long minimumTime, long minimumDistance, PendingIntent pendingIntent) {
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, minimumTime, minimumDistance, pendingIntent);
    }
}
