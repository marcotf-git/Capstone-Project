package com.example.androidstudio.capstoneproject.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class SyncDatabaseService extends JobService {

    private AsyncTask mBackgroundTask;
    private static final String USER_UID = "userUid";
    private static final String DATABASE_VISIBILITY = "databaseVisibility";


    @Override
    public boolean onStartJob(final JobParameters jobParameters) {

        Bundle bundle = jobParameters.getExtras();

        if(bundle == null) {
            return false;
        }

        final String userUid = bundle.getString(USER_UID);
        final String databaseVisibility = bundle.getString(DATABASE_VISIBILITY);

        mBackgroundTask = new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {
                Context context = SyncDatabaseService.this;
                SyncTasks.executeTask(context, SyncTasks.ACTION_SYNC_GROUP_TABLE, userUid, databaseVisibility);
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
               jobFinished(jobParameters, false);
            }
        };

        mBackgroundTask.execute();
        return true;
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mBackgroundTask != null) mBackgroundTask.cancel(true);
        return true; // retry the job as soon as possible
    }


}
