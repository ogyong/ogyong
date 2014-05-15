package com.nribeka.ogyong.utils;

import android.app.AlarmManager;

/**
 */
public class AppConstants {
    public static final String TWITTER_CONSUMER_KEY = "RQ4sWgbshXlNuvhdvAaHQUavq";
    public static final String TWITTER_CONSUMER_SECRET = "XZ04eK6iNp6IZBQwPkgximLLGobWzOGKsFkloNGPWC3S3K7rBm";
    public static final String TWITTER_CALLBACK = "oauth://oauthUri";

    public static final String EMPTY_STRING = "";
    public static final String PLACE_UNKNOWN = "-No place info-";

    public static final String SP_TWITTER_NAME = "twitter.name";
    public static final String SP_TWITTER_SCREEN_NAME = "twitter.screen_name";
    public static final String SP_TWITTER_PROFILE_PICTURE = "twitter.profile_picture";

    public static final String SP_TWITTER_LOGGED_IN = "twitter.logged_in";
    public static final String SP_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    public static final String SP_TWITTER_REQUEST_TOKEN = "twitter.request_token";
    public static final String SP_TWITTER_REQUEST_TOKEN_SECRET = "twitter.request_token_secret";
    public static final String SP_TWITTER_ACCESS_TOKEN = "twitter.access_token";
    public static final String SP_TWITTER_ACCESS_TOKEN_SECRET = "twitter.access_token_secret";

    public static final String SP_MEDIA_TRACK = "media.track";
    public static final String SP_MEDIA_ALBUM = "media.album";
    public static final String SP_MEDIA_ARTIST = "media.artist";
    public static final String SP_MEDIA_SIGNATURE = "media.signature";

    public static final String SP_STATUS_TEMPLATE = "status_template";

    public static final String SP_TWITTER_INCLUDE_LOCATION = "twitter.include_location";
    public static final String SP_TWITTER_RANDOMIZE_LOCATION = "twitter.randomize_location";

    public static final String SP_FACEBOOK_INCLUDE_LOCATION = "facebook.include_location";
    public static final String SP_FACEBOOK_RANDOMIZE_LOCATION = "facebook.randomize_location";

    public static final String SP_RUN_ONCE = "app.run_once";
    public static final boolean DEVELOPER_MODE = true;

    public static final int LOCATION_DEFAULT_RADIUS = 150;
    public static final int LOCATION_MAX_DISTANCE = LOCATION_DEFAULT_RADIUS / 2;
    public static final int LOCATION_PASSIVE_MAX_DISTANCE = LOCATION_MAX_DISTANCE;
    public static final long LOCATION_MAX_TIME = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    public static final long LOCATION_PASSIVE_MAX_TIME = LOCATION_MAX_TIME;

    public static final String SP_LAST_UPDATED_TIME = "location.last_updated_time";
    public static final String SP_LAST_UPDATED_LATITUDE = "location.last_updated_latitude";
    public static final String SP_LAST_UPDATED_LONGITUDE = "location.last_updated_longitude";
    public static final String SP_FOLLOW_LOCATION_CHANGES = "location.follow_changes";
    public static final String LOCATION_PROVIDER_DUMMY = "location_provider.dummy";

    public static final String INTENT_MUSIC_UPDATED = "com.nribeka.ogyong.INTENT_MUSIC_UPDATED";
    public static final String INTENT_LOCATION_UPDATED = "com.nribeka.ogyong.INTENT_LOCATION_UPDATED";
    public static final String INTENT_STATUS_POSTED_ACTION = "com.nribeka.ogyong.INTENT_STATUS_POSTED";
    public static final String INTENT_LOCATION_PROVIDER_DISABLED = "com.nribeka.ogyong.INTENT_LOCATION_PROVIDER_DISABLED";
    public static final String SINGLE_LOCATION_UPDATE_ACTION = "com.nribeka.ogyong.SINGLE_LOCATION_UPDATE_ACTION";

    public static final String INTENT_EXTRA_RADIUS = "intent_extra.radius";
    public static final String INTENT_EXTRA_LOCATION = "intent_extra.location";
    public static final String INTENT_EXTRA_FORCE_REFRESH = "intent_extra.force_refresh";
    public static final String INTENT_EXTRA_MESSAGE_DESTINATION = "intent_extra.message_destination";

    public static final String SP_APP_IN_BACKGROUND = "app.in_background";
    public static final boolean USE_GPS_WHEN_ACTIVITY_VISIBLE = true;
    public static final boolean DISABLE_PASSIVE_LOCATION_WHEN_USER_EXIT = false;
    public static final int STATUS_NOTIFICATION = 0;

    public static final String SP_TWITTER_LOCATION_COUNT = "twitter.location_count";
    public static final String SP_TWITTER_LOCATION_HASHES = "twitter.location_hashes";
    public static final String SP_FACEBOOK_LOCATION_COUNT = "facebook.location_count";
    public static final String SP_FACEBOOK_LOCATION_HASHES = "facebook.location_hashes";
}
