package com.example.androidstudio.capstoneproject.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

/**
 * This class is for testing.
 */
public class TestUtil {

    public static void insertFakeData(Context context){

        ContentResolver contentResolver = context.getContentResolver();

        for (int i = 0; i < 10; i++) {
            ContentValues testLessonValues = new ContentValues();
            testLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, "Lesson " + i);
            Uri uri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI, testLessonValues);
        }
    }
}