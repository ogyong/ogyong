package com.nribeka.ogyong.task;

import android.os.AsyncTask;
import android.util.Log;

import com.nribeka.ogyong.listener.RequestTokenDownloadListener;
import com.nribeka.ogyong.utils.AppConstants;
import com.nribeka.ogyong.utils.AppUtils;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.RequestToken;

/**
 */
public class DownloadRequestTokenTask extends AsyncTask<Void, Void, RequestToken> {

    private static final String TAG = DownloadRequestTokenTask.class.getSimpleName();

    private RequestTokenDownloadListener listener;

    public void setRequestTokenDownloadListener(RequestTokenDownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected RequestToken doInBackground(Void... params) {
        RequestToken requestToken = null;
        Twitter twitter = AppUtils.getTwitterInstance();
        try {
            requestToken = twitter.getOAuthRequestToken(AppConstants.TWITTER_CALLBACK);
        } catch (TwitterException e) {
            Log.e(TAG, "Unable to retrieve twitter request token to authenticate with twitter.", e);
        }
        return requestToken;
    }

    @Override
    protected void onPostExecute(RequestToken requestToken) {
        super.onPostExecute(requestToken);
        listener.onRequestTokenDownloaded(requestToken);
    }
}
