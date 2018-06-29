package com.example.androidstudio.capstoneproject.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is for testing.
 */
public class TestUtil {

    public static void insertFakeData(Context context){

        ContentResolver contentResolver = context.getContentResolver();

        for (int i = 0; i < 10; i++) {
            ContentValues testLessonValues = new ContentValues();
            testLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME, "Lesson " + i);
            Uri uri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI, testLessonValues);
        }
    }
}