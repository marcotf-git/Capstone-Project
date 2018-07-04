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
    // This is the path for the "my_lesson_parts" directory
    public static final String PATH_MY_LESSON_PARTS = "my_lesson_parts";
    // new path for all parts by its lesson_id
    public static final String MY_LESSON_PARTS_BY_LESSON_ID = "my_lesson_parts_by_part_id";
    // This is the path for the "group_lessons" directory
    public static final String PATH_GROUP_LESSONS = "group_lessons";
    // This is the path for the "group_lesson_parts" directory
    public static final String PATH_GROUP_LESSON_PARTS = "group_lesson_parts";

    // Empty constructor
    private LessonsContract() {}

    public static class MyLessonsEntry implements BaseColumns {

        // my_lessons table entry content URI = base content URI + path
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MY_LESSONS).build();

        // table and column names
        public static final String TABLE_NAME = "my_lessons";
        /* Since MyContentEntry implements the interface "BaseColumns", it has an automatically produced
         * "_id" column in addition to the column "id" below.
         */
        //public static final String COLUMN_LESSON_ID = "id";
        public static final String COLUMN_LESSON_TITLE = "title";

    }

    public static class MyLessonPartsEntry implements BaseColumns {

        // my_lesson_parts table entry content URI = base content URI + path
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_MY_LESSON_PARTS).build();

        public static final Uri CONTENT_URI_BY_LESSON_ID =
                BASE_CONTENT_URI.buildUpon().appendPath(MY_LESSON_PARTS_BY_LESSON_ID).build();

        // table and column names
        public static final String TABLE_NAME = "my_lesson_parts";
        // column names
        public static final String COLUMN_LESSON_ID = "lesson_id";
        public static final String COLUMN_PART_TITLE = "title";
    }

    public static class GroupLessonsEntry implements BaseColumns {

        // my_lessons table entry content URI = base content URI + path
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_GROUP_LESSONS).build();

        // table and column names
        public static final String TABLE_NAME = "group_lessons";
        /* Since MyContentEntry implements the interface "BaseColumns", it has an automatically produced
         * "_id" column.
         */
        public static final String COLUMN_LESSON_ID = "lesson_id";
        public static final String COLUMN_LESSON_TITLE = "title";
        public static final String COLUMN_LESSON_TIME_STAMP = "time_stamp";
        public static final String COLUMN_USER_UID = "user_uid";

    }

    public static class GroupLessonPartsEntry implements BaseColumns {

        // my_lesson_parts table entry content URI = base content URI + path
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_GROUP_LESSON_PARTS).build();

        // table and column names
        public static final String TABLE_NAME = "group_lesson_parts";
        /* Since MyContentEntry implements the interface "BaseColumns", it has an automatically produced
         * "_id" column.
         */
        public static final String COLUMN_PART_ID = "part_id";
        public static final String COLUMN_LESSON_ID = "lesson_id";
        public static final String COLUMN_PART_TITLE = "title";
        public static final String COLUMN_USER_UID = "user_uid";

    }


}
