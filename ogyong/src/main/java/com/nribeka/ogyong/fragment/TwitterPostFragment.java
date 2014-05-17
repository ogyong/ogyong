package com.nribeka.ogyong.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.R;
import com.nribeka.ogyong.listener.AccessTokenListener;
import com.nribeka.ogyong.listener.LocationSelectionListener;
import com.nribeka.ogyong.listener.RequestTokenListener;
import com.nribeka.ogyong.listener.UserBitmapListener;
import com.nribeka.ogyong.listener.UserDetailListener;
import com.nribeka.ogyong.service.StatusUpdaterService;
import com.nribeka.ogyong.task.DownloadAccessTokenTask;
import com.nribeka.ogyong.task.DownloadBitmapTask;
import com.nribeka.ogyong.task.DownloadRequestTokenTask;
import com.nribeka.ogyong.task.DownloadUserTask;
import com.nribeka.ogyong.utils.OgyongUtils;

import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 */
public class TwitterPostFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = TwitterPostFragment.class.getSimpleName();

    private static final String FRAGMENT_NAME = "twitter.fragment.name";
    private static final String FRAGMENT_DESCRIPTION = "twitter.fragment.description";

    private String fragmentName;
    private String fragmentDescription;

    private MenuItem postMenuItem;
    private EditText statusEditText;
    private TextView longTextWarningTextView;
    private TextView nameTextView;
    private TextView handleTextView;
    private TextView placeTextView;
    private TextView latLongTextView;
    private CheckBox includeLocationCb;
    private CheckBox randomizeLocationCb;
    private ImageButton loginButton;
    private ImageButton logoutButton;
    private Bitmap profileBitmap;
    private ImageView profilePicture;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private LocationSelectionListener locationSelectionListener;

    public TwitterPostFragment() {
        // Required empty public constructor
    }

    /**
     * @return A new instance of fragment TwitterPostFragment.
     */
    public static TwitterPostFragment newInstance(String name, String description) {
        TwitterPostFragment fragment = new TwitterPostFragment();
        Bundle arguments = new Bundle();
        arguments.putString(FRAGMENT_NAME, name);
        arguments.putString(FRAGMENT_DESCRIPTION, description);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fragmentName = getArguments().getString(FRAGMENT_NAME);
            fragmentDescription = getArguments().getString(FRAGMENT_DESCRIPTION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_twitter_post, container, false);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = preferences.edit();

        includeLocationCb = (CheckBox) view.findViewById(R.id.twitter_include_location_cb);
        includeLocationCb.setOnClickListener(this);
        randomizeLocationCb = (CheckBox) view.findViewById(R.id.twitter_randomize_location_cb);
        randomizeLocationCb.setOnClickListener(this);

        placeTextView = (TextView) view.findViewById(R.id.twitter_place_text_view);
        latLongTextView = (TextView) view.findViewById(R.id.twitter_lat_long_text_view);
        longTextWarningTextView = (TextView) view.findViewById(R.id.long_text_warning_text_view);

        nameTextView = (TextView) view.findViewById(R.id.twitter_name_text_view);
        handleTextView = (TextView) view.findViewById(R.id.twitter_handle_text_view);
        profilePicture = (ImageView) view.findViewById(R.id.twitter_profile_picture);

        statusEditText = (EditText) view.findViewById(R.id.twitter_status_edit_text);
        statusEditText.setOnFocusChangeListener(new StatusFocusListener());
        statusEditText.addTextChangedListener(new TwitterTextWatcher());

        loginButton = (ImageButton) view.findViewById(R.id.twitter_login_button);
        loginButton.setOnClickListener(this);
        logoutButton = (ImageButton) view.findViewById(R.id.twitter_logout_button);
        logoutButton.setOnClickListener(this);

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

    private void useBlankBitmap() {
        int blankImageResource = R.drawable.blank_profile_picture;
        profileBitmap = BitmapFactory.decodeResource(getResources(), blankImageResource);
        profilePicture.setImageBitmap(profileBitmap);
    }

    ;

    @Override
    public void onResume() {
        super.onResume();
        verifyTwitter();
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

    private void postStatusUpdate() {
        Intent updateServiceIntent = new Intent(getActivity(), StatusUpdaterService.class);
        getActivity().startService(updateServiceIntent);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_post_fragment, menu);
        boolean enablePost = isOathAuthenticated();
        postMenuItem = menu.findItem(R.id.post_status);
        if (postMenuItem != null) {
            postMenuItem.setVisible(enablePost);
        }
    }

    /**
     * Function to login twitter attached through the layout xml file
     */
    private void signOutFromTwitter() {
        editor.remove(Constants.TWITTER_OAUTH_VERIFIER);
        editor.remove(Constants.TWITTER_REQUEST_TOKEN);
        editor.remove(Constants.TWITTER_REQUEST_TOKEN_SECRET);
        editor.remove(Constants.TWITTER_ACCESS_TOKEN);
        editor.remove(Constants.TWITTER_ACCESS_TOKEN_SECRET);
        editor.remove(Constants.TWITTER_LOGGED_IN);
        editor.remove(Constants.TWITTER_INCLUDE_LOCATION);
        editor.remove(Constants.TWITTER_RANDOMIZE_LOCATION);
        editor.commit();

        useBlankBitmap();
        updateInterface();
    }

    private void signInToTwitter() {
        DownloadRequestTokenTask downloadRequestTokenTask = new DownloadRequestTokenTask();
        downloadRequestTokenTask.setRequestTokenDownloadListener(new TwitterRequestTokenListener());
        downloadRequestTokenTask.execute();
    }

    private void verifyTwitter() {
        boolean isOauthVerified = isOauthVerified();
        boolean isOauthAuthenticated = isOathAuthenticated();
        if (!isOauthAuthenticated) {
            if (isOauthVerified) {
                startDownloadingAccessToken();
            } else {
                useBlankBitmap();
                updateInterface();
            }
        } else {
            startDownloadingBitmap();
            updateInterface();
        }
    }

    private void updateInterface() {
        int visible = isOathAuthenticated() ? View.VISIBLE : View.GONE;
        includeLocationCb.setVisibility(visible);
        randomizeLocationCb.setVisibility(visible);
        logoutButton.setVisibility(visible);
        nameTextView.setVisibility(visible);
        handleTextView.setVisibility(visible);
        statusEditText.setVisibility(visible);

        int invisible = isOathAuthenticated() ? View.GONE : View.VISIBLE;
        loginButton.setVisibility(invisible);

        boolean includeLocation = preferences.getBoolean(Constants.TWITTER_INCLUDE_LOCATION, false);
        boolean randomizeLocation = preferences.getBoolean(Constants.TWITTER_RANDOMIZE_LOCATION, false);
        if (visible == View.VISIBLE) {
            includeLocationCb.setChecked(includeLocation);
            randomizeLocationCb.setChecked(randomizeLocation);
            if (includeLocation) {
                randomizeLocationCb.setEnabled(true);
            }

            String name = preferences.getString(Constants.TWITTER_NAME, Constants.EMPTY_STRING);
            String handle = "@" + preferences.getString(Constants.TWITTER_SCREEN_NAME, Constants.EMPTY_STRING);
            nameTextView.setText(name);
            handleTextView.setText(handle);
            statusEditText.setText(OgyongUtils.generateStatus(getActivity()));
        }

        int locationVisible = (isOathAuthenticated() && includeLocation) ? View.VISIBLE : View.GONE;
        placeTextView.setVisibility(locationVisible);
        latLongTextView.setVisibility(locationVisible);
        if (locationVisible == View.VISIBLE) {

        }
    }

    private boolean isOathAuthenticated() {
        return preferences.getBoolean(Constants.TWITTER_LOGGED_IN, false);
    }

    private boolean isOauthVerified() {
        String oauthVerifier = preferences.getString(Constants.TWITTER_OAUTH_VERIFIER, Constants.EMPTY_STRING);
        return !Constants.EMPTY_STRING.equals(oauthVerifier);
    }

    /**
     * Handlers implementation
     */

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.twitter_include_location_cb: {
                onIncludeLocationChecked(view);
                break;
            }
            case R.id.twitter_randomize_location_cb: {
                onRandomizeLocationChecked(view);
                break;
            }
            case R.id.twitter_login_button: {
                signInToTwitter();
                break;
            }
            case R.id.twitter_logout_button: {
                signOutFromTwitter();
                break;
            }
        }
    }

    private void onRandomizeLocationChecked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        locationSelectionListener.onRandomLocation(Constants.TWITTER_UPDATE_DESTINATION, checked);

        editor.putBoolean(Constants.TWITTER_RANDOMIZE_LOCATION, checked);
        editor.commit();
    }

    private void onIncludeLocationChecked(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        randomizeLocationCb.setEnabled(checked);

        if (!checked) {
            randomizeLocationCb.setChecked(false);
            editor.remove(Constants.TWITTER_RANDOMIZE_LOCATION);
        }

        locationSelectionListener.onIncludeLocation(Constants.TWITTER_UPDATE_DESTINATION, checked);

        int visibility = checked ? View.VISIBLE : View.GONE;
        placeTextView.setVisibility(visibility);
        latLongTextView.setVisibility(visibility);

        editor.putBoolean(Constants.TWITTER_INCLUDE_LOCATION, checked);
        editor.commit();
    }

    private void startDownloadingAccessToken() {
        String token = preferences.getString(Constants.TWITTER_REQUEST_TOKEN, Constants.EMPTY_STRING);
        String tokenSecret = preferences.getString(Constants.TWITTER_REQUEST_TOKEN_SECRET, Constants.EMPTY_STRING);
        RequestToken requestToken = new RequestToken(token, tokenSecret);

        String oauthVerifier = preferences.getString(Constants.TWITTER_OAUTH_VERIFIER, Constants.EMPTY_STRING);
        DownloadAccessTokenTask downloadAccessTokenTask = new DownloadAccessTokenTask(requestToken, oauthVerifier);
        downloadAccessTokenTask.setDownloadAccessTokenListener(new TwitterAccessTokenListener());
        downloadAccessTokenTask.execute();
    }

    private void startDownloadingUser() {
        // start downloading user information
        String token = preferences.getString(Constants.TWITTER_ACCESS_TOKEN, Constants.EMPTY_STRING);
        String tokenSecret = preferences.getString(Constants.TWITTER_ACCESS_TOKEN_SECRET, Constants.EMPTY_STRING);

        AccessToken accessToken = new AccessToken(token, tokenSecret);
        DownloadUserTask downloadUserTask = new DownloadUserTask(accessToken);
        downloadUserTask.setUserDownloadListener(new TwitterUserDetailListener());
        downloadUserTask.execute();
    }

    private void startDownloadingBitmap() {
        String profileImageUrl = preferences.getString(Constants.TWITTER_PROFILE_PICTURE, Constants.EMPTY_STRING);
        DownloadBitmapTask downloadBitmapTask = new DownloadBitmapTask(profileImageUrl);
        downloadBitmapTask.setBitmapDownloadListener(new TwitterBitmapListener());
        downloadBitmapTask.execute();
    }

    public void setPlaceTextView(String place) {
        placeTextView.setText(place);
    }

    public void setLatLongTextView(String latLong) {
        latLongTextView.setText(latLong);
    }

    public void setStatusMessage(String statusMessage) {
        statusEditText.setText(statusMessage);
    }

    private class TwitterTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (longTextWarningTextView != null) {
                if (start + count > 140) {
                    longTextWarningTextView.setVisibility(View.VISIBLE);
                } else {
                    longTextWarningTextView.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
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

    private class TwitterBitmapListener implements UserBitmapListener {

        @Override
        public void onBitmapDownloaded(Bitmap bitmap) {
            if (bitmap != null) {
                profileBitmap = bitmap;
                profilePicture.setImageBitmap(profileBitmap);
            }
        }
    }

    private class TwitterRequestTokenListener implements RequestTokenListener {
        @Override
        public void onRequestTokenDownloaded(RequestToken requestToken) {
            // start the activity to auth with twitter
            if (requestToken != null) {
                editor.putString(Constants.TWITTER_REQUEST_TOKEN, requestToken.getToken());
                editor.putString(Constants.TWITTER_REQUEST_TOKEN_SECRET, requestToken.getTokenSecret());
                editor.commit();
            }
            getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
        }
    }

    private class TwitterAccessTokenListener implements AccessTokenListener {

        @Override
        public void onAccessTokenDownloaded(AccessToken accessToken) {
            // save the access token for future use
            if (accessToken != null) {
                editor.putString(Constants.TWITTER_ACCESS_TOKEN, accessToken.getToken());
                editor.putString(Constants.TWITTER_ACCESS_TOKEN_SECRET, accessToken.getTokenSecret());
                editor.commit();
            }
            startDownloadingUser();
        }
    }

    private class TwitterUserDetailListener implements UserDetailListener {

        @Override
        public void onUserDownloaded(User user) {
            // save the user information for future use
            if (user != null) {
                editor.putString(Constants.TWITTER_NAME, user.getName());
                editor.putString(Constants.TWITTER_SCREEN_NAME, user.getScreenName());
                editor.putString(Constants.TWITTER_PROFILE_PICTURE, user.getOriginalProfileImageURL());
                editor.putBoolean(Constants.TWITTER_LOGGED_IN, true);
                editor.commit();
            }
            startDownloadingBitmap();
            updateInterface();
        }
    }
}
