package com.nribeka.ogyong.listener;

import twitter4j.auth.RequestToken;

/**
 */
public interface RequestTokenListener {
    void onRequestTokenDownloaded(RequestToken requestToken);
}
