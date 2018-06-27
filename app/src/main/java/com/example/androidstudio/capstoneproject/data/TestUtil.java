package com.example.androidstudio.capstoneproject.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is for testing.
 */
public class TestUtil {

    public static void insertFakeData(Context context){

        LessonsDbHelper dbHelper = new LessonsDbHelper(context);
        SQLiteDatabase mDb = dbHelper.getWritableDatabase();

        if(mDb == null){
            return;
        }

        //create a list of fake guests
        List<ContentValues> list = new ArrayList<>();

        ContentValues cv;
        for (int i = 0; i < 10; i++){
            cv = new ContentValues();
            cv.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME, "Lesson " + i);
            list.add(cv);
        }

        //insert all movies in one transaction
        try
        {
            mDb.beginTransaction();
            //clear the table first
            mDb.delete (LessonsContract.MyLessonsEntry.TABLE_NAME,null,null);
            //go through the list and add one by one
            for(ContentValues row:list){
                mDb.insert(LessonsContract.MyLessonsEntry.TABLE_NAME, null, row);
            }
            mDb.setTransactionSuccessful();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally
        {
            mDb.endTransaction();
        }

        mDb.close();

    }
}