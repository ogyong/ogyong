package com.nribeka.ogyong.task;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.nribeka.ogyong.listener.BitmapDownloadListener;

import java.net.URL;

/**
 */
public class DownloadBitmapTask extends AsyncTask<Void, Void, Bitmap> {

    private static final String TAG = DownloadBitmapTask.class.getSimpleName();
    private String bitmapUrl;
    private BitmapDownloadListener listener;

    public DownloadBitmapTask(String bitmapUrl) {
        this.bitmapUrl = bitmapUrl;
    }

    public void setBitmapDownloadListener(BitmapDownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Bitmap doInBackground(Void... params) {
        Bitmap bitmap = null;
        try {
            Bitmap originalBitmap = BitmapFactory.decodeStream(new URL(bitmapUrl).openStream());
            bitmap = Bitmap.createScaledBitmap(originalBitmap, 200, 200, false);
        } catch (Exception e) {
            Log.e(TAG, "Unable to download user twitter profile picture information.", e);
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);
        listener.onBitmapDownloaded(bitmap);
    }
}
