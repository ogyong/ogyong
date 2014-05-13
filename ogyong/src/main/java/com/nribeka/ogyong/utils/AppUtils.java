package com.nribeka.ogyong.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 */
public class AppUtils {

    private static final String TAG = AppUtils.class.getSimpleName();
    private static final String NOT_AVAILABLE = "N/A";
    private static final String DEFAULT_FORMAT = "Now playing '@track' \n\r by '@artist' \n\r from album '@album'";

    public static Twitter getTwitterInstance() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(AppConstants.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(AppConstants.TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        return factory.getInstance();
    }

    public static String generateStatus(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String album = preferences.getString(AppConstants.SP_MEDIA_ALBUM, NOT_AVAILABLE);
        String track = preferences.getString(AppConstants.SP_MEDIA_TRACK, NOT_AVAILABLE);
        String artist = preferences.getString(AppConstants.SP_MEDIA_ARTIST, NOT_AVAILABLE);

        String statusMessage = AppConstants.EMPTY_STRING;
        if (!NOT_AVAILABLE.equals(album) && !NOT_AVAILABLE.equals(track) && !NOT_AVAILABLE.equals(artist)) {
            String images = "*•♫♪჻♪♫•*¨*•♪♫ ☼ ♫ ♪჻♪♫•";
            statusMessage = preferences.getString(AppConstants.SP_STATUS_TEMPLATE, DEFAULT_FORMAT);
            statusMessage = statusMessage.replace("@artist", album);
            statusMessage = statusMessage.replace("@track", track);
            statusMessage = statusMessage.replace("@album", artist);
        }
        return statusMessage;
    }

    public static String generateHash(String artist, String album, String track) {
        String mediaInformation = artist + ":" + album + ":" + track;
        return generateHash(mediaInformation);
    }

    public static String generateHash(String input) {
        StringBuilder hashValue = new StringBuilder();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(input.getBytes());
            byte[] digest = messageDigest.digest();
            for (int i = 0; i < digest.length; i++) {
                if ((0xff & digest[i]) < 0x10) {
                    hashValue.append("0" + Integer.toHexString((0xFF & digest[i])));
                } else {
                    hashValue.append(Integer.toHexString(0xFF & digest[i]));
                }
            }
        } catch (NoSuchAlgorithmException e) {
            hashValue.append(String.valueOf(input.hashCode()));
            Log.e(TAG, "Unable to find algorithm to generate hash code data.", e);
        }
        return hashValue.toString();
    }
}
