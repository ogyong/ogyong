package com.nribeka.ogyong.task;

import android.os.AsyncTask;
import android.util.Log;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.listener.RequestTokenListener;
import com.nribeka.ogyong.utils.OgyongUtils;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.RequestToken;

/**
 */
public class DownloadRequestTokenTask extends AsyncTask<Void, Void, RequestToken> {

    private static final String TAG = DownloadRequestTokenTask.class.getSimpleName();

    private RequestTokenListener listener;

    public void setRequestTokenDownloadListener(RequestTokenListener listener) {
        this.listener = listener;
    }

    @Override
    protected RequestToken doInBackground(Void... params) {
        RequestToken requestToken = null;
        Twitter twitter = OgyongUtils.getTwitterInstance();
        try {
            requestToken = twitter.getOAuthRequestToken(Constants.TWITTER_CALLBACK);
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
