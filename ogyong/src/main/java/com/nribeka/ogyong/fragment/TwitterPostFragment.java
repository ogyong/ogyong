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
import android.util.Log;
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

import com.nribeka.ogyong.R;
import com.nribeka.ogyong.listener.AccessTokenDownloadListener;
import com.nribeka.ogyong.listener.BitmapDownloadListener;
import com.nribeka.ogyong.listener.OnLocationTrackingListener;
import com.nribeka.ogyong.listener.RequestTokenDownloadListener;
import com.nribeka.ogyong.listener.UserDownloadListener;
import com.nribeka.ogyong.service.StatusUpdaterService;
import com.nribeka.ogyong.task.DownloadAccessTokenTask;
import com.nribeka.ogyong.task.DownloadBitmapTask;
import com.nribeka.ogyong.task.DownloadRequestTokenTask;
import com.nribeka.ogyong.task.DownloadUserTask;
import com.nribeka.ogyong.utils.AppConstants;
import com.nribeka.ogyong.utils.AppUtils;

import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 */
public class TwitterPostFragment extends Fragment implements View.OnClickListener,
        RequestTokenDownloadListener, AccessTokenDownloadListener,
        BitmapDownloadListener, UserDownloadListener {

    private static final String TAG = TwitterPostFragment.class.getSimpleName();

    private static final String FRAGMENT_NAME = "twitter.fragment.name";
    private static final String FRAGMENT_DESCRIPTION = "twitter.fragment.description";

    private String fragmentName;
    private String fragmentDescription;

    private EditText twitterStatusEditText;
    private TextView twitterNameTextView;
    private TextView twitterHandleTextView;

    private TextView placeTextView;
    private TextView latLongTextView;

    private ImageButton twitterLoginButton;
    private ImageButton twitterLogoutButton;

    private CheckBox twitterIncludeLocationCb;
    private CheckBox twitterRandomizeLocationCb;

    private MenuItem postMenuItem;

    private Bitmap twitterProfileBitmap;
    private ImageView twitterProfilePicture;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private OnLocationTrackingListener onLocationTrackingListener;

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

        twitterIncludeLocationCb = (CheckBox) view.findViewById(R.id.twitter_include_location_cb);
        twitterIncludeLocationCb.setOnClickListener(this);
        twitterRandomizeLocationCb = (CheckBox) view.findViewById(R.id.twitter_randomize_location_cb);
        twitterRandomizeLocationCb.setOnClickListener(this);

        placeTextView = (TextView) view.findViewById(R.id.twitter_place_text_view);
        latLongTextView = (TextView) view.findViewById(R.id.twitter_lat_long_text_view);

        twitterNameTextView = (TextView) view.findViewById(R.id.twitter_name_text_view);
        twitterHandleTextView = (TextView) view.findViewById(R.id.twitter_handle_text_view);
        twitterProfilePicture = (ImageView) view.findViewById(R.id.twitter_profile_picture);
        twitterStatusEditText = (EditText) view.findViewById(R.id.twitter_status_edit_text);
        twitterStatusEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    postMenuItem.setVisible(true);
                } else {
                    postMenuItem.setVisible(false);
                }
            }
        });

        twitterLoginButton = (ImageButton) view.findViewById(R.id.twitter_login_button);
        twitterLoginButton.setOnClickListener(this);
        twitterLogoutButton = (ImageButton) view.findViewById(R.id.twitter_logout_button);
        twitterLogoutButton.setOnClickListener(this);

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

    private void useBlankBitmap() {
        int blankImageResource = R.drawable.blank_profile_picture;
        twitterProfileBitmap = BitmapFactory.decodeResource(getResources(), blankImageResource);
        twitterProfilePicture.setImageBitmap(twitterProfileBitmap);
    }

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
        editor.remove(AppConstants.SP_TWITTER_OAUTH_VERIFIER);
        editor.remove(AppConstants.SP_TWITTER_REQUEST_TOKEN);
        editor.remove(AppConstants.SP_TWITTER_REQUEST_TOKEN_SECRET);
        editor.remove(AppConstants.SP_TWITTER_ACCESS_TOKEN);
        editor.remove(AppConstants.SP_TWITTER_ACCESS_TOKEN_SECRET);
        editor.remove(AppConstants.SP_TWITTER_LOGGED_IN);
        editor.commit();

        useBlankBitmap();
        updateInterface();
    }

    private void signInToTwitter() {
        DownloadRequestTokenTask downloadRequestTokenTask = new DownloadRequestTokenTask();
        downloadRequestTokenTask.setRequestTokenDownloadListener(this);
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
        twitterIncludeLocationCb.setVisibility(visible);
        twitterRandomizeLocationCb.setVisibility(visible);
        twitterLogoutButton.setVisibility(visible);
        twitterNameTextView.setVisibility(visible);
        twitterHandleTextView.setVisibility(visible);
        twitterStatusEditText.setVisibility(visible);

        int invisible = isOathAuthenticated() ? View.GONE : View.VISIBLE;
        twitterLoginButton.setVisibility(invisible);

        if (visible == View.VISIBLE) {
            twitterIncludeLocationCb.setChecked(preferences.getBoolean(AppConstants.SP_TWITTER_INCLUDE_LOCATION, false));
            twitterRandomizeLocationCb.setChecked(preferences.getBoolean(AppConstants.SP_TWITTER_RANDOMIZE_LOCATION, false));
            twitterNameTextView.setText(preferences.getString(AppConstants.SP_TWITTER_NAME, AppConstants.EMPTY_STRING));
            twitterHandleTextView.setText("@" + preferences.getString(AppConstants.SP_TWITTER_SCREEN_NAME, AppConstants.EMPTY_STRING));
            twitterStatusEditText.setText(AppUtils.generateStatus(getActivity()));
        }

        boolean includeLocation = preferences.getBoolean(AppConstants.SP_TWITTER_INCLUDE_LOCATION, false);
        int locationVisible = (isOathAuthenticated() && includeLocation) ? View.VISIBLE : View.GONE;
        placeTextView.setVisibility(locationVisible);
        latLongTextView.setVisibility(locationVisible);
    }

    private boolean isOathAuthenticated() {
        return preferences.getBoolean(AppConstants.SP_TWITTER_LOGGED_IN, false);
    }

    private boolean isOauthVerified() {
        String oauthVerifier = preferences.getString(AppConstants.SP_TWITTER_OAUTH_VERIFIER, AppConstants.EMPTY_STRING);
        Log.i("OAUTH VERIFIER INFO", "Oauth verifier information: " + oauthVerifier);
        return !AppConstants.EMPTY_STRING.equals(oauthVerifier);
    }

    /**
     * Handlers implementation
     */

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.twitter_include_location_cb: {
                boolean checked = ((CheckBox) view).isChecked();
                onLocationTrackingListener.onLocationTrackingEnabled(view);
                editor.putBoolean(AppConstants.SP_TWITTER_INCLUDE_LOCATION, checked);
                editor.commit();

                int visibility = checked ? View.VISIBLE : View.GONE;
                placeTextView.setVisibility(visibility);
                latLongTextView.setVisibility(visibility);
                break;
            }
            case R.id.twitter_randomize_location_cb: {
                boolean checked = ((CheckBox) view).isChecked();
                onLocationTrackingListener.onLocationTrackingEnabled(view);
                editor.putBoolean(AppConstants.SP_TWITTER_RANDOMIZE_LOCATION, checked);
                editor.commit();
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

    public void onRequestTokenDownloaded(RequestToken requestToken) {
        // start the activity to auth with twitter
        if (requestToken != null) {
            editor.putString(AppConstants.SP_TWITTER_REQUEST_TOKEN, requestToken.getToken());
            editor.putString(AppConstants.SP_TWITTER_REQUEST_TOKEN_SECRET, requestToken.getTokenSecret());
            editor.commit();
        }
        getActivity().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())));
    }

    private void startDownloadingAccessToken() {
        String token = preferences.getString(AppConstants.SP_TWITTER_REQUEST_TOKEN, AppConstants.EMPTY_STRING);
        String tokenSecret = preferences.getString(AppConstants.SP_TWITTER_REQUEST_TOKEN_SECRET, AppConstants.EMPTY_STRING);
        RequestToken requestToken = new RequestToken(token, tokenSecret);

        String oauthVerifier = preferences.getString(AppConstants.SP_TWITTER_OAUTH_VERIFIER, AppConstants.EMPTY_STRING);
        DownloadAccessTokenTask downloadAccessTokenTask = new DownloadAccessTokenTask(requestToken, oauthVerifier);
        downloadAccessTokenTask.setDownloadAccessTokenListener(this);
        downloadAccessTokenTask.execute();
    }

    @Override
    public void onAccessTokenDownloaded(AccessToken accessToken) {
        // save the access token for future use
        if (accessToken != null) {
            editor.putString(AppConstants.SP_TWITTER_ACCESS_TOKEN, accessToken.getToken());
            editor.putString(AppConstants.SP_TWITTER_ACCESS_TOKEN_SECRET, accessToken.getTokenSecret());
            editor.commit();
        }
        startDownloadingUser();
    }

    private void startDownloadingUser() {
        // start downloading user information
        String token = preferences.getString(AppConstants.SP_TWITTER_ACCESS_TOKEN, AppConstants.EMPTY_STRING);
        String tokenSecret = preferences.getString(AppConstants.SP_TWITTER_ACCESS_TOKEN_SECRET, AppConstants.EMPTY_STRING);

        AccessToken accessToken = new AccessToken(token, tokenSecret);
        DownloadUserTask downloadUserTask = new DownloadUserTask(accessToken);
        downloadUserTask.setUserDownloadListener(this);
        downloadUserTask.execute();
    }

    @Override
    public void onUserDownloaded(User user) {
        // save the user information for future use
        if (user != null) {
            editor.putString(AppConstants.SP_TWITTER_NAME, user.getName());
            editor.putString(AppConstants.SP_TWITTER_SCREEN_NAME, user.getScreenName());
            editor.putString(AppConstants.SP_TWITTER_PROFILE_PICTURE, user.getOriginalProfileImageURL());
            editor.putBoolean(AppConstants.SP_TWITTER_LOGGED_IN, true);
            editor.commit();
        }
        startDownloadingBitmap();
        updateInterface();
    }

    private void startDownloadingBitmap() {
        String profileImageUrl = preferences.getString(AppConstants.SP_TWITTER_PROFILE_PICTURE, AppConstants.EMPTY_STRING);
        DownloadBitmapTask downloadBitmapTask = new DownloadBitmapTask(profileImageUrl);
        downloadBitmapTask.setBitmapDownloadListener(this);
        downloadBitmapTask.execute();
    }

    @Override
    public void onBitmapDownloaded(Bitmap bitmap) {
        if (bitmap != null) {
            twitterProfileBitmap = bitmap;
            twitterProfilePicture.setImageBitmap(twitterProfileBitmap);
        }
    }

    public void setPlaceTextView(String place) {
        placeTextView.setText(place);
    }

    public void setLatLongTextView(String latLong) {
        latLongTextView.setText(latLong);
    }

    public void setStatusMessage(String statusMessage) {
        twitterStatusEditText.setText(statusMessage);
    }
}
