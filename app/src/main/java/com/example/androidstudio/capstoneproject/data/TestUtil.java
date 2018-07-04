package com.example.androidstudio.capstoneproject.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * This class is for testing.
 */
public class TestUtil {

    private static final String TAG = TestUtil.class.getSimpleName();


    public static void insertFakeData(Context context){


        ContentResolver contentResolver = context.getContentResolver();

        // create for the local users tables
        for (int i = 1; i <= 6; i++) {

            ContentValues testLessonValues = new ContentValues();
            testLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, "Lesson " + i +
            " author: myself");

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

            for (int j = 1; j <= 6; j++) {

                ContentValues testLessonPartsValues = new ContentValues();
                testLessonPartsValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID, lesson_id);
                testLessonPartsValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE,
                        " Lesson " + i + "  -  Part " + j + " author: myself");

                Uri partUri = contentResolver.insert(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                        testLessonPartsValues);

                if (partUri != null) {
                    Log.d(TAG, "insertFakeData partUri:" + partUri.toString());
                } else {
                    Log.e(TAG, "insertFakeData partUri error!");
                }

            }
        }


        long part_id_author1 = 1;

        // create for the group users tables
        for (int lesson_id = 1; lesson_id <= 3; lesson_id++) {

            ContentValues testLessonValues = new ContentValues();
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_ID, lesson_id);
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TITLE, "Lesson "
                    + lesson_id + " author: ABCDEF");
            String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                    .format(new Date());
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TIME_STAMP, time_stamp);
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_USER_UID, "ABCDEF");

            Uri lessonUri = contentResolver.insert(LessonsContract.GroupLessonsEntry.CONTENT_URI,
                    testLessonValues);


            long inserted_lesson_id = -1;

            if (lessonUri != null) {
                inserted_lesson_id = Long.parseLong(lessonUri.getPathSegments().get(1));
                Log.d(TAG, "insertFakeData lessonUri:" + lessonUri.toString());
                Log.d(TAG, "insertFakeData inserted_lesson_id:" + inserted_lesson_id);
            } else {
                Log.e(TAG, "insertFakeData lessonUri error!");
            }

            if (inserted_lesson_id == -1) {
                Log.e(TAG, "insertFakeData inserted_lesson_id error!");
                return;
            }

            for (int j = 1; j <= 6; j++) {

                ContentValues testLessonPartsValues = new ContentValues();
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID,
                        Long.toString(inserted_lesson_id));
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_ID, part_id_author1);
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TITLE,
                        " Lesson " + lesson_id + "  -  Part " + j + " author: ABCDEF");
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_USER_UID, "ABCDEF");

                Uri partUri = contentResolver.insert(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                        testLessonPartsValues);

                if (partUri != null) {
                    Log.d(TAG, "insertFakeData partUri:" + partUri.toString());
                } else {
                    Log.e(TAG, "insertFakeData partUri error!");
                }

                part_id_author1++;

            }
        }


        long part_id_author2 = 1;

        // create for the group users tables
        for (int lesson_id = 1; lesson_id <= 3; lesson_id++) {

            // insert one lesson
            ContentValues testLessonValues = new ContentValues();
            // lesson id as in the external database
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_ID, lesson_id);
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TITLE, "Lesson "
                    + lesson_id + " author: GHIJKL");
            String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                    .format(new Date());
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TIME_STAMP, time_stamp);
            testLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_USER_UID, "GHIJKL");

            Uri lessonUri = contentResolver.insert(LessonsContract.GroupLessonsEntry.CONTENT_URI,
                    testLessonValues);


            long inserted_lesson_id = -1;

            if (lessonUri != null) {
                inserted_lesson_id = Long.parseLong(lessonUri.getPathSegments().get(1));
                Log.d(TAG, "insertFakeData lessonUri:" + lessonUri.toString());
                Log.d(TAG, "insertFakeData inserted_lesson_id:" + inserted_lesson_id);
            } else {
                Log.e(TAG, "insertFakeData lessonUri error!");
            }

            if (inserted_lesson_id == -1) {
                Log.e(TAG, "insertFakeData inserted_lesson_id error!");
                return;
            }

            // insert six parts
            for (int j = 1; j <= 6; j++) {

                ContentValues testLessonPartsValues = new ContentValues();
                // _id of the lesson
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID,
                        Long.toString(inserted_lesson_id));
                // part id as in the external database
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_ID, part_id_author2);
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TITLE,
                        " Lesson " + lesson_id + "  -  Part " + j + " author: GHIJKL");
                testLessonPartsValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_USER_UID, "GHIJKL");

                Uri partUri = contentResolver.insert(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                        testLessonPartsValues);

                if (partUri != null) {
                    Log.d(TAG, "insertFakeData partUri:" + partUri.toString());
                } else {
                    Log.e(TAG, "insertFakeData partUri error!");
                }

                part_id_author2++;

            }
        }

    }

}