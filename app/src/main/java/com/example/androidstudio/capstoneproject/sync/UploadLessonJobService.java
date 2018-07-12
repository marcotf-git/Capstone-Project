package com.example.androidstudio.capstoneproject.sync;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

public class UploadLessonJobService extends JobService {

    private AsyncTask mBackgroundTask;
    private static final String SELECTED_LESSON_ID = "selectedLessonId";
    private static final String USER_UID = "userUid";


    @Override
    public boolean onStartJob(final JobParameters jobParameters) {

        Bundle bundle = jobParameters.getExtras();

        if(bundle == null) {
            return false;
        }

        final long lesson_id = bundle.getLong(SELECTED_LESSON_ID);
        final String userUid = bundle.getString(USER_UID);

        mBackgroundTask = new AsyncTask() {

            @Override
            protected Object doInBackground(Object[] params) {

                Context context = UploadLessonJobService.this;
                SyncTasks.executeTask(context, SyncTasks.ACTION_UPLOAD_LESSON, userUid, lesson_id);
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
