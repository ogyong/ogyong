package com.nribeka.ogyong.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
import com.facebook.widget.ProfilePictureView;
import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.R;
import com.nribeka.ogyong.listener.LocationSelectionListener;
import com.nribeka.ogyong.service.StatusUpdaterService;
import com.nribeka.ogyong.utils.OgyongUtils;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/**
 */
public class FacebookPostFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = FacebookPostFragment.class.getSimpleName();
    private static final List<String> PERMISSIONS = Arrays.asList("basic_info");
    private static final String FRAGMENT_NAME = "facebook.fragment.name";
    private static final String FRAGMENT_DESCRIPTION = "facebook.fragment.description";
    private Session.StatusCallback callback = new Session.StatusCallback() {
        @Override
        public void call(final Session session, final SessionState state, final Exception exception) {
            onSessionStateChange(session, state, exception);
        }
    };
    private String fragmentName;
    private String fragmentDescription;

    private MenuItem postMenuItem;

    private CheckBox includeLocationCb;
    private CheckBox randomizeLocationCb;

    private UiLifecycleHelper uiHelper;
    private ProfilePictureView profilePictureView;

    private EditText facebookStatusEditText;

    private TextView placeTextView;
    private TextView latLongTextView;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private LocationSelectionListener locationSelectionListener;

    public FacebookPostFragment() {
        // empty constructor
    }

    public static FacebookPostFragment newInstance(String name, String description) {
        FacebookPostFragment fragment = new FacebookPostFragment();
        Bundle arguments = new Bundle();
        arguments.putString(FRAGMENT_NAME, name);
        arguments.putString(FRAGMENT_DESCRIPTION, description);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_facebook_post, container, false);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = preferences.edit();

        facebookStatusEditText = (EditText) view.findViewById(R.id.facebook_status_edit_text);
        facebookStatusEditText.setOnFocusChangeListener(new StatusFocusListener());
        profilePictureView = (ProfilePictureView) view.findViewById(R.id.facebook_profile_picture);

        includeLocationCb = (CheckBox) view.findViewById(R.id.facebook_include_location_cb);
        includeLocationCb.setOnClickListener(this);
        randomizeLocationCb = (CheckBox) view.findViewById(R.id.facebook_randomize_location_cb);
        randomizeLocationCb.setOnClickListener(this);

        placeTextView = (TextView) view.findViewById(R.id.facebook_place_text_view);
        latLongTextView = (TextView) view.findViewById(R.id.facebook_lat_long_text_view);

        LoginButton loginButton = (LoginButton) view.findViewById(R.id.facebook_login_button);
        loginButton.setUserInfoChangedCallback(new UserInfoChangedCallback());
        loginButton.setPublishPermissions(PERMISSIONS);
        loginButton.setFragment(this);
        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            locationSelectionListener = (LocationSelectionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    private void postStatusUpdate() {
        Intent updateServiceIntent = new Intent(getActivity(), StatusUpdaterService.class);
        updateServiceIntent.putExtra(Constants.INTENT_EXTRA_UPDATE_DESTINATION, Constants.FACEBOOK_UPDATE_DESTINATION);
        getActivity().startService(updateServiceIntent);
    }

    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.post_status:
                postStatusUpdate();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_post_fragment, menu);

        Session session = Session.getActiveSession();
        boolean enablePost = (session != null && session.isOpened());
        postMenuItem = menu.findItem(R.id.post_status);
        if (postMenuItem != null) {
            postMenuItem.setVisible(enablePost);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHelper = new UiLifecycleHelper(getActivity(), callback);
        uiHelper.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        uiHelper.onResume();
        updateInterface();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        uiHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (state != null) {
            if (state.isClosed()) {
                editor.remove(Constants.FACEBOOK_INCLUDE_LOCATION);
                editor.remove(Constants.FACEBOOK_RANDOMIZE_LOCATION);
                editor.commit();
            }
        }

        if (exception != null) {
            Log.i(TAG, "Facebook exception message: " + exception.getLocalizedMessage(), exception);
        }

        // check the session
        if (session != null && session.isOpened()) {
            if (!session.getPermissions().contains("publish_actions")) {
                // create a new permission
                Session.NewPermissionsRequest newPermissionsRequest =
                        new Session.NewPermissionsRequest(this, Arrays.asList("publish_actions"));
                // set a new publish permission request
                session.requestNewPublishPermissions(newPermissionsRequest);
            }
        }
        updateInterface();
    }

    private void updateInterface() {
        Session session = Session.getActiveSession();
        boolean isOpened = session != null && session.isOpened();
        int visible = isOpened ? View.VISIBLE : View.GONE;
        includeLocationCb.setVisibility(visible);
        randomizeLocationCb.setVisibility(visible);
        facebookStatusEditText.setVisibility(visible);

        boolean includeLocation = preferences.getBoolean(Constants.FACEBOOK_INCLUDE_LOCATION, false);
        boolean randomizeLocation = preferences.getBoolean(Constants.FACEBOOK_RANDOMIZE_LOCATION, false);
        if (visible == View.VISIBLE) {
            facebookStatusEditText.setText(OgyongUtils.generateStatus(getActivity()));
            includeLocationCb.setChecked(includeLocation);
            randomizeLocationCb.setChecked(randomizeLocation);
            if (includeLocation) {
                randomizeLocationCb.setEnabled(true);
            }
        }

        int locationVisible = (isOpened && includeLocation) ? View.VISIBLE : View.GONE;
        placeTextView.setVisibility(locationVisible);
        latLongTextView.setVisibility(locationVisible);
        if (locationVisible == View.VISIBLE) {
            double latitude = Double.longBitsToDouble(preferences.getLong(Constants.FACEBOOK_LATITUDE, Long.MIN_VALUE));
            double longitude = Double.longBitsToDouble(preferences.getLong(Constants.FACEBOOK_LONGITUDE, Long.MIN_VALUE));

            String latLong = String.valueOf(latitude) + ", " + String.valueOf(longitude);
            String hashValue = OgyongUtils.generateHash(latLong);

            String facebookPlace = preferences.getString("facebook:name:" + hashValue, Constants.PLACE_UNKNOWN);

            DecimalFormat decimalFormat = new DecimalFormat("#.000000");
            String latLongText = decimalFormat.format(latitude) + ", " + decimalFormat.format(longitude);

            setLatLongTextView(latLongText);
            setPlaceTextView(facebookPlace);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.facebook_include_location_cb:
                onIncludeLocationChecked(view);
                break;
            case R.id.facebook_randomize_location_cb:
                onRandomizeLocationChecked(view);
                break;
        }
    }

    private void onRandomizeLocationChecked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        locationSelectionListener.onRandomLocation(Constants.FACEBOOK_UPDATE_DESTINATION, checked);

        editor.putBoolean(Constants.FACEBOOK_RANDOMIZE_LOCATION, checked);
        editor.commit();
    }

    private void onIncludeLocationChecked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        randomizeLocationCb.setEnabled(checked);

        if (!checked) {
            randomizeLocationCb.setChecked(false);
            editor.remove(Constants.FACEBOOK_RANDOMIZE_LOCATION);
        }

        locationSelectionListener.onIncludeLocation(Constants.FACEBOOK_UPDATE_DESTINATION, checked);

        int visibility = checked ? View.VISIBLE : View.GONE;
        placeTextView.setVisibility(visibility);
        latLongTextView.setVisibility(visibility);

        editor.putBoolean(Constants.FACEBOOK_INCLUDE_LOCATION, checked);
        editor.commit();
    }

    public void setPlaceTextView(String place) {
        placeTextView.setText(place);
    }

    public void setLatLongTextView(String latLong) {
        latLongTextView.setText(latLong);
    }

    public void setStatusMessage(String statusMessage) {
        facebookStatusEditText.setText(statusMessage);
    }

    private class StatusFocusListener implements View.OnFocusChangeListener {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (postMenuItem != null) {
                if (hasFocus) {
                    postMenuItem.setVisible(true);
                } else {
                    postMenuItem.setVisible(false);
                }
            }
        }
    }

    private class UserInfoChangedCallback implements LoginButton.UserInfoChangedCallback {
        @Override
        public void onUserInfoFetched(GraphUser user) {
            profilePictureView.setProfileId(user == null ? null : user.getId());
        }
    }
}
