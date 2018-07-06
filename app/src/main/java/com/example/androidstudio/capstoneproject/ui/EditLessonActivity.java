package com.example.androidstudio.capstoneproject.ui;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
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

public class EditLessonActivity extends AppCompatActivity {

    private static final String TAG = EditLessonActivity.class.getSimpleName();

    private static final String SELECTED_LESSON_ID = "selectedLessonId";

    private long selectedLesson_id;

    private Uri updateUri;
    private EditText myEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_lesson);

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
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        myEditText = findViewById(R.id.editTextLessonTitle);

        // Recover information from caller activity
        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra(SELECTED_LESSON_ID)) {
            selectedLesson_id = intentThatStartedThisActivity.getLongExtra(SELECTED_LESSON_ID, -1);
        }

        // Copy the data from the database to the text view
        if (!(selectedLesson_id >= 0)) {
            Toast.makeText(getBaseContext(), "No Lesson selected!", Toast.LENGTH_LONG).show();
            finish();
        }

        Log.d(TAG, "onCreate selectedLesson_id:" + selectedLesson_id);

        updateUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, selectedLesson_id);

        Log.d(TAG, "onCreate updateUri:" + updateUri);

        if (null != getContentResolver()) {

            Cursor cursor = getContentResolver().query(
                    updateUri,
                    null,
                    null,
                    null,
                    null);

            if (cursor != null) {

                cursor.moveToLast();
                Log.d(TAG, "cursor:" + cursor.toString());

                if (cursor.getPosition() == -1) {
                    Log.e(TAG, "Have cursor position -1");
                    Toast.makeText(getBaseContext(),
                            "No data in the database!", Toast.LENGTH_LONG).show();
                    cursor.close();
                    finish();
                } else {
                    String lessonTitle = cursor.getString(cursor.
                            getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE));

                    myEditText.setText(lessonTitle);
                    cursor.close();
                }

            } else {
                Log.e(TAG, "Have no cursor");
                Toast.makeText(getBaseContext(),
                        "Internal error about the database!", Toast.LENGTH_LONG).show();
                finish();
            }

        } else {
            Log.e(TAG, "Did not find content resolver");
            Toast.makeText(getBaseContext(),
                    "Internal error about the database!", Toast.LENGTH_LONG).show();
            finish();
        }

    }

    /**
     * onClickEditLesson is called when the "EDIT" button is clicked.
     * It retrieves user input and edits that lesson title data into the underlying database.
     */
    public void onClickEditLesson(View view) {

        // Copy the data from the database to the text view
        if (selectedLesson_id == -1) {
            Toast.makeText(getBaseContext(), "No Lesson selected!", Toast.LENGTH_LONG).show();
            finish();
        }

        // Check if EditText is empty, if not retrieve input and store it in a ContentValues object
        // If the EditText input is empty -> don't create an entry
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
        int nUpdates = getContentResolver().update(
                updateUri,
                contentValues,
                null,
                null);

        //Display the updates returned with a Toast
        if(nUpdates != -1) {
            Toast.makeText(getBaseContext(), "Updated " +
                    nUpdates + " item(s)", Toast.LENGTH_LONG).show();
        }

        finish();

    }

    public void onClickCancel(View view) {
        finish();
    }
}
