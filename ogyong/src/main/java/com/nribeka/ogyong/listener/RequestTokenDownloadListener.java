package com.nribeka.ogyong.listener;

import twitter4j.auth.RequestToken;

/**
 */
public interface RequestTokenDownloadListener {
    void onRequestTokenDownloaded(RequestToken requestToken);
}
