package com.nribeka.ogyong.task;

import android.os.AsyncTask;
import android.util.Log;

import com.nribeka.ogyong.listener.AccessTokenDownloadListener;
import com.nribeka.ogyong.utils.AppUtils;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 */
public class DownloadAccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

    private static final String TAG = DownloadAccessTokenTask.class.getSimpleName();

    private String oauthVerifier;
    private RequestToken requestToken;
    private AccessTokenDownloadListener listener;

    public DownloadAccessTokenTask(RequestToken requestToken, String oauthVerifier) {
        this.requestToken = requestToken;
        this.oauthVerifier = oauthVerifier;
    }

    public void setDownloadAccessTokenListener(AccessTokenDownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected AccessToken doInBackground(Void... params) {
        AccessToken accessToken = null;
        Twitter twitter = AppUtils.getTwitterInstance();
        try {
            accessToken = twitter.getOAuthAccessToken(requestToken, oauthVerifier);
        } catch (TwitterException e) {
            Log.e(TAG, "Unable to retrieve twitter access token to authenticate with twitter.", e);
        }
        return accessToken;
    }

    @Override
    protected void onPostExecute(AccessToken accessToken) {
        super.onPostExecute(accessToken);
        listener.onAccessTokenDownloaded(accessToken);
    }
}
