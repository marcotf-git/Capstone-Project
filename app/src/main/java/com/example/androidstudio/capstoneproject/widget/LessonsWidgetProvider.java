package com.example.androidstudio.capstoneproject.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RemoteViews;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.ui.MainActivity;

/**
 * Implementation of App Widget functionality (the receiver)
 * Receive broadcasts when the App Widget is updated, enabled, disabled and deleted.
 */
public class LessonsWidgetProvider extends AppWidgetProvider {

    private static final String TAG = LessonsWidgetProvider.class.getSimpleName();


    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId) {

        Log.d(TAG, "updateAppWidget ");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_lessons);
        //views.setTextViewText(R.id.tv_widget_lesson_name);

        // Set the widget title to launch the MainActivity
        Intent appMainIntent = new Intent(context, MainActivity.class);
        PendingIntent appMainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appMainIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.tv_widget_title, appMainPendingIntent);

        // Set up the collection
        setRemoteAdapter(context, views);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled

    }

    /**
     * Sets the remote adapter used to fill in the list items
     *
     * @param views RemoteViews to set the RemoteAdapter
     */
    private static void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
        views.setRemoteAdapter(R.id.widget_list,
                new Intent(context, ListWidgetService.class));
    }


    public static void updateLessonsWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

}

