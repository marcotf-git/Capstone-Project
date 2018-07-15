package com.example.androidstudio.capstoneproject.sync;

import android.app.Activity;
import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


public class MyUploadService extends IntentService {

    private static final String TAG = MyDownloadService.class.getSimpleName();

    public static final String ACTION =
            "com.example.androidstudio.capstoneproject.sync.MyUpload"; // use same action

    private static final String SELECTED_LESSON_ID = "selectedLessonId";
    private static final String USER_UID = "userUid";

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

        MyLog myLog = new MyLog(this);

        // Recover information from caller activity
        if (intent != null && intent.hasExtra(SELECTED_LESSON_ID)) {
            lesson_id = intent.getLongExtra(SELECTED_LESSON_ID, -1);
        }

        if (intent != null && intent.hasExtra(USER_UID)) {
            userUid = intent.getStringExtra(USER_UID);
        }

        if (lesson_id!= null && userUid != null) {
            try {
                MyUpload myUpload = new MyUpload(this, userUid, lesson_id);
                myUpload.uploadImagesAndDatabase();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                myLog.addToLog(e.getMessage());
                sendMessages(e.getMessage());
                sendMessages("UPLOAD LESSON FINISHED WITH ERROR");
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
