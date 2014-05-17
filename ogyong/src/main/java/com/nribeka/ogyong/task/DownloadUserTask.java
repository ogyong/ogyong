package com.nribeka.ogyong.task;

import android.os.AsyncTask;
import android.util.Log;

import com.nribeka.ogyong.listener.UserDetailListener;
import com.nribeka.ogyong.utils.OgyongUtils;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;
import twitter4j.auth.AccessToken;

/**
 */
public class DownloadUserTask extends AsyncTask<Void, Void, User> {

    private static final String TAG = DownloadUserTask.class.getSimpleName();

    private AccessToken accessToken;
    private UserDetailListener listener;

    public DownloadUserTask(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    public void setUserDownloadListener(UserDetailListener listener) {
        this.listener = listener;
    }

    @Override
    protected User doInBackground(Void... params) {
        User user = null;
        Twitter twitter = OgyongUtils.getTwitterInstance();
        try {
            twitter.setOAuthAccessToken(accessToken);
            user = twitter.showUser(twitter.getId());
        } catch (TwitterException e) {
            Log.e(TAG, "Unable to retrieve user data from twitter.", e);
        }
        return user;
    }

    @Override
    protected void onPostExecute(User user) {
        super.onPostExecute(user);
        listener.onUserDownloaded(user);
    }
}
