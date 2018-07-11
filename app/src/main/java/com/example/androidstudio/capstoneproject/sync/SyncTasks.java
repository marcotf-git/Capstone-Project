package com.example.androidstudio.capstoneproject.sync;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.androidstudio.capstoneproject.utilities.NotificationUtils;


public class SyncTasks {

    private static final String TAG = SyncTasks.class.getSimpleName();
    public static final String ACTION_SYNC_GROUP_TABLE = "sync-group-database";
    public static final String ACTION_UPLOAD_LESSON = "upload-lesson";
    public static final String ACTION_DISMISS_NOTIFICATION = "dismiss_notification";

    public static void executeTask(Context context, String action, @Nullable Long lesson_id) {
        if(ACTION_SYNC_GROUP_TABLE.equals(action)) {
            syncGroupTable(context);
        } else if (ACTION_UPLOAD_LESSON.equals(action)) {
            uploadLesson(context, lesson_id);
        } else if (ACTION_DISMISS_NOTIFICATION.equals(action)) {
            NotificationUtils.clearAllNotifications(context);
        }
    }


    private static void syncGroupTable(Context context) {
        Log.v(TAG, "syncGroupTable: syncing group table");
        NotificationUtils.notifyUserBecauseSyncGroupFinished(context);
    }

    private static void uploadLesson(Context context, Long lesson_id) {
        if (lesson_id == null) {
            Log.v(TAG, "uploadLesson: no lesson selected!");
            return;
        }
        Log.v(TAG, "uploadLesson: uploading lesson id:" + lesson_id);
        NotificationUtils.notifyUserBecauseUploadFinished(context);
    }



}
