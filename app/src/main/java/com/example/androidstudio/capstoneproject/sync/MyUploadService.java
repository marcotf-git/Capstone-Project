package com.example.androidstudio.capstoneproject.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;



public class MyUploadService extends IntentService {

    private static final String SELECTED_LESSON_ID = "selectedLessonId";
    private static final String LOCAL_USER_UID = "localUserUid";

    private Long lesson_id;
    private String userUid;


    public MyUploadService() {
        super("MyUploadService");
    }

    @Override
    public void onCreate() {

        super.onCreate();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        // Recover information from caller activity
        if (intent != null && intent.hasExtra(SELECTED_LESSON_ID)) {
            lesson_id = intent.getLongExtra(SELECTED_LESSON_ID, -1);
        }

        if (intent != null && intent.hasExtra(LOCAL_USER_UID)) {
            userUid = intent.getStringExtra(LOCAL_USER_UID);
        }

        if (lesson_id!= null && userUid != null) {
            MyUpload myUpload = new MyUpload(this, userUid, lesson_id);
            myUpload.uploadImagesAndDatabase();
        }

    }


}
