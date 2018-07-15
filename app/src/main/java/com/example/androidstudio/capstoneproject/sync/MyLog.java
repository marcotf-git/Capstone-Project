package com.example.androidstudio.capstoneproject.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.example.androidstudio.capstoneproject.data.LessonsContract;

import java.util.ArrayList;
import java.util.List;


public class MyLog {

    private static final String TAG = MyLog.class.getSimpleName();

    // This limits the number of rows in the log table
    private static final int MAX_ROWS_LOG_TABLE = 200;

    private Context mContext;


    MyLog(Context mContext) {
        this.mContext = mContext;
    }

    // Add data to the log table and limit its size
    public void addToLog(String logText) {

        ContentResolver contentResolver = mContext.getContentResolver();

        Cursor mCursor = contentResolver.query(LessonsContract.MyLogEntry.CONTENT_URI,
                null,
                null,
                null,
                null);
        int nRows = 0;
        if (mCursor != null) { nRows = mCursor.getCount(); }

        if (nRows > MAX_ROWS_LOG_TABLE) {

            mCursor.moveToFirst();
            // limit the number of deletions
            int maxToDelete = nRows / 5;
            List<Long> idsToDelete = new ArrayList<>();
            // get the id_s of the rows to delete and save in the array
            for (int i = 0; i < maxToDelete; i++) {
                idsToDelete.add(mCursor.getLong(mCursor.getColumnIndex(LessonsContract.MyLogEntry._ID)));
                mCursor.moveToNext();
            }
            mCursor.close();

            // delete that rows
            int count = 0;
            while (count < idsToDelete.size()) {
                long log_id = idsToDelete.get(count);
                // delete the row with that log_id
                Uri uriToDelete = LessonsContract.MyLogEntry.CONTENT_URI.buildUpon()
                        .appendPath(Long.toString(log_id)).build();
                if (uriToDelete != null) {
                    Log.d(TAG, "uriToDelete:" + uriToDelete.toString());
                    int nRowsDeleted = contentResolver.delete(uriToDelete, null, null);
                    Log.d(TAG, "addToLog nRowsDeleted:" + nRowsDeleted);
                    nRows--;
                }
                // count the number of tries
                count++;
            }
        }

        if (mCursor != null) { mCursor.close(); }

        // Now add the new value to the log table
        ContentValues contentValues = new ContentValues();
        //contentValues.put(LessonsContract.MyLogEntry.COLUMN_LOG_ITEM_TEXT, logBuffer.get(j));
        contentValues.put(LessonsContract.MyLogEntry.COLUMN_LOG_ITEM_TEXT, logText);
        // Insert the content values via a ContentResolver
        Uri uri = contentResolver.insert(LessonsContract.MyLogEntry.CONTENT_URI, contentValues);
        if (uri == null) {
            Log.e(TAG, "addToLog: error in inserting item on log",
                    new Exception("addToLog: error in inserting item on log"));
        }
    }


}
