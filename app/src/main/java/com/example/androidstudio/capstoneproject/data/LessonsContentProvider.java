package com.example.androidstudio.capstoneproject.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;


public class LessonsContentProvider extends ContentProvider {

    // Define final integer constants for the directory of lessons and a single item.
    // It's convention to use 100, 200, 300, etc for directories,
    // and related ints (101, 102, ..) for items in that directory.
    public static final int MY_LESSONS = 100;
    public static final int MY_LESSON_WITH_ID = 101;

    // Declare a static variable for the Uri matcher
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    /**
     Initialize a new matcher object without any matches,
     then use .addURI(String authority, String path, int match) to add matches
     */
    public static UriMatcher buildUriMatcher() {

        // Initialize a UriMatcher with no matches by passing in NO_MATCH to the constructor
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        /*
          All paths added to the UriMatcher have a corresponding int.
          For each kind of uri you may want to access, add the corresponding match with addURI.
          The two calls below add matches for the my_lessons directory and a single item by ID.
         */
        uriMatcher.addURI(LessonsContract.AUTHORITY, LessonsContract.PATH_MY_LESSONS, MY_LESSONS);
        uriMatcher.addURI(LessonsContract.AUTHORITY, LessonsContract.PATH_MY_LESSONS + "/#", MY_LESSON_WITH_ID);

        return uriMatcher;

    }

    // Member variable for a LessonsDbHelper that's initialized in the onCreate() method
    private LessonsDbHelper mLessonsDbHelper;

    @Override
    public boolean onCreate() {
        // Initialize a LessonsDbHelper on startup
        // Declare the DbHelper as a global variable
        Context context = getContext();
        mLessonsDbHelper = new LessonsDbHelper(context);
        return true;
    }


    // Implement insert to handle requests to insert a single new row of data
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        // Get access to the lessons.db database (to write new data to)
        final SQLiteDatabase db = mLessonsDbHelper.getWritableDatabase();

        // Write URI matching code to identify the match for the my_lessons directory
        int match = sUriMatcher.match(uri);
        Uri returnUri; // URI to be returned

        switch (match) {
            case MY_LESSONS:
                // Insert new values into the database
                // Inserting values into my_lessons table
                long _id = db.insert(LessonsContract.MyLessonsEntry.TABLE_NAME, null, values);
                if ( _id > 0 ) {
                    returnUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, _id);
                } else {
                    throw new SQLException("Failed to insert row into " + uri);
                }
                break;
            // Set the value for the returnedUri and write the default case for unknown URI's
            // Default case throws an UnsupportedOperationException
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver if the uri has been changed, and return the newly inserted URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return constructed uri (this points to the newly inserted row of data)
        return returnUri;
    }


    // Implement query to handle requests for data by URI
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        // Get access to underlying database (read-only for query)
        final SQLiteDatabase db = mLessonsDbHelper.getReadableDatabase();

        // Write URI match code and set a variable to return a Cursor
        int match = sUriMatcher.match(uri);
        Cursor retCursor;

        // Query for the my_lessons directory and write a default case
        switch (match) {
            // Query for the my_lessons directory
            case MY_LESSONS:
                retCursor =  db.query(LessonsContract.MyLessonsEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            case MY_LESSON_WITH_ID:
                // Get the lesson "_id" from the URI path
                String _id = uri.getPathSegments().get(1);
                // Use selections/selectionArgs to filter for this "_id"
                retCursor =  db.query(LessonsContract.MyLessonsEntry.TABLE_NAME,
                        projection,
                        "_id=?",
                        new String[]{_id},
                        null,
                        null,
                        null);
                break;
            // Default exception
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Set a notification URI on the Cursor and return that Cursor
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the desired Cursor
        return retCursor;
    }


    // Implement delete to delete a single row of data
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

        // Get access to the database and write URI matching code to recognize a single item
        final SQLiteDatabase db = mLessonsDbHelper.getWritableDatabase();

        int match = sUriMatcher.match(uri);

        // Keep track of the number of deleted lessons
        int lessonsDeleted; // starts as 0

        // Write the code to delete a single row of data
        // [Hint] Use selections to delete an item by its row ID
        switch (match) {
            // Handle the single item case, recognized by the _id included in the URI path
            case MY_LESSON_WITH_ID:
                // Get the lesson "_id" from the URI path
                String _id = uri.getPathSegments().get(1);
                // Use selections/selectionArgs to filter for this "_id"
                lessonsDeleted = db.delete(LessonsContract.MyLessonsEntry.TABLE_NAME,
                        "_id=?", new String[]{_id});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver of a change and return the number of items deleted
        if (lessonsDeleted != 0 && (null !=  getContext())) {
            // A lesson was deleted, set notification
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of lessons deleted
        return lessonsDeleted;
    }


    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        // Get access to the database and write URI matching code to recognize a single item
        final SQLiteDatabase db = mLessonsDbHelper.getWritableDatabase();
        int match = sUriMatcher.match(uri);

        // Keep track of if an update occurs
        int lessonsUpdated;

        switch (match) {
            case MY_LESSON_WITH_ID:
                // update a single lesson by getting the "_id"
                String _id = uri.getPathSegments().get(1);
                // Use selections/selectionArgs to filter for this ID
                lessonsUpdated = db.update(LessonsContract.MyLessonsEntry.TABLE_NAME, values,
                        "_id=?", new String[]{_id});
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Notify the resolver of a lesson was updated
        if (lessonsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number of lessons updated
        return lessonsUpdated;
    }


    @Override
    public String getType(@NonNull Uri uri) {

        throw new UnsupportedOperationException("Not yet implemented");
    }
}
