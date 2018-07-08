package com.example.androidstudio.capstoneproject.data;

import android.net.Uri;
import android.provider.BaseColumns;


public class LogContract {


        // The authority, which is how your code knows which Content Provider to access
        public static final String AUTHORITY = "com.example.androidstudio.capstoneproject";

        // The base content URI = "content://" + <authority>
        private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

        // Define the possible paths for accessing data in this contract
        // This is the path for the "my_lessons" directory
        public static final String PATH_MY_LOG = "my_log";

        // Empty constructor
        private LogContract() {}

        public static class MyLogEntry implements BaseColumns {

            // my_lessons table entry content URI = base content URI + path
            public static final Uri CONTENT_URI =
                    BASE_CONTENT_URI.buildUpon().appendPath(PATH_MY_LOG).build();

            // table and column names
            public static final String TABLE_NAME = "my_log_items";
            /* Since MyContentEntry implements the interface "BaseColumns", it has an automatically produced
             * "_id" column in addition to the column "id" below.
             */
            public static final String COLUMN_LOG_ITEM = "item";

        }
}
