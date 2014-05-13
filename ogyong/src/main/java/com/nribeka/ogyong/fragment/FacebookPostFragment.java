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
import com.nribeka.ogyong.R;
import com.nribeka.ogyong.listener.OnLocationTrackingListener;
import com.nribeka.ogyong.service.StatusUpdaterService;
import com.nribeka.ogyong.utils.AppConstants;
import com.nribeka.ogyong.utils.AppUtils;

import java.util.Arrays;
import java.util.List;

/**
 */
public class FacebookPostFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "FacebookPostFragment";
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

    private CheckBox facebookIncludeLocationCb;
    private CheckBox facebookRandomizeLocationCb;

    private UiLifecycleHelper uiHelper;
    private ProfilePictureView profilePictureView;

    private EditText facebookStatusEditText;

    private TextView placeTextView;
    private TextView latLongTextView;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private OnLocationTrackingListener onLocationTrackingListener;

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
        profilePictureView = (ProfilePictureView) view.findViewById(R.id.facebook_profile_picture);

        facebookIncludeLocationCb = (CheckBox) view.findViewById(R.id.facebook_include_location_cb);
        facebookIncludeLocationCb.setOnClickListener(this);
        facebookRandomizeLocationCb = (CheckBox) view.findViewById(R.id.facebook_randomize_location_cb);
        facebookRandomizeLocationCb.setOnClickListener(this);

        placeTextView = (TextView) view.findViewById(R.id.facebook_place_text_view);
        latLongTextView = (TextView) view.findViewById(R.id.facebook_lat_long_text_view);

        LoginButton loginButton = (LoginButton) view.findViewById(R.id.facebook_login_button);
        loginButton.setPublishPermissions(PERMISSIONS);
        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                profilePictureView.setProfileId(user == null ? null : user.getId());
            }
        });
        loginButton.setFragment(this);
        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            onLocationTrackingListener = (OnLocationTrackingListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    private void postStatusUpdate() {
        Intent updateServiceIntent = new Intent(getActivity(), StatusUpdaterService.class);
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
            Log.i(TAG, "State isOpened: " + state.isOpened() + ", isClosed: " + state.isClosed());
        }

        if (exception != null) {
            Log.i(TAG, "Exception message: " + exception.getLocalizedMessage(), exception);
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
        facebookIncludeLocationCb.setVisibility(visible);
        facebookRandomizeLocationCb.setVisibility(visible);
        facebookStatusEditText.setVisibility(visible);
        if (visible == View.VISIBLE) {
            facebookStatusEditText.setText(AppUtils.generateStatus(getActivity()));
            facebookIncludeLocationCb.setChecked(preferences.getBoolean(AppConstants.SP_FACEBOOK_INCLUDE_LOCATION, false));
            facebookRandomizeLocationCb.setChecked(preferences.getBoolean(AppConstants.SP_FACEBOOK_RANDOMIZE_LOCATION, false));
        }
        boolean includeLocation = preferences.getBoolean(AppConstants.SP_FACEBOOK_INCLUDE_LOCATION, false);
        int locationVisible = (isOpened && includeLocation) ? View.VISIBLE : View.GONE;
        placeTextView.setVisibility(locationVisible);
        latLongTextView.setVisibility(locationVisible);
    }

    @Override
    public void onClick(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        switch (view.getId()) {
            case R.id.facebook_include_location_cb:
                editor.putBoolean(AppConstants.SP_FACEBOOK_INCLUDE_LOCATION, checked);
                facebookRandomizeLocationCb.setEnabled(checked);
                int visibility = checked ? View.VISIBLE : View.GONE;
                placeTextView.setVisibility(visibility);
                latLongTextView.setVisibility(visibility);
                break;
            case R.id.facebook_randomize_location_cb:
                editor.putBoolean(AppConstants.SP_FACEBOOK_RANDOMIZE_LOCATION, checked);
                break;
        }
        editor.commit();
        onLocationTrackingListener.onLocationTrackingEnabled(view);
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
}
