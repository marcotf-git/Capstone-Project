package com.example.androidstudio.capstoneproject.sync;

import android.content.Context;
import android.os.AsyncTask;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class SyncGroupTableJobService extends JobService {

    private AsyncTask mBackgroundTask;


    @Override
    public boolean onStartJob(final JobParameters jobParameters) {

        mBackgroundTask = new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {
                Context context = SyncGroupTableJobService.this;
                SyncTasks.executeTask(context, SyncTasks.ACTION_SYNC_GROUP_TABLE, null);
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
