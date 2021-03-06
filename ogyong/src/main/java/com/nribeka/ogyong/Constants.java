package com.nribeka.ogyong;

import android.app.AlarmManager;

/**
 */
public interface Constants {
    String TWITTER_CONSUMER_KEY = "RQ4sWgbshXlNuvhdvAaHQUavq";
    String TWITTER_CONSUMER_SECRET = "XZ04eK6iNp6IZBQwPkgximLLGobWzOGKsFkloNGPWC3S3K7rBm";
    String TWITTER_CALLBACK = "oauth://oauthUri";

    int ZERO = 0;
    String BLANK = "";
    String PLACE_UNKNOWN = "-No place info-";

    String TWITTER_NAME = "twitter.name";
    String TWITTER_SCREEN_NAME = "twitter.screen_name";
    String TWITTER_PROFILE_PICTURE = "twitter.profile_picture";

    String TWITTER_LOGGED_IN = "twitter.logged_in";
    String TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    String TWITTER_REQUEST_TOKEN = "twitter.request_token";
    String TWITTER_REQUEST_TOKEN_SECRET = "twitter.request_token_secret";
    String TWITTER_ACCESS_TOKEN = "twitter.access_token";
    String TWITTER_ACCESS_TOKEN_SECRET = "twitter.access_token_secret";

    String MEDIA_ID = "media.id";
    String MEDIA_TRACK = "media.track";
    String MEDIA_ALBUM = "media.album";
    String MEDIA_ARTIST = "media.artist";
    String MEDIA_SIGNATURE = "media.signature";

    String STATUS_TEMPLATE = "status_template";

    String TWITTER_INCLUDE_LOCATION = "twitter.include_location";
    String TWITTER_RANDOMIZE_LOCATION = "twitter.randomize_location";

    String FACEBOOK_INCLUDE_LOCATION = "facebook.include_location";
    String FACEBOOK_RANDOMIZE_LOCATION = "facebook.randomize_location";

    String RUN_ONCE = "app.run_once";
    boolean DEVELOPER_MODE = true;

    int LOCATION_DEFAULT_RADIUS = 100;
    int LOCATION_MAX_DISTANCE = LOCATION_DEFAULT_RADIUS / 2;
    int LOCATION_PASSIVE_MAX_DISTANCE = LOCATION_MAX_DISTANCE * 4;
    long LOCATION_MAX_TIME = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    long LOCATION_PASSIVE_MAX_TIME = LOCATION_MAX_TIME * 4;

    String LAST_UPDATED_TIME = "location.last_updated_time";
    String LAST_UPDATED_LATITUDE = "location.last_updated_latitude";
    String LAST_UPDATED_LONGITUDE = "location.last_updated_longitude";
    String CONSTRUCTED_LOCATION_PROVIDER = "location_provider.constructed";

    String INTENT_MUSIC_UPDATED = "com.nribeka.ogyong.INTENT_MUSIC_UPDATED";
    String INTENT_SINGLE_LOCATION_UPDATE = "com.nribeka.ogyong.INTENT_SINGLE_LOCATION_UPDATE";

    String INTENT_EXTRA_RADIUS = "intent_extra.radius";
    String INTENT_EXTRA_LOCATION = "intent_extra.location";
    String INTENT_EXTRA_ERROR_MESSAGE = "intent_extra.error_message";
    String INTENT_EXTRA_FORCE_REFRESH = "intent_extra.force_refresh";
    String INTENT_EXTRA_MESSAGE_DESTINATION = "intent_extra.message_destination";
    String INTENT_EXTRA_UPDATE_DESTINATION = "intent_extra.update_destination";

    String APP_IN_BACKGROUND = "app.in_background";
    boolean USE_GPS_WHEN_ACTIVITY_VISIBLE = true;
    boolean DISABLE_PASSIVE_LOCATION_WHEN_USER_EXIT = true;

    String TWITTER_LOCATION_COUNT = "twitter.location_count";
    String TWITTER_LOCATION_HASHES = "twitter.location_hashes";
    String FACEBOOK_LOCATION_COUNT = "facebook.location_count";
    String FACEBOOK_LOCATION_HASHES = "facebook.location_hashes";

    int TWITTER_UPDATE_DESTINATION = 2;
    int FACEBOOK_UPDATE_DESTINATION = 1;
    int UNSPECIFIED_UPDATE_DESTINATION = 999;

    String INTENT_STATUS_UPDATED = "com.nribeka.ogyong.INTENT_STATUS_UPDATED";
    String INTENT_EXTRA_STATUS_MESSAGE = "com.nribeka.ogyong.INTENT_STATUS_MESSAGE";
    String INTENT_STATUS_UPDATE_FAILED = "com.nribeka.ogyong.INTENT_STATUS_UPDATE_FAILED";
    String INTENT_TWITTER_LOCATION_UPDATED = "com.nribeka.ogyong.INTENT_TWITTER_LOCATION_UPDATED";
    String INTENT_FACEBOOK_LOCATION_UPDATED = "com.nribeka.ogyong.INTENT_FACEBOOK_LOCATION_UPDATED";

    String TWITTER_LATITUDE = "twitter.latitude";
    String TWITTER_LONGITUDE = "twitter.longitude";
    String FACEBOOK_LATITUDE = "facebook.latitude";
    String FACEBOOK_LONGITUDE = "facebook.longitude";
}
