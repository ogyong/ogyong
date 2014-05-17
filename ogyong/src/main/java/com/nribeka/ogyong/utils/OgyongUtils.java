package com.nribeka.ogyong.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 */
public class OgyongUtils {

    private static final String TAG = OgyongUtils.class.getSimpleName();
    private static final String NOT_AVAILABLE = "N/A";
    private static final String DEFAULT_FORMAT = "Now playing '@track' \n\r by '@artist' \n\r " +
            "from album '@album'";

    public static Twitter getTwitterInstance() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY);
        builder.setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET);
        Configuration configuration = builder.build();
        TwitterFactory factory = new TwitterFactory(configuration);
        return factory.getInstance();
    }

    public static String generateStatus(final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String album = preferences.getString(Constants.MEDIA_ALBUM, NOT_AVAILABLE);
        String track = preferences.getString(Constants.MEDIA_TRACK, NOT_AVAILABLE);
        String artist = preferences.getString(Constants.MEDIA_ARTIST, NOT_AVAILABLE);

        String statusMessage = Constants.EMPTY_STRING;
        if (!NOT_AVAILABLE.equals(album)
                && !NOT_AVAILABLE.equals(track)
                && !NOT_AVAILABLE.equals(artist)) {
            String images = "*•♫♪჻♪♫•*¨*•♪♫ ☼ ♫ ♪჻♪♫•";
            statusMessage = preferences.getString(Constants.STATUS_TEMPLATE, DEFAULT_FORMAT);
            statusMessage = statusMessage.replace("@artist", album);
            statusMessage = statusMessage.replace("@track", track);
            statusMessage = statusMessage.replace("@album", artist);
        }
        return statusMessage;
    }

    public static String generateHash(final String artist, final String album, final String track) {
        String mediaInformation = artist + ":" + album + ":" + track;
        return generateHash(mediaInformation);
    }

    public static String generateHash(final String input) {
        StringBuilder hashValue = new StringBuilder();
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(input.getBytes());
            byte[] digest = messageDigest.digest();
            for (int i = 0; i < digest.length; i++) {
                if ((0xff & digest[i]) < 0x10) {
                    hashValue.append("0").append(Integer.toHexString((0xFF & digest[i])));
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

    public static Location getRandomLocation(Context context) {
        Location location;
        Random random = new Random();
        Resources resources = context.getResources();
        String[] locationKeys = resources.getStringArray(R.array.random_location_seed);
        int randomNumber = random.nextInt(locationKeys.length);

        Log.i(TAG, "Location seed key: " + locationKeys[randomNumber]);
        int randomLatId = resources.getIdentifier(
                locationKeys[randomNumber] + "_latitude", "string", context.getPackageName());
        int randomLongId = resources.getIdentifier(
                locationKeys[randomNumber] + "_longitude", "string", context.getPackageName());

        double randomLatitude = convertLatLongToDecimal(resources.getString(randomLatId));
        double randomLongitude = convertLatLongToDecimal(resources.getString(randomLongId));

        location = new Location(Constants.CONSTRUCTED_LOCATION_PROVIDER);
        location.setLatitude(randomLatitude);
        location.setLongitude(randomLongitude);
        return location;
    }

    public static double convertLatLongToDecimal(String latOrLongInfo) {
        int degreesIndex = latOrLongInfo.indexOf("_deg_");
        String degrees = latOrLongInfo.substring(0, degreesIndex);
        int minutesIndex = latOrLongInfo.indexOf("_min_");
        String minutes = latOrLongInfo.substring(degreesIndex + 5, minutesIndex);
        int directionIndex = latOrLongInfo.lastIndexOf("_");
        String direction = latOrLongInfo.substring(directionIndex + 1);
        double decimalLatLong = Double.parseDouble(degrees) + (Double.parseDouble(minutes) / 60);
        if (direction.equalsIgnoreCase("S") || direction.equalsIgnoreCase("W")) {
            decimalLatLong = -decimalLatLong;
        }
        return decimalLatLong;
    }
}
