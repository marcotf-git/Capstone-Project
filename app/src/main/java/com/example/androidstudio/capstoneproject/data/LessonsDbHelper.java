package com.example.androidstudio.capstoneproject.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LessonsDbHelper extends SQLiteOpenHelper {

    // The database name
    private static final String DATABASE_NAME = "lessons.db";

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 20;

    // Constructor
    public LessonsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        // Create a table to hold the lesson data of the user
        final String SQL_CREATE_MY_LESSONS_TABLE =

                "CREATE TABLE " +  LessonsContract.MyLessonsEntry.TABLE_NAME + " (" +

                /*
                 * MyLessonsEntry did not explicitly declare a column called "_id". However,
                 * MyLessonsEntry implements the interface, "BaseColumns", which does have a field
                 * named "_id". We use that here to designate our table's primary key.
                 */
                LessonsContract.MyLessonsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                //LessonsContract.MyLessonsEntry.COLUMN_LESSON_ID + " TEXT NOT NULL," +

                LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE + " TEXT NOT NULL" +

                /*
                 * To ensure this table can only contain one entry per ..., we declare the ...
                 * column to be unique.
                 * We also specify "ON CONFLICT REPLACE". This tells SQLite that if we have a lesson
                 * with the same "_id" entry, we replace the old.
                 */
                //" UNIQUE (" + LessonsContract.MyLessonsEntry._ID + ") ON CONFLICT REPLACE";
                ");";


        // Create a table to hold the lesson part data of the user
        final String SQL_CREATE_MY_LESSON_PARTS_TABLE =

                "CREATE TABLE " +  LessonsContract.MyLessonPartsEntry.TABLE_NAME + " (" +

                        /*
                         * MyLessonPartsEntry did not explicitly declare a column called "_id". However,
                         * MyLessonPartsEntry implements the interface, "BaseColumns", which does have a field
                         * named "_id". We use that here to designate our table's primary key.
                         */
                        LessonsContract.MyLessonPartsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                        // Must map to an lesson _id!
                        LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + " INTEGER NOT NULL," +

                        LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE + " TEXT NOT NULL," +

                        // Now, the consistency is with the _ID of the table lessons
                        // (LESSON_ID) <--> (_ID)
                        "FOREIGN KEY(" + LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + ") " +
                        "REFERENCES " + LessonsContract.MyLessonsEntry.TABLE_NAME + "(" +
                        LessonsContract.MyLessonsEntry._ID +")" +

                        ");";

                        /*
                         * To ensure this table can only contain one entry per ..., we declare the ...
                         * column to be unique.
                         * We also specify "ON CONFLICT REPLACE". This tells SQLite that if we have a lesson
                         * with the same ... entry, we replace the old.
                         */
                        //" UNIQUE (" + LessonsContract.MyLessonPartsEntry._ID + ") ON CONFLICT REPLACE);";

        // Create a table to hold the lesson data of the group
        final String SQL_CREATE_GROUP_LESSONS_TABLE =

                "CREATE TABLE " +  LessonsContract.GroupLessonsEntry.TABLE_NAME + " (" +

                        /*
                         * MyLessonsEntry did not explicitly declare a column called "_id". However,
                         * MyLessonsEntry implements the interface, "BaseColumns", which does have a field
                         * named "_id". We use that here to designate our table's primary key.
                         */
                        LessonsContract.GroupLessonsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                        // this is the id in the author database
                        LessonsContract.GroupLessonsEntry.COLUMN_LESSON_ID + " INTEGER NOT NULL," +

                        LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TITLE + " TEXT NOT NULL," +

                        LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TIME_STAMP + " TEXT NOT NULL," +

                        LessonsContract.GroupLessonsEntry.COLUMN_USER_UID + " TEXT NOT NULL" +

                        ");";


        // Create a table to hold the lesson part data of the group
        final String SQL_CREATE_GROUP_LESSON_PARTS_TABLE =

                "CREATE TABLE " +  LessonsContract.GroupLessonPartsEntry.TABLE_NAME + " (" +

                        /*
                         * MyLessonPartsEntry did not explicitly declare a column called "_id". However,
                         * MyLessonPartsEntry implements the interface, "BaseColumns", which does have a field
                         * named "_id". We use that here to designate our table's primary key.
                         */
                        LessonsContract.GroupLessonPartsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                        // this is the lesson id in the LOCAL DATABASE
                        LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID + " INTEGER NOT NULL," +

                        // this is the part id in the author database
                        LessonsContract.GroupLessonPartsEntry.COLUMN_PART_ID + " INTEGER NOT NULL," +

                        LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TITLE + " TEXT NOT NULL," +

                        LessonsContract.GroupLessonPartsEntry.COLUMN_USER_UID + " TEXT NOT NULL," +

                        // Now, the consistency is with the LESSON_ID, not with the _ID of the table lessons
                        // (USER_ID, LESSON_ID) <--> (USER_ID, LESSON_ID)
                        "FOREIGN KEY("  + LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID + ") " +
                        "REFERENCES " + LessonsContract.GroupLessonsEntry.TABLE_NAME +
                                    "(" + LessonsContract.GroupLessonsEntry._ID +")" +
                        ");";


        // Create the database
        sqLiteDatabase.execSQL(SQL_CREATE_MY_LESSONS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_MY_LESSON_PARTS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_GROUP_LESSONS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_GROUP_LESSON_PARTS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // For now simply drop the table and create a new one. This means if you change the
        // DATABASE_VERSION the table will be dropped.
        // In a production app, this method might be modified to ALTER the table
        // instead of dropping it, so that existing data is not deleted.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LessonsContract.MyLessonPartsEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LessonsContract.MyLessonsEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LessonsContract.GroupLessonPartsEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LessonsContract.GroupLessonsEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
