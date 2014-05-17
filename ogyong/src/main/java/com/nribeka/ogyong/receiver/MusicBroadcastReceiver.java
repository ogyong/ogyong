package com.nribeka.ogyong.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.nribeka.ogyong.Constants;
import com.nribeka.ogyong.service.StatusUpdaterService;
import com.nribeka.ogyong.utils.OgyongUtils;

/**
 */
public class MusicBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = MusicBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Executing receiver ...");

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean automatePosting = preferences.getBoolean("automate_status_update", false);
        boolean isPlaying = intent.getBooleanExtra("playing", false);
        if (isPlaying) {
            String artist = intent.getStringExtra("artist");
            String album = intent.getStringExtra("album");
            String track = intent.getStringExtra("track");
            String generatedHash = OgyongUtils.generateHash(artist, album, track);

            String savedHash = preferences.getString(Constants.MEDIA_SIGNATURE, Constants.EMPTY_STRING);
            if (!generatedHash.equals(savedHash)) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(Constants.MEDIA_TRACK, track);
                editor.putString(Constants.MEDIA_ALBUM, album);
                editor.putString(Constants.MEDIA_ARTIST, artist);
                editor.putString(Constants.MEDIA_SIGNATURE, generatedHash);
                editor.commit();

                Intent updateStatusIntent = new Intent(Constants.INTENT_MUSIC_UPDATED);
                context.sendBroadcast(updateStatusIntent);

                if (automatePosting) {
                    Intent updateServiceIntent = new Intent(context, StatusUpdaterService.class);
                    context.startService(updateServiceIntent);
                }
            }
        }
    }
}
