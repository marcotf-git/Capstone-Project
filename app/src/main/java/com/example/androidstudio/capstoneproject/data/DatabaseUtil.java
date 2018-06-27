package com.example.androidstudio.capstoneproject.data;

import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;


public class DatabaseUtil {

    public static void deleteDatabase(SQLiteDatabase db){

        if(db == null){
            return;
        }

        try
        {
            db.beginTransaction();
            db.delete (LessonsContract.MyLessonsEntry.TABLE_NAME,null,null);
            db.setTransactionSuccessful();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        finally
        {
            db.endTransaction();
        }

    }
}