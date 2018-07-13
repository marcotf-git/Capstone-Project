package com.example.androidstudio.capstoneproject.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/**
 * ListWidgetService is the {@link RemoteViewsService} that will return our RemoteViewsFactory
 */
public class ListWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ListRemoteViewsFactory(this.getApplicationContext());
    }
}
