package com.example.androidstudio.capstoneproject.sync;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;


public class ScheduledUtilities {

    private static final String TAG = ScheduledUtilities.class.getSimpleName();

    public static final int SYNC_INTERVAL_SECONDS = 60;

    private static final String SYNC_GROUP_TABLE_TAG = "sync_group_table_tag";
    private static final String UPLOAD_LESSON_TAG = "upload_lesson_tag";
    private static final String SELECTED_LESSON_ID = "selectedLessonId";
    private static final String USER_UID = "userUid";
    private static final String DATABASE_VISIBILITY = "databaseVisibility";

    private static boolean syncInitialized;
    private static boolean uploadInitialized;


    synchronized public static void scheduleDownloadDatabase(final Context context,
                                                             String userUid,
                                                             String databaseVisibility) {

        if (syncInitialized) {
            Log.v(TAG, "scheduleUploadLesson syncInitialized = true");
        }

        Bundle bundle = new Bundle();
        bundle.putString(USER_UID, userUid);
        bundle.putString(DATABASE_VISIBILITY, databaseVisibility);

        // context parameter is for the Driver
        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        Log.v("ScheduledUtilities", "scheduleDownloadDatabase scheduling");

        /* Create the Job to periodically create reminders to drink water */
        Job syncGroupTableJob = dispatcher.newJobBuilder()
                /* The Service that will be used to write to preferences */
                .setService(ScheduledDownloadService.class)
                /*
                 * Set the UNIQUE tag used to identify this Job.
                 */
                .setTag(SYNC_GROUP_TABLE_TAG)
                /*
                 * Network constraints on which this Job should run. In this app, we're using the
                 * device charging constraint so that the job only executes if the device is
                 * charging.
                 *
                 * In a normal app, it might be a good idea to include a preference for this,
                 * as different users may have different preferences on when you should be
                 * syncing your application's data.
                 */
                .setConstraints(Constraint.ON_UNMETERED_NETWORK)
                /*
                 * setLifetime sets how long this job should persist. The options are to keep the
                 * Job "forever" or to have it die the next time the device boots up.
                 */
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                /*
                 * We want these reminders to continuously happen, so we tell this Job to recur.
                 */
                .setRecurring(false)
                /*
                 * We want the reminders to happen every 15 minutes or so. The first argument for
                 * Trigger class's static executionWindow method is the start of the time frame
                 * when the
                 * job should be performed. The second argument is the latest point in time at
                 * which the data should be synced. Please note that this end time is not
                 * guaranteed, but is more of a guideline for FirebaseJobDispatcher to go off of.
                 */
                .setTrigger(Trigger.executionWindow(
                        0,
                        SYNC_INTERVAL_SECONDS))
                /*
                 * If a Job with the tag with provided already exists, this new job will replace
                 * the old one.
                 */
                .setReplaceCurrent(true)
                .setExtras(bundle)
                /* Once the Job is ready, call the builder's build method to return the Job */
                .build();

        /* Schedule the Job with the dispatcher */
        dispatcher.schedule(syncGroupTableJob);

        syncInitialized = true;

    }



    synchronized public static void scheduleUploadLesson(final Context context, String userUid, long lesson_id) {

        if (uploadInitialized) {
            Log.v(TAG, "scheduleUploadLesson uploadInitialized = true");
        }

        Bundle bundle = new Bundle();

        bundle.putString(USER_UID, userUid);
        bundle.putLong(SELECTED_LESSON_ID, lesson_id);

        // context parameter is for the Driver
        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);

        Log.v("ScheduledUtilities", "scheduleDownloadDatabase scheduling");

        /* Create the Job to periodically create reminders to drink water */
        Job uploadTableJob = dispatcher.newJobBuilder()
                /* The Service that will be used to write to preferences */
                .setService(ScheduledUploadService.class)
                /*
                 * Set the UNIQUE tag used to identify this Job.
                 */
                .setTag(UPLOAD_LESSON_TAG)
                /*
                 * Network constraints on which this Job should run. In this app, we're using the
                 * device charging constraint so that the job only executes if the device is
                 * charging.
                 *
                 * In a normal app, it might be a good idea to include a preference for this,
                 * as different users may have different preferences on when you should be
                 * syncing your application's data.
                 */
                .setConstraints(Constraint.ON_UNMETERED_NETWORK)
                /*
                 * setLifetime sets how long this job should persist. The options are to keep the
                 * Job "forever" or to have it die the next time the device boots up.
                 */
                .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                /*
                 * We want these reminders to continuously happen, so we tell this Job to recur.
                 */
                .setRecurring(false)
                /*
                 * We want the reminders to happen every 15 minutes or so. The first argument for
                 * Trigger class's static executionWindow method is the start of the time frame
                 * when the
                 * job should be performed. The second argument is the latest point in time at
                 * which the data should be synced. Please note that this end time is not
                 * guaranteed, but is more of a guideline for FirebaseJobDispatcher to go off of.
                 */
                .setTrigger(Trigger.executionWindow(
                        0,
                        SYNC_INTERVAL_SECONDS))
                /*
                 * If a Job with the tag with provided already exists, this new job will replace
                 * the old one.
                 */
                .setReplaceCurrent(true)
                .setExtras(bundle)
                /* Once the Job is ready, call the builder's build method to return the Job */
                .build();

        /* Schedule the Job with the dispatcher */
        dispatcher.schedule(uploadTableJob);

        uploadInitialized = true;

    }


}
