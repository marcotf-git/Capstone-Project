package com.example.androidstudio.capstoneproject.data;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * This class will store the database contract schema names
 */
public class LessonsContract {

    // The authority, which is how your code knows which Content Provider to access
    public static final String AUTHORITY = "com.example.androidstudio.capstoneproject";

    // The base content URI = "content://" + <authority>
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // Define the possible paths for accessing data in this contract
    // This is the path for the "my_lessons" directory
    public static final String PATH_MY_LESSONS = "my_lessons";

    // Empty constructor
    private LessonsContract() {}

    public static class MyLessonsEntry implements BaseColumns {

        // my_lessons table entry content URI = base content URI + path
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MY_LESSONS).build();

        // table and column names
        public static final String TABLE_NAME = "my_lessons";

        /* Since MyContentEntry implements the interface "BaseColumns", it has an automatically produced
         * "_ID" column in addition to the column "id" below. The "id" below is the global id of the
         * lesson, retrieved form the remote database. This will uniquely identify the lesson.
         */
        //public static final String COLUMN_LESSON_ID = "id";

        public static final String COLUMN_LESSON_NAME = "name";

    }


}
