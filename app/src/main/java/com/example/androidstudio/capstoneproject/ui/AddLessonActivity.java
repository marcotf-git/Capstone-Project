package com.example.androidstudio.capstoneproject.ui;

import android.content.ContentValues;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;


public class AddLessonActivity extends AppCompatActivity {

    private static final String TAG = AddLessonActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lesson);

        // toolbar is defined in the layout file
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        if(getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
//        // Get a support ActionBar corresponding to this toolbar
//        ActionBar actionBar = getSupportActionBar();
//        // Enable the Up button
//        if (null != actionBar) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//        }

    }

    /**
     * onClickAddLesson is called when the "ADD" button is clicked.
     * It retrieves user input and inserts that new lesson title data into the underlying database.
     */
    public void onClickAddLesson(View view) {

        // Check if EditText is empty, if not retrieve input and store it in a ContentValues object
        // If the EditText input is empty -> don't create an entry
        EditText myEditText = findViewById(R.id.addTextLessonTitle);
        String input = myEditText.getText().toString();
        if (input.length() == 0) {
            return;
        }

        // Insert new lesson data via a ContentResolver
        // Create new empty ContentValues object
        ContentValues contentValues = new ContentValues();
        // Put the lesson title into the ContentValues
        contentValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, input);
        // Insert the content values via a ContentResolver
        Uri uri = getContentResolver().insert(LessonsContract.MyLessonsEntry.CONTENT_URI, contentValues);

         //Display the URI that's returned with a Toast
        if(uri != null) {
            Toast.makeText(getBaseContext(), uri.toString(), Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "Uri of added lesson:" + uri);

        //Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG)
        //  .setAction("Action", null).show();

        // Finish activity (this returns back to MainActivity)
        finish();

    }


    public void onClickCancel(View view) {
        finish();
    }

}
