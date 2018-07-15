package com.example.androidstudio.capstoneproject.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;



public class MyDownloadService extends IntentService {

    private static final String DATABASE_VISIBILITY = "databaseVisibility";
    private static final String LOCAL_USER_UID = "localUserUid";

    private String databaseVisibility;
    private String userUid;


    public MyDownloadService() {
        super("MyDownloadService");
    }

    @Override
    public void onCreate() {

        super.onCreate();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        // Recover information from caller activity
        if (intent != null && intent.hasExtra(DATABASE_VISIBILITY)) {
            databaseVisibility = intent.getStringExtra(DATABASE_VISIBILITY);
        }

        if (intent != null && intent.hasExtra(LOCAL_USER_UID)) {
            userUid = intent.getStringExtra(LOCAL_USER_UID);
        }

        if (databaseVisibility != null && userUid != null) {
            MyDownload myDownload = new MyDownload(this, databaseVisibility, userUid);
            myDownload.refreshDatabase();
        }

    }

}
