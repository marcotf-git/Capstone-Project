package com.example.androidstudio.capstoneproject.sync;

import android.content.Context;
import android.util.Log;

import com.example.androidstudio.capstoneproject.utilities.MyFirebaseFragment;
import com.example.androidstudio.capstoneproject.utilities.NotificationUtils;


public class SyncTasks {

    private static final String TAG = SyncTasks.class.getSimpleName();
    public static final String ACTION_SYNC_GROUP_TABLE = "sync-group-database";
    public static final String ACTION_UPLOAD_LESSON = "upload-lesson";
    public static final String ACTION_DISMISS_NOTIFICATION = "dismiss_notification";


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


    synchronized private static void syncDatabase(Context context, String userUid, String databaseVisibility) {
        Log.v(TAG, "syncDatabase: syncing group table");


        try {
            MyFirebaseFragment firebaseFragment = new MyFirebaseFragment();
            firebaseFragment.setFirebase(userUid);
            firebaseFragment.refreshDatabase(databaseVisibility);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        // notify the user that the task (synchronized) has finished
        NotificationUtils.notifyUserBecauseSyncGroupFinished(context);
    }


    synchronized private static void uploadLesson(Context context, String userUid, Long lesson_id) {
        if (lesson_id == null) {
            Log.v(TAG, "uploadLesson: no lesson selected!");
            return;
        }
        Log.v(TAG, "uploadLesson: uploading lesson id:" + lesson_id);

        try {
            MyFirebaseFragment firebaseFragment = new MyFirebaseFragment();
            firebaseFragment.setFirebase(userUid);
            firebaseFragment.uploadImagesAndDatabase(lesson_id);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        // notify the user that the task (synchronized) has finished
        NotificationUtils.notifyUserBecauseUploadFinished(context);
    }



}
