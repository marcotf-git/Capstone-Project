package com.example.androidstudio.capstoneproject.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LessonsDbHelper extends SQLiteOpenHelper {

    // The database name
    private static final String DATABASE_NAME = "lessons.db";

    // If you change the database schema, you must increment the database version
    private static final int DATABASE_VERSION = 2;

    // Constructor
    public LessonsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        // Create a table to hold the lesson data
        final String SQL_CREATE_MY_LESSONS_TABLE =

                "CREATE TABLE " +  LessonsContract.MyLessonsEntry.TABLE_NAME + " (" +

                /*
                 * MyLessonsEntry did not explicitly declare a column called "_id". However,
                 * MyLessonsEntry implements the interface, "BaseColumns", which does have a field
                 * named "_id". We use that here to designate our table's primary key.
                 */
                LessonsContract.MyLessonsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                //LessonsContract.MyLessonsEntry.COLUMN_LESSON_ID + " TEXT NOT NULL," +

                LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE + " TEXT NOT NULL," +

                /*
                 * To ensure this table can only contain one entry per local lesson, we declare the "_id"
                 * column to be unique.
                 * We also specify "ON CONFLICT REPLACE". This tells SQLite that if we have a lesson
                 * with the same "_id" entry, we replace the old.
                 */
                " UNIQUE (" + LessonsContract.MyLessonsEntry._ID + ") ON CONFLICT REPLACE);";

        // Create the database
        sqLiteDatabase.execSQL(SQL_CREATE_MY_LESSONS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // For now simply drop the table and create a new one. This means if you change the
        // DATABASE_VERSION the table will be dropped.
        // In a production app, this method might be modified to ALTER the table
        // instead of dropping it, so that existing data is not deleted.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LessonsContract.MyLessonsEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
