package com.example.androidstudio.capstoneproject.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/**
 * This class is for testing.
 */
public class TestUtil {

    private static final String TAG = TestUtil.class.getSimpleName();


    public static void insertFakeData(Context context){

        ContentResolver contentResolver = context.getContentResolver();

        for (int i = 0; i < 10; i++) {

            ContentValues testLessonValues = new ContentValues();
            testLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, "Lesson " + i);

            Uri lessonUri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI,
                    testLessonValues);

            if (lessonUri != null) {
                Log.d(TAG, "insertFakeData lessonUri:" + lessonUri.toString());
            } else {
                Log.e(TAG, "insertFakeData lessonUri error!");
            }

            Cursor cursor = contentResolver.query(LessonsContract.MyLessonsEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null);

            Long lesson_id = (long) -1;

            if(null != cursor) {
                cursor.moveToLast();
                int columnIndex = cursor.getColumnIndex(LessonsContract.MyLessonsEntry._ID);
                lesson_id = cursor.getLong(columnIndex);
                cursor.close();
            }

            if (!(lesson_id > 0)) {
                Log.e(TAG, "insertFakeData cursor error!");
            }

            for (int j = 0; j < 10; j++) {

                ContentValues testLessonPartsValues = new ContentValues();
                testLessonPartsValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID, lesson_id);
                testLessonPartsValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE,
                        " Lesson " + i + "  -  Part " + j );

                Uri partUri = contentResolver.insert(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                        testLessonPartsValues);

                if (partUri != null) {
                    Log.d(TAG, "insertFakeData partUri:" + partUri.toString());
                } else {
                    Log.e(TAG, "insertFakeData partUri error!");
                }

            }
        }
    }
}