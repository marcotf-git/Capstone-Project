package com.example.androidstudio.capstoneproject.sync;

import android.content.Context;
import android.util.Log;



// called by ScheduledDownloadService and ScheduledUploadService
public class ScheduledTasks {

    private static final String TAG = ScheduledTasks.class.getSimpleName();
    public static final String ACTION_SYNC_GROUP_TABLE = "sync-group-database";
    public static final String ACTION_UPLOAD_LESSON = "upload-lesson";


    public static void executeTask(Context context, String action, String userUid, Long lesson_id) {
        if (ACTION_UPLOAD_LESSON.equals(action)) {
            uploadLesson(context, userUid, lesson_id);
        }
    }

    public static void executeTask(Context context, String action, String userUid, String databaseVisibility) {
        if(ACTION_SYNC_GROUP_TABLE.equals(action)) {
            syncDatabase(context, userUid, databaseVisibility);
        }
    }


    synchronized private static void syncDatabase(Context context, String userUid, String databaseVisibility) {
        Log.d(TAG, "syncDatabase: syncing group table");

        try {
            MyDownload myDownload = new MyDownload(context, databaseVisibility, userUid);
            myDownload.refreshDatabase();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }


    synchronized private static void uploadLesson(Context context, String userUid, Long lesson_id) {
        Log.d(TAG, "uploadLesson: uploading lesson id:" + lesson_id);

        try {
            MyUpload myUpload = new MyUpload(context, userUid, lesson_id);
            myUpload.uploadImagesAndDatabase();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }



}
