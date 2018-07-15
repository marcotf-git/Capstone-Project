package com.example.androidstudio.capstoneproject.sync;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


public class MyDownloadService extends IntentService {

    private static final String TAG = MyDownloadService.class.getSimpleName();

    public static final String ACTION =
            "com.example.androidstudio.capstoneproject.sync.MyDownload"; // use same action

    private static final String DATABASE_VISIBILITY = "databaseVisibility";
    private static final String USER_UID = "userUid";

    private String databaseVisibility;
    private String userUid;
    private MyLog myLog;


    public MyDownloadService() {
        super("MyDownloadService");
    }

    @Override
    public void onCreate() {
        myLog = new MyLog(this);
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        // Recover information from caller activity
        if (intent != null && intent.hasExtra(DATABASE_VISIBILITY)) {
            databaseVisibility = intent.getStringExtra(DATABASE_VISIBILITY);
        }

        if (intent != null && intent.hasExtra(USER_UID)) {
            userUid = intent.getStringExtra(USER_UID);
        }

        if (databaseVisibility != null && userUid != null) {

            try {
                MyDownload myDownload = new MyDownload(this, userUid, databaseVisibility);
                myDownload.refreshDatabase();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                myLog.addToLog(e.getMessage());
                sendMessages(e.getMessage());
                sendMessages("REFRESH USER FINISHED WITH ERROR");
            }
        }

    }

    private void sendMessages(String message) {

        Log.d(TAG, "sendMessages messages:" + message);

        Intent in = new Intent(ACTION);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("resultValue", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(in);

    }

}
