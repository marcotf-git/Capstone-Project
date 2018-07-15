package com.example.androidstudio.capstoneproject.sync;

import android.content.Context;
import android.util.Log;

import com.example.androidstudio.capstoneproject.utilities.NotificationUtils;


// called by ScheduledDownloadService and ScheduledUploadService
public class ScheduledTasks {

    private static final String TAG = ScheduledTasks.class.getSimpleName();

    public static final String ACTION_SYNC_GROUP_TABLE = "sync-group-database";
    public static final String ACTION_UPLOAD_LESSON = "upload-lesson";
    public static final String ACTION_DISMISS_NOTIFICATION = "dismiss_notification";

    private static final int ACTION_IGNORE_PENDING_INTENT_ID = 3000;


    // Entry points
    public static void executeTask(Context context, String action) {
        if (ACTION_DISMISS_NOTIFICATION.equals(action)) {
            NotificationUtils.clearAllNotifications(context);
        }
    }

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




    // Helper methods 
    synchronized private static void syncDatabase(Context context, String userUid, String databaseVisibility) {
        Log.d(TAG, "syncDatabase: downloading group data");

        try {
            MyDownloadService myDownload = new MyDownloadService(context);
            myDownload.downloadDatabase(userUid, databaseVisibility);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }


    synchronized private static void uploadLesson(Context context, String userUid, long lesson_id) {
        Log.d(TAG, "uploadLesson: uploading lesson id:" + lesson_id);

        try {
            MyUploadService myUpload = new MyUploadService(context);
            myUpload.uploadImagesAndDatabase(userUid, lesson_id);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }


}
