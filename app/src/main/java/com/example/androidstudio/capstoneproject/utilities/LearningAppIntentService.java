package com.example.androidstudio.capstoneproject.utilities;

import android.app.IntentService;
import android.content.Intent;

import com.example.androidstudio.capstoneproject.sync.ScheduledTasks;


public class LearningAppIntentService extends IntentService {

    public LearningAppIntentService() {
        super("LearningAppIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        ScheduledTasks.executeTask(this, action);
    }
}
