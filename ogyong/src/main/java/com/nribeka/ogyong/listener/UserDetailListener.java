package com.nribeka.ogyong.listener;

import twitter4j.User;

/**
 */
public interface UserDetailListener {
    void onUserDownloaded(User user);
}
