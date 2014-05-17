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

    public LocationUpdateRequester(final LocationManager locationManager) {
        this.locationManager = locationManager;
    }

    public void requestLocationUpdates(final long minTime, final long minDistance,
                                       final Criteria criteria, final PendingIntent pendingIntent) {
        locationManager.requestLocationUpdates(minTime, minDistance, criteria, pendingIntent);
    }

    public void requestPassiveLocationUpdates(final long minimumTime, final long minimumDistance,
                                              final PendingIntent pendingIntent) {
        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, minimumTime,
                minimumDistance, pendingIntent);
    }
}
