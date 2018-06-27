package com.example.androidstudio.capstoneproject.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.androidstudio.capstoneproject.R;

/**
 * The app has two modes: 'view' mode and 'create' mode.
 * If the user select 'create' mode, it will show only the content created by the user,
 * plus the options to create, edit and delete, in the action items of the app bar.
 * If the user shows 'view' mode, it will show the content that is synced with an remote server.
 * This class will start showing the lessons' titles queried from a local database, which is a copy
 * of the remote database. This corresponds to the 'view' mode, which can be selected in the
 * overflow menu.
 * According to the modes of the app, the local database has two tables.
 * The table "group_content" is a copy of the remote database.
 * The table "my_content" is the content created by the user.
 * The database and its tables are handled by a content provider LessonsContentProvider.
 * The provider is queried by a cursor loader, which returns a cursor object.
 * In the 'view' mode, the view layout will be the activity_main.xml, which has a RecyclerView,
 * populated with a GridLayoutManager and a custom adapter LessonsCursorAdapter.
 * The cursor provided by the loader is passed to the adapter with the data that will be shown.
 * In the 'view' mode, if the user shows the option to sync the database, it will call a task ....
 * In the 'create' mode, if the user selects the option to add a lesson title, it will open another
 * activity AddLessonActivity to add the title. If the user select to delete and then click on an item,
 * the item will be deleted after a confirmation. If the user select to edit and then click, it will
 * open an activity EditLessonActivity to edit that item.
 *
 *
 *
 *
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
