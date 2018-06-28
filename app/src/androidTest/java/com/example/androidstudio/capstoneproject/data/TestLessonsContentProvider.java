/*
* Copyright (C) 2016 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.androidstudio.capstoneproject.data;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class TestLessonsContentProvider {

    /* Context used to access various parts of the system */
    private final Context mContext = InstrumentationRegistry.getTargetContext();

    /**
     * Because we annotate this method with the @Before annotation, this method will be called
     * before every single method with an @Test annotation. We want to start each test clean, so we
     * delete all entries in the tasks directory to do so.
     */
    @Before
    public void setUp() {
        /* Use the LessonsDbHelper to get access to a writable database */
        LessonsDbHelper dbHelper = new LessonsDbHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.delete(LessonsContract.MyLessonsEntry.TABLE_NAME, null, null);
    }


    //================================================================================
    // Test ContentProvider Registration
    //================================================================================


    /**
     * This test checks to make sure that the content provider is registered correctly in the
     * AndroidManifest file. If it fails, you should check the AndroidManifest to see if you've
     * added a <provider/> tag and that you've properly specified the android:authorities attribute.
     */
    @Test
    public void testProviderRegistry() {

        /*
         * A ComponentName is an identifier for a specific application component, such as an
         * Activity, ContentProvider, BroadcastReceiver, or a Service.
         *
         * Two pieces of information are required to identify a component: the package (a String)
         * it exists in, and the class (a String) name inside of that package.
         *
         * We will use the ComponentName for our ContentProvider class to ask the system
         * information about the ContentProvider, specifically, the authority under which it is
         * registered.
         */
        String packageName = mContext.getPackageName();
        String lessonsProviderClassName = LessonsContentProvider.class.getName();
        ComponentName componentName = new ComponentName(packageName, lessonsProviderClassName);

        try {

            /*
             * Get a reference to the package manager. The package manager allows us to access
             * information about packages installed on a particular device. In this case, we're
             * going to use it to get some information about our ContentProvider under test.
             */
            PackageManager pm = mContext.getPackageManager();

            /* The ProviderInfo will contain the authority, which is what we want to test */
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);
            String actualAuthority = providerInfo.authority;
            String expectedAuthority = packageName;

            /* Make sure that the registered authority matches the authority from the Contract */
            String incorrectAuthority =
                    "Error: TaskContentProvider registered with authority: " + actualAuthority +
                            " instead of expected authority: " + expectedAuthority;
            assertEquals(incorrectAuthority,
                    actualAuthority,
                    expectedAuthority);

        } catch (PackageManager.NameNotFoundException e) {
            String providerNotRegisteredAtAll =
                    "Error: TaskContentProvider not registered at " + mContext.getPackageName();
            /*
             * This exception is thrown if the ContentProvider hasn't been registered with the
             * manifest at all. If this is the case, you need to double check your
             * AndroidManifest file
             */
            fail(providerNotRegisteredAtAll);
        }
    }


    //================================================================================
    // Test UriMatcher
    //================================================================================


    private static final Uri TEST_LESSONS = LessonsContract.MyLessonsEntry.CONTENT_URI;
    // Content URI for a single task with id = 1
    private static final Uri TEST_LESSON_WITH_ID = TEST_LESSONS.buildUpon().appendPath("1").build();


    /**
     * This function tests that the UriMatcher returns the correct integer value for
     * each of the Uri types that the ContentProvider can handle. Uncomment this when you are
     * ready to test your UriMatcher.
     */
    @Test
    public void testUriMatcher() {

        /* Create a URI matcher that the TaskContentProvider uses */
        UriMatcher testMatcher = LessonsContentProvider.buildUriMatcher();

        /* Test that the code returned from our matcher matches the expected MY_LESSONS int */
        String tasksUriDoesNotMatch = "Error: The TASKS URI was matched incorrectly.";
        int actualTasksMatchCode = testMatcher.match(TEST_LESSONS);
        int expectedTasksMatchCode = LessonsContentProvider.MY_LESSONS;
        assertEquals(tasksUriDoesNotMatch,
                actualTasksMatchCode,
                expectedTasksMatchCode);

        /* Test that the code returned from our matcher matches the expected MY_LESSON_WITH_ID */
        String taskWithIdDoesNotMatch =
                "Error: The TASK_WITH_ID URI was matched incorrectly.";
        int actualTaskWithIdCode = testMatcher.match(TEST_LESSON_WITH_ID);
        int expectedTaskWithIdCode = LessonsContentProvider.MY_LESSON_WITH_ID;
        assertEquals(taskWithIdDoesNotMatch,
                actualTaskWithIdCode,
                expectedTaskWithIdCode);
    }


    //================================================================================
    // Test Insert (for my_lessons directory)
    //================================================================================


    /**
     * Tests inserting a single row of data via a ContentResolver
     */
    @Test
    public void testInsert() {

        /* Create values to insert */
        ContentValues testLessonValues = new ContentValues();
        testLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME, "Lesson name");

        /* TestContentObserver allows us to test if notifyChange was called appropriately */
        TestUtilities.TestContentObserver lessonObserver = TestUtilities.getTestContentObserver();

        ContentResolver contentResolver = mContext.getContentResolver();

        /* Register a content observer to be notified of changes to data at a given URI (tasks) */
        contentResolver.registerContentObserver(
                /* URI that we would like to observe changes to */
                LessonsContract.MyLessonsEntry.CONTENT_URI,
                /* Whether or not to notify us if descendants of this URI change */
                true,
                /* The observer to register (that will receive notifyChange callbacks) */
                lessonObserver);


        Uri uri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI, testLessonValues);

        long testId = Integer.parseInt(uri.getLastPathSegment());

        uri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI, testLessonValues);

        Uri expectedUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI,
                testId + 1);

        String insertProviderFailed = "Unable to insert item through Provider";
        assertEquals(insertProviderFailed, expectedUri, uri);

        /*
         * If this fails, it's likely you didn't call notifyChange in your insert method from
         * your ContentProvider.
         */
        lessonObserver.waitForNotificationOrFail();

        /*
         * waitForNotificationOrFail is synchronous, so after that call, we are done observing
         * changes to content and should therefore unregister this observer.
         */
        contentResolver.unregisterContentObserver(lessonObserver);
    }


    //================================================================================
    // Test Query (for my_lessons directory)
    //================================================================================


    /**
     * Inserts data, then tests if a query for the tasks directory returns that data as a Cursor
     */
    @Test
    public void testQuery() {

        /* Get access to a writable database */
        LessonsDbHelper dbHelper = new LessonsDbHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        /* Create values to insert */
        ContentValues testLessonValues = new ContentValues();
        testLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME, "Lesson name");

        /* Insert ContentValues into database and get a row ID back */
        long lessonRowId = database.insert(
                /* Table to insert values into */
                LessonsContract.MyLessonsEntry.TABLE_NAME,
                null,
                /* Values to insert into table */
                testLessonValues);

        String insertFailed = "Unable to insert directly into the database";
        assertTrue(insertFailed, lessonRowId != -1);

        /* We are done with the database, close it now. */
        database.close();

        /* Perform the ContentProvider query */
        Cursor lessonCursor = mContext.getContentResolver().query(
                LessonsContract.MyLessonsEntry.CONTENT_URI,
                /* Columns; leaving this null returns every column in the table */
                null,
                /* Optional specification for columns in the "where" clause above */
                null,
                /* Values for "where" clause */
                null,
                /* Sort order to return in Cursor */
                null);


        String queryFailed = "Query failed to return a valid Cursor";
        assertTrue(queryFailed, lessonCursor != null);

        /* We are done with the cursor, close it now. */
        lessonCursor.close();
    }


    //================================================================================
    // Test Update (for my_lessons directory)
    //================================================================================


    /**
     * Tests updating a single row of data via a ContentResolver
     */
    @Test
    public void testUpdate() {

        /* Create values to insert */
        ContentValues testLessonValues = new ContentValues();
        testLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME, "Lesson name 1");

        /* TestContentObserver allows us to test if notifyChange was called appropriately */
        TestUtilities.TestContentObserver lessonObserver = TestUtilities.getTestContentObserver();

        ContentResolver contentResolver = mContext.getContentResolver();

        /* Register a content observer to be notified of changes to data at a given URI (tasks) */
        contentResolver.registerContentObserver(
                /* URI that we would like to observe changes to */
                LessonsContract.MyLessonsEntry.CONTENT_URI,
                /* Whether or not to notify us if descendants of this URI change */
                true,
                /* The observer to register (that will receive notifyChange callbacks) */
                lessonObserver);


        Uri uri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI, testLessonValues);

        long testId = Long.parseLong(uri.getLastPathSegment());

        /* Create values to update */
        ContentValues testEditLessonValues = new ContentValues();
        final String TEST_UPDATE_STRING = "Lesson name 2";
        testEditLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME, TEST_UPDATE_STRING);

        Uri updateUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, testId);

        int numberOfLessonsUpdated = contentResolver.update(updateUri,
                testEditLessonValues, null, null);

        String updateProviderFailed = "Unable to update item through Provider";
        assertEquals(updateProviderFailed, 1, numberOfLessonsUpdated);

        /* Perform the ContentProvider query */
        Cursor lessonCursor = mContext.getContentResolver().query(
                LessonsContract.MyLessonsEntry.CONTENT_URI,
                /* Columns; leaving this null returns every column in the table */
                null,
                /* Optional specification for columns in the "where" clause above */
                null,
                /* Values for "where" clause */
                null,
                /* Sort order to return in Cursor */
                null);

        lessonCursor.moveToLast();

        String testString = lessonCursor.getString(lessonCursor.
                getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME));

        updateProviderFailed = "Update item is different";
        assertEquals(updateProviderFailed, TEST_UPDATE_STRING, testString);

        /*
         * If this fails, it's likely you didn't call notifyChange in your insert method from
         * your ContentProvider.
         */
        lessonObserver.waitForNotificationOrFail();

        /*
         * waitForNotificationOrFail is synchronous, so after that call, we are done observing
         * changes to content and should therefore unregister this observer.
         */
        contentResolver.unregisterContentObserver(lessonObserver);
    }


    //================================================================================
    // Test Delete (for my_lessons directory; for a single item)
    //================================================================================


    /**
     * Tests deleting a single row of data via a ContentResolver
     */
    @Test
    public void testDelete() {
        /* Access writable database */
        LessonsDbHelper helper = new LessonsDbHelper(InstrumentationRegistry.getTargetContext());
        SQLiteDatabase database = helper.getWritableDatabase();

        /* Create a new row of task data */
        ContentValues testLessonValues = new ContentValues();
        testLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME, "Lesson name");

        /* Insert ContentValues into database and get a row ID back */
        long lessonRow_id = database.insert(
                /* Table to insert values into */
                LessonsContract.MyLessonsEntry.TABLE_NAME,
                null,
                /* Values to insert into table */
                testLessonValues);

        /* Always close the database when you're through with it */
        database.close();

        String insertFailed = "Unable to insert into the database";
        assertTrue(insertFailed, lessonRow_id != -1);


        /* TestContentObserver allows us to test if notifyChange was called appropriately */
        TestUtilities.TestContentObserver lessonObserver = TestUtilities.getTestContentObserver();

        ContentResolver contentResolver = mContext.getContentResolver();

        /* Register a content observer to be notified of changes to data at a given URI (tasks) */
        contentResolver.registerContentObserver(
                /* URI that we would like to observe changes to */
                LessonsContract.MyLessonsEntry.CONTENT_URI,
                /* Whether or not to notify us if descendants of this URI change */
                true,
                /* The observer to register (that will receive notifyChange callbacks) */
                lessonObserver);


        /* The delete method deletes the previously inserted row by its _id */
        Uri uriToDelete = LessonsContract.MyLessonsEntry.CONTENT_URI.buildUpon()
                .appendPath("" + lessonRow_id + "").build();

        Log.v("Testing", "Uri to delete:" + uriToDelete.toString());

        int numberOfLessonsDeleted = contentResolver.delete(uriToDelete, null, null);



        String deleteFailed = "Unable to delete item in the database";
        assertTrue(deleteFailed, numberOfLessonsDeleted != 0);

        /*
         * If this fails, it's likely you didn't call notifyChange in your delete method from
         * your ContentProvider.
         */
        lessonObserver.waitForNotificationOrFail();

        /*
         * waitForNotificationOrFail is synchronous, so after that call, we are done observing
         * changes to content and should therefore unregister this observer.
         */
        contentResolver.unregisterContentObserver(lessonObserver);
    }

}
