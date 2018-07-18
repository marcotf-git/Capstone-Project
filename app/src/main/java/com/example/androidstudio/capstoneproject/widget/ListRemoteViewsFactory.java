package com.example.androidstudio.capstoneproject.widget;

import android.content.Context;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.androidstudio.capstoneproject.data.Lesson;

import java.util.ArrayList;
import java.util.List;

/**
 * ListRemoteViewsFactory acts as the adapter for the collection view widget,
 * providing RemoteViews to the widget in the getViewAt method.
 */
public class ListRemoteViewsFactory implements
        RemoteViewsService.RemoteViewsFactory {

    private static final String TAG = ListRemoteViewsFactory.class.getSimpleName();

    private static final List<String> mCollection = new ArrayList<>();

    private final Context mContext;


    ListRemoteViewsFactory(Context context) {
        mContext = context;
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate  mCollection: " + mCollection.toString());
    }

    @Override
    public void onDataSetChanged() {
        Log.v(TAG, "onDataSetChanged  mCollection: " + mCollection.toString());
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return mCollection.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {

        RemoteViews view = new RemoteViews(mContext.getPackageName(),
                android.R.layout.simple_list_item_1);
        view.setTextViewText(android.R.id.text1, mCollection.get(position));

        return view;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        // Treat all items in the ListView the same
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }



    public static void setWidgetProviderData(List<Lesson> lessons) {

        mCollection.clear();

        if (lessons != null) {
            for (int i = 0; i < lessons.size(); i++) {
                Lesson lesson = lessons.get(i);
                mCollection.add(lesson.getLesson_title());
            }
        }

        Log.d(TAG, "setWidgetProviderData mCollection:" + mCollection.toString());
    }

}
