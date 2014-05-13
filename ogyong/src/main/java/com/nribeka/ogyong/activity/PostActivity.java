package com.nribeka.ogyong.activity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.nribeka.ogyong.R;
import com.nribeka.ogyong.adapter.SmartFragmentStatePagerAdapter;
import com.nribeka.ogyong.fragment.FacebookPostFragment;
import com.nribeka.ogyong.fragment.TwitterPostFragment;
import com.nribeka.ogyong.listener.OnLocationTrackingListener;
import com.nribeka.ogyong.receiver.LocationChangedActiveReceiver;
import com.nribeka.ogyong.receiver.LocationChangedPassiveReceiver;
import com.nribeka.ogyong.receiver.StatusUpdatedReceiver;
import com.nribeka.ogyong.service.PlaceUpdaterService;
import com.nribeka.ogyong.utils.AppConstants;
import com.nribeka.ogyong.utils.AppUtils;
import com.nribeka.ogyong.utils.LastLocationFinder;
import com.nribeka.ogyong.utils.LocationUpdateRequester;

import java.text.DecimalFormat;


public class PostActivity extends ActionBarActivity implements ActionBar.TabListener, OnLocationTrackingListener {

    private static final String TAG = PostActivity.class.getSimpleName();
    protected PackageManager packageManager;
    protected NotificationManager notificationManager;
    protected LocationManager locationManager;
    protected Criteria criteria;
    protected LastLocationFinder lastLocationFinder;
    protected LocationUpdateRequester locationUpdateRequester;
    protected PendingIntent locationListenerActivePendingIntent;
    protected PendingIntent locationListenerPassivePendingIntent;
    protected ComponentName statusReceiverName;
    protected IntentFilter locationUpdatedIntentFilter;
    protected IntentFilter musicUpdatedIntentFilter;
    protected IntentFilter statusUpdatedIntentFilter;
    /**
     * One-off location listener that receives updates from the {@link LastLocationFinder}.
     * This is triggered where the last known location is outside the bounds of our maximum
     * distance and latency.
     */
    protected LocationListener oneShotLastLocationUpdateListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            updatePlaces(location, AppConstants.LOCATION_DEFAULT_RADIUS, true);
        }

        public void onProviderDisabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }
    };
    /**
     * If the best Location Provider (usually GPS) is not available when we request location
     * updates, this listener will be notified if / when it becomes available. It calls
     * requestLocationUpdates to re-register the location listeners using the better Location
     * Provider.
     */
    protected LocationListener bestInactiveLocationProviderListener = new LocationListener() {
        public void onLocationChanged(Location location) {
        }

        public void onProviderDisabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
            // Re-register the location listeners using the better Location Provider.
            requestLocationUpdates();
        }
    };
    /**
     * If the Location Provider we're using to receive location updates is disabled while the
     * app is running, this Receiver will be notified, allowing us to re-register our Location
     * Receivers using the best available Location Provider is still available.
     */
    protected BroadcastReceiver locationProviderDisabledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean providerDisabled = !intent.getBooleanExtra(LocationManager.KEY_PROVIDER_ENABLED, false);
            // Re-register the location listeners using the best available Location Provider.
            if (providerDisabled)
                requestLocationUpdates();
        }
    };
    private ActionBar actionBar;
    private ViewPager viewPager;
    private SmartFragmentStatePagerAdapter sectionsPagerAdapter;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private BroadcastReceiver locationUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = Double.longBitsToDouble(preferences.getLong(AppConstants.SP_LAST_UPDATED_LATITUDE, Long.MIN_VALUE));
            double longitude = Double.longBitsToDouble(preferences.getLong(AppConstants.SP_LAST_UPDATED_LONGITUDE, Long.MIN_VALUE));

            String latLong = String.valueOf(latitude) + ", " + String.valueOf(longitude);
            String hashValue = AppUtils.generateHash(latLong);

            String twitterPlace = preferences.getString("twitter:name:" + hashValue, AppConstants.EMPTY_STRING);
            String facebookPlace = preferences.getString("facebook:name:" + hashValue, AppConstants.EMPTY_STRING);

            DecimalFormat decimalFormat = new DecimalFormat("#.000000");
            String latLongText = decimalFormat.format(latitude) + ", " + decimalFormat.format(longitude);

            TwitterPostFragment twitterPostFragment = (TwitterPostFragment) sectionsPagerAdapter.getRegisteredFragment(1);
            twitterPostFragment.setLatLongTextView(latLongText);
            twitterPostFragment.setPlaceTextView(twitterPlace);
            FacebookPostFragment facebookPostFragment = (FacebookPostFragment) sectionsPagerAdapter.getRegisteredFragment(0);
            facebookPostFragment.setLatLongTextView(latLongText);
            facebookPostFragment.setPlaceTextView(facebookPlace);
        }
    };

    private BroadcastReceiver musicUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String statusMessage = AppUtils.generateStatus(context);
            TwitterPostFragment twitterPostFragment = (TwitterPostFragment) sectionsPagerAdapter.getRegisteredFragment(1);
            twitterPostFragment.setStatusMessage(statusMessage);
            FacebookPostFragment facebookPostFragment = (FacebookPostFragment) sectionsPagerAdapter.getRegisteredFragment(0);
            facebookPostFragment.setStatusMessage(statusMessage);
        }
    };

    private BroadcastReceiver statusUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String destination = intent.getStringExtra(AppConstants.INTENT_EXTRA_MESSAGE_DESTINATION);
            String message = "Music player information posted to " + destination;
            Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
            toast.show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        packageManager = getPackageManager();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        editor = preferences.edit();
        editor.putBoolean(AppConstants.SP_RUN_ONCE, true);
        editor.commit();

        // Setup the location update Pending Intents
        Intent activeIntent = new Intent(this, LocationChangedActiveReceiver.class);
        locationListenerActivePendingIntent = PendingIntent.getBroadcast(this, 0, activeIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent passiveIntent = new Intent(this, LocationChangedPassiveReceiver.class);
        locationListenerPassivePendingIntent = PendingIntent.getBroadcast(this, 0, passiveIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        lastLocationFinder = new LastLocationFinder(this);
        lastLocationFinder.setChangedLocationListener(oneShotLastLocationUpdateListener);

        // Instantiate a Location Update Requester class based on the available platform version.
        // This will be used to request location updates.
        locationUpdateRequester = new LocationUpdateRequester(locationManager);

        // Create an Intent Filter to listen for status update
        statusReceiverName = new ComponentName(this, StatusUpdatedReceiver.class);
        musicUpdatedIntentFilter = new IntentFilter(AppConstants.INTENT_MUSIC_UPDATED);
        locationUpdatedIntentFilter = new IntentFilter(AppConstants.INTENT_LOCATION_UPDATED);
        statusUpdatedIntentFilter = new IntentFilter(AppConstants.INTENT_STATUS_POSTED_ACTION);

        criteria = new Criteria();
        if (AppConstants.USE_GPS_WHEN_ACTIVITY_VISIBLE) {
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
        } else {
            criteria.setPowerRequirement(Criteria.POWER_LOW);
        }

        // Set up the action bar.
        actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.narsis_pager);
        viewPager.setAdapter(sectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < sectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            ActionBar.Tab tab = actionBar.newTab();
            tab.setText(sectionsPagerAdapter.getPageTitle(i));
            tab.setTabListener(this);
            actionBar.addTab(tab);
        }
    }

    @Override
    protected void onPause() {
        editor.putBoolean(AppConstants.SP_APP_IN_BACKGROUND, true);
        editor.commit();

        unregisterReceiver(musicUpdatedReceiver);
        unregisterReceiver(statusUpdatedReceiver);
        unregisterReceiver(locationUpdatedReceiver);
        // Enable the Manifest Checkin Receiver when the Activity isn't active.
        // The Manifest Checkin Receiver is designed to run only when the Application
        // isn't active to notify the user of pending checkins that have succeeded
        // (usually through a Notification).
        packageManager.setComponentEnabledSetting(statusReceiverName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        // Stop listening for location updates when the Activity is inactive.
        // TODO: when the receiver is already unregistered from the un-check cb, and then this happen, it will throw exception
        disableLocationUpdates();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null && intent.getData() != null) {
            Uri uri = intent.getData();
            if (uri.toString().startsWith(AppConstants.TWITTER_CALLBACK)) {
                String verifier = uri.getQueryParameter(AppConstants.SP_TWITTER_OAUTH_VERIFIER);
                editor.putString(AppConstants.SP_TWITTER_OAUTH_VERIFIER, verifier);
                intent.setData(null);
            }
        }
        editor.putBoolean(AppConstants.SP_APP_IN_BACKGROUND, false);
        editor.commit();

        registerReceiver(musicUpdatedReceiver, musicUpdatedIntentFilter);
        registerReceiver(statusUpdatedReceiver, statusUpdatedIntentFilter);
        registerReceiver(locationUpdatedReceiver, locationUpdatedIntentFilter);
        boolean facebookIncludeLocation = preferences.getBoolean(AppConstants.SP_FACEBOOK_INCLUDE_LOCATION, false);
        boolean facebookRandomizeLocation = preferences.getBoolean(AppConstants.SP_FACEBOOK_RANDOMIZE_LOCATION, false);
        boolean twitterIncludeLocation = preferences.getBoolean(AppConstants.SP_TWITTER_INCLUDE_LOCATION, false);
        boolean twitterRandomizeLocation = preferences.getBoolean(AppConstants.SP_TWITTER_RANDOMIZE_LOCATION, false);
        if ((facebookIncludeLocation && !facebookRandomizeLocation)
                || (twitterIncludeLocation && !twitterRandomizeLocation)) {
            boolean followLocationChanges = preferences.getBoolean(AppConstants.SP_FOLLOW_LOCATION_CHANGES, true);
            getLocationAndUpdatePlaces(followLocationChanges);
        }

        // Disable the Manifest Checkin Receiver when the Activity is visible.
        // The Manifest Checkin Receiver is designed to run only when the Application
        // isn't active to notify the user of pending checkins that have succeeded
        // (usually through a Notification).
        // When the Activity is visible we capture checkins through the checkinReceiver.
        packageManager.setComponentEnabledSetting(statusReceiverName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // Cancel notifications.
        notificationManager.cancel(AppConstants.STATUS_NOTIFICATION);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_post, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * Find the last known location (using a {@link LastLocationFinder}) and updates the
     * place list accordingly.
     *
     * @param updateWhenLocationChanges Request location updates
     */
    protected void getLocationAndUpdatePlaces(boolean updateWhenLocationChanges) {
        // This isn't directly affecting the UI, so put it on a worker thread.
        AsyncTask<Void, Void, Void> findLastLocationTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Find the last known location, specifying a required accuracy of within the min distance between updates
                // and a required latency of the minimum time required between updates.
                Location lastKnownLocation = lastLocationFinder.getLastBestLocation(AppConstants.LOCATION_MAX_DISTANCE,
                        System.currentTimeMillis() - AppConstants.LOCATION_MAX_TIME);

                // Update the place list based on the last known location within a defined radius.
                // Note that this is *not* a forced update. The Place List Service has settings to
                // determine how frequently the underlying web service should be pinged. This function
                // is called every time the Activity becomes active, so we don't want to flood the server
                // unless the location has changed or a minimum latency or distance has been covered.
                updatePlaces(lastKnownLocation, AppConstants.LOCATION_DEFAULT_RADIUS, false);
                return null;
            }
        };
        findLastLocationTask.execute();

        // If we have requested location updates, turn them on here.
        toggleUpdatesWhenLocationChanges(updateWhenLocationChanges);
    }

    /**
     * Choose if we should receive location updates.
     *
     * @param updateWhenLocationChanges Request location updates
     */
    protected void toggleUpdatesWhenLocationChanges(boolean updateWhenLocationChanges) {
        editor.putBoolean(AppConstants.SP_FOLLOW_LOCATION_CHANGES, true);
        editor.commit();
        // Start or stop listening for location changes
        if (updateWhenLocationChanges) {
            requestLocationUpdates();
        } else {
            disableLocationUpdates();
        }
    }

    /**
     * Start listening for location updates.
     */
    protected void requestLocationUpdates() {
        // Normal updates while activity is visible.
        locationUpdateRequester.requestLocationUpdates(
                AppConstants.LOCATION_MAX_TIME,
                AppConstants.LOCATION_MAX_DISTANCE,
                criteria, locationListenerActivePendingIntent);

        // Passive location updates from 3rd party apps when the Activity isn't visible.
        locationUpdateRequester.requestPassiveLocationUpdates(
                AppConstants.LOCATION_PASSIVE_MAX_TIME,
                AppConstants.LOCATION_PASSIVE_MAX_DISTANCE, locationListenerPassivePendingIntent);

        // Register a receiver that listens for when the provider I'm using has been disabled.
        // IntentFilter intentFilter = new IntentFilter(AppConstants.INTENT_LOCATION_PROVIDER_DISABLED);
        // registerReceiver(locationProviderDisabledReceiver, intentFilter);

        // Register a receiver that listens for when a better provider than I'm using becomes available.
        String bestProvider = locationManager.getBestProvider(criteria, false);
        String bestAvailableProvider = locationManager.getBestProvider(criteria, true);
        if (bestProvider != null && !bestProvider.equals(bestAvailableProvider)) {
            locationManager.requestLocationUpdates(bestProvider, 0, 0, bestInactiveLocationProviderListener, getMainLooper());
        }
    }

    /**
     * Stop listening for location updates
     */
    protected void disableLocationUpdates() {
        // unregisterReceiver(locationProviderDisabledReceiver);
        locationManager.removeUpdates(locationListenerActivePendingIntent);
        locationManager.removeUpdates(bestInactiveLocationProviderListener);
        if (isFinishing()) {
            lastLocationFinder.cancel();
        }
        if (AppConstants.DISABLE_PASSIVE_LOCATION_WHEN_USER_EXIT && isFinishing()) {
            locationManager.removeUpdates(locationListenerPassivePendingIntent);
        }
    }

    /**
     * Update the list of nearby places centered on the specified Location, within the specified radius.
     * This will start the {@link com.nribeka.ogyong.service.PlaceUpdaterService} that will poll the underlying web service.
     *
     * @param location     Location
     * @param radius       Radius (meters)
     * @param forceRefresh Force Refresh
     */
    protected void updatePlaces(Location location, int radius, boolean forceRefresh) {
        if (location != null) {
            Log.d(TAG, "Updating place by calling the service.");
            // Start the PlacesUpdateService. Note that we use an action rather than specifying the
            // class directly. That's because we have different variations of the Service for different
            // platform versions.
            Intent updateServiceIntent = new Intent(this, PlaceUpdaterService.class);
            updateServiceIntent.putExtra(AppConstants.INTENT_EXTRA_LOCATION, location);
            updateServiceIntent.putExtra(AppConstants.INTENT_EXTRA_RADIUS, radius);
            updateServiceIntent.putExtra(AppConstants.INTENT_EXTRA_FORCE_REFRESH, forceRefresh);
            startService(updateServiceIntent);
        } else {
            Log.d(TAG, "Updating place list for: No Previous Location Found");
        }
    }

    @Override
    public void onLocationTrackingEnabled(View view) {
        boolean updateLocation = false;
        boolean viewChecked = ((CheckBox) view).isChecked();
        if (viewChecked) {
            switch (view.getId()) {
                case R.id.facebook_include_location_cb:
                case R.id.twitter_include_location_cb:
                    updateLocation = true;
                    break;
                case R.id.facebook_randomize_location_cb:
                case R.id.twitter_randomize_location_cb:
                    updateLocation = false;
                    break;
                default:
                    break;
            }
        }
        if (updateLocation) {
            // Request the last known location.
            Location lastKnownLocation = lastLocationFinder.getLastBestLocation(AppConstants.LOCATION_MAX_DISTANCE,
                    System.currentTimeMillis() - AppConstants.LOCATION_MAX_TIME);
            // Force an update
            updatePlaces(lastKnownLocation, AppConstants.LOCATION_DEFAULT_RADIUS, true);
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    private class SectionsPagerAdapter extends SmartFragmentStatePagerAdapter {

        private int NUM_ITEMS = 2;

        public SectionsPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 1:
                    return TwitterPostFragment.newInstance("Twitter", "Twitter integration.");
                case 0:
                    return FacebookPostFragment.newInstance("Facebook", "Facebook integration.");
            }
            return null;
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return "Twitter";
                case 0:
                    return "Facebook";
            }
            return "Unknown Page";
        }
    }

}
