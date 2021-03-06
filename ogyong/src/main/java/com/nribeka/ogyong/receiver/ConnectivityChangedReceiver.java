package com.nribeka.ogyong.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * This Receiver class is designed to listen for changes in connectivity.
 * <p/>
 * When we lose connectivity the relevant Service classes will automatically
 * disable passive Location updates and queue pending checkins.
 * <p/>
 * This class will restart the  checkin service to retry pending checkins
 * and re-enables passive location updates.
 */
public class ConnectivityChangedReceiver extends BroadcastReceiver {

    private static final String TAG = ConnectivityChangedReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Executing receiver ...");

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check if we are connected to an active data network.
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (isConnected) {
            PackageManager packageManager = context.getPackageManager();

            ComponentName connectivityReceiver = new ComponentName(context, ConnectivityChangedReceiver.class);
            ComponentName locationReceiver = new ComponentName(context, LocationChangedActiveReceiver.class);
            ComponentName passiveLocationReceiver = new ComponentName(context, LocationChangedPassiveReceiver.class);

            // The default state for this Receiver is disabled. it is only
            // enabled when a Service disables updates pending connectivity.
            packageManager.setComponentEnabledSetting(connectivityReceiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP);

            // The default state for the Location Receiver is enabled. it is only
            // disabled when a Service disables updates pending connectivity.
            packageManager.setComponentEnabledSetting(locationReceiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP);

            // The default state for the Location Receiver is enabled. it is only
            // disabled when a Service disables updates pending connectivity.
            packageManager.setComponentEnabledSetting(passiveLocationReceiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                    PackageManager.DONT_KILL_APP);
        }
    }
}