package com.example.androidstudio.capstoneproject.ui;

import android.content.ContentValues;
import android.content.Intent;
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

public class AddLessonPartActivity extends AppCompatActivity {

    private static final String TAG = AddLessonPartActivity.class.getSimpleName();

    private static final String CLICKED_LESSON_ID = "clickedLessonId";

    private long clickedLesson_id;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_lesson_part);

        // toolbar is defined in the layout file
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        if(getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        // Get a support ActionBar corresponding to this toolbar
        ActionBar actionBar = getSupportActionBar();
        // Enable the Up button
        if (null != actionBar) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Recover information from caller activity
        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra(CLICKED_LESSON_ID)) {
            clickedLesson_id = intentThatStartedThisActivity.getLongExtra(CLICKED_LESSON_ID, -1);
        } else {
            clickedLesson_id = -1;
        }

        // Copy the data from the database to the text view
        if (!(clickedLesson_id >= 0)) {
            Toast.makeText(getBaseContext(), "No Lesson selected!", Toast.LENGTH_LONG).show();
            finish();
        }

    }

    /**
     * onClickAddLessonPart is called when the "ADD" button is clicked.
     * It retrieves user input and inserts that new lesson title data into the underlying database.
     */
    public void onClickAddLessonPart(View view) {

        // Check if EditText is empty, if not retrieve input and store it in a ContentValues object
        // If the EditText input is empty -> don't create an entry
        EditText myEditText = findViewById(R.id.addTextLessonPartTitle);
        String input = myEditText.getText().toString();
        if (input.length() == 0) {
            return;
        }

        // Insert new lesson part data via a ContentResolver
        // Create new empty ContentValues object
        ContentValues contentValues = new ContentValues();
        // Put the lesson part title into the ContentValues
        contentValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID, clickedLesson_id);
        contentValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE, input);

        Log.d(TAG,"onClickAddLessonPart contentValues:" + contentValues.toString());

        // Insert the content values via a ContentResolver
        Uri uri = getContentResolver().insert(LessonsContract.MyLessonPartsEntry.CONTENT_URI, contentValues);

        //Display the URI that's returned with a Toast
        if(uri != null) {
            Toast.makeText(getBaseContext(), uri.toString(), Toast.LENGTH_LONG).show();
        }

        Log.d(TAG, "Uri of added lesson part:" + uri);

//        Snackbar.make(view, "Here's a Snackbar", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show();

        // Finish activity (this returns back to MainActivity)
        finish();

    }


    public void onClickCancel(View view) {
        finish();
    }
}
