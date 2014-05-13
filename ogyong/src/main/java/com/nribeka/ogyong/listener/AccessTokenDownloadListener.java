package com.nribeka.ogyong.listener;

import twitter4j.auth.AccessToken;

/**
 */
public interface AccessTokenDownloadListener {
    void onAccessTokenDownloaded(AccessToken accessToken);
}
