package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;


import com.example.xyzreader.model.Article;
import com.example.xyzreader.remote.XYZService;

import java.util.ArrayList;
import java.util.List;

import retrofit.RestAdapter;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.example.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.example.xyzreader.intent.extra.REFRESHING";

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Time time = new Time();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            return;
        }

        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, true));

        // Don't even inspect the intent, we only do one thing, and that's fetch content.
        ArrayList<ContentProviderOperation> cpo = new ArrayList<>();

        Uri dirUri = ItemsContract.Items.buildDirUri();

        // Delete all items
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        try {
            RestAdapter retrofit = new RestAdapter.Builder()
                    .setEndpoint("https://dl.dropboxusercontent.com")
                    .build();

            XYZService service = retrofit.create(XYZService.class);
            List<Article> array = service.readData();
            for (int i = 0; i < array.size(); i++) {
                ContentValues values = new ContentValues();
                Article article = array.get(i);
                values.put(ItemsContract.Items.SERVER_ID, article.getId());
                values.put(ItemsContract.Items.AUTHOR, article.getAuthor());
                values.put(ItemsContract.Items.TITLE, article.getTitle());
                values.put(ItemsContract.Items.BODY, article.getBody());
                values.put(ItemsContract.Items.THUMB_URL, article.getThumb());
                values.put(ItemsContract.Items.PHOTO_URL, article.getPhoto());
                values.put(ItemsContract.Items.ASPECT_RATIO, article.getAspect_ratio());
                time.parse3339(article.getPublished_date());
                values.put(ItemsContract.Items.PUBLISHED_DATE, time.toMillis(false));
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }

            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

        } catch (RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating content.", e);
        }

        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false));
    }
}
