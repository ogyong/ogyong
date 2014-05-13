package com.nribeka.ogyong.listener;

import twitter4j.User;

/**
 */
public interface UserDownloadListener {
    void onUserDownloaded(User user);
}
