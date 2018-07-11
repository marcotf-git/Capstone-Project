package com.example.androidstudio.capstoneproject.utilities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.sync.SyncTasks;
import com.example.androidstudio.capstoneproject.ui.MainActivity;

public class NotificationUtils {

    private static final int SYNC_GROUP_NOTIFICATION_ID = 10;
    private static final int SYNC_GROUP_PENDING_INTENT_ID = 100;
    private static final String SYNC_GROUP_NOTIFICATION_CHANNEL_ID = "sync_group_notification_channel";

    private static final int UPLOAD_LESSON_NOTIFICATION_ID = 20;
    private static final int UPLOAD_LESSON_PENDING_INTENT_ID = 200;
    private static final String UPLOAD_LESSON_NOTIFICATION_CHANNEL_ID = "upload_lesson_notification_channel";

    private static final int ACTION_IGNORE_PENDING_INTENT_ID = 3000;


    public static void clearAllNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }


    public static void notifyUserBecauseSyncGroupFinished(Context context) {

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    SYNC_GROUP_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.main_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,
                SYNC_GROUP_NOTIFICATION_CHANNEL_ID)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentTitle(context.getString(R.string.sync_group_notification_title))
                .setContentText(context.getString(R.string.sync_group_notification_body))
                .setSmallIcon(R.drawable.ic_cancel)
                .setLargeIcon(largeIcon(context))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        context.getString(R.string.sync_group_notification_body)))
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(contentGroupIntent(context))
                .addAction(ignorePendingIntent(context))
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        notificationManager.notify(SYNC_GROUP_NOTIFICATION_ID, notificationBuilder.build());

    }

    private static NotificationCompat.Action ignorePendingIntent(Context context) {
        Intent ignoreReminderIntent = new Intent(context, LearningAppIntentService.class);
        ignoreReminderIntent.setAction(SyncTasks.ACTION_DISMISS_NOTIFICATION);
        PendingIntent ignoreReminderPendingIntent = PendingIntent.getService(
                context,
                ACTION_IGNORE_PENDING_INTENT_ID,
                ignoreReminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action ignoreReminderAction =
                new NotificationCompat.Action(R.drawable.ic_cancel,"OK.",
                ignoreReminderPendingIntent);
        return ignoreReminderAction;
    }



    public static void notifyUserBecauseUploadFinished(Context context) {

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    UPLOAD_LESSON_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.main_notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context,
                UPLOAD_LESSON_NOTIFICATION_CHANNEL_ID)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setContentTitle(context.getString(R.string.upload_notification_title))
                .setContentText(context.getString(R.string.upload_notification_body))
                .setSmallIcon(R.drawable.ic_cancel)
                .setLargeIcon(largeIcon(context))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        context.getString(R.string.upload_notification_body)))
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(contentLessonIntent(context))
                .addAction(ignorePendingIntent(context))
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH);
        }

        notificationManager.notify(UPLOAD_LESSON_NOTIFICATION_ID, notificationBuilder.build());
    }


    private static PendingIntent contentGroupIntent(Context context) {
        Intent startActivityIntent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(
                context,
                SYNC_GROUP_PENDING_INTENT_ID,
                startActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent contentLessonIntent(Context context) {
        Intent startActivityIntent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(
                context,
                UPLOAD_LESSON_PENDING_INTENT_ID,
                startActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Bitmap largeIcon(Context context) {
        Resources res = context.getResources();
        Bitmap largeIcon = BitmapFactory.decodeResource(res, R.drawable.ic_arrow_back);
        return largeIcon;
    }
}