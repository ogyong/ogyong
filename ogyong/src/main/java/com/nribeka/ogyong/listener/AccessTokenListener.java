package com.nribeka.ogyong.listener;

import twitter4j.auth.AccessToken;

/**
 */
public interface AccessTokenListener {
    void onAccessTokenDownloaded(AccessToken accessToken);
}
