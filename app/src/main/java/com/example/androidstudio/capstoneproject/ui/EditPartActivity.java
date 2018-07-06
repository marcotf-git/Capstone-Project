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

public class EditPartActivity extends AppCompatActivity {

    private static final String TAG = EditPartActivity.class.getSimpleName();
    private static final String SELECTED_LESSON_PART_ID = "selectedLessonPartId";

    private long selectedLessonPartId;

    private Uri updateUri;
    private EditText myEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_part);

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

        myEditText = findViewById(R.id.editTextLessonPartTitle);

        // Recover information from caller activity
        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra(SELECTED_LESSON_PART_ID)) {
            selectedLessonPartId = intentThatStartedThisActivity.getLongExtra(SELECTED_LESSON_PART_ID, -1);
        }

        // Copy the data from the database to the text view
        if (!(selectedLessonPartId >= 0)) {
            Toast.makeText(getBaseContext(), "No Lesson Part selected!", Toast.LENGTH_LONG).show();
            finish();
        }

        Log.d(TAG, "onCreate selectedLessonPartId:" + selectedLessonPartId);

        updateUri = ContentUris.withAppendedId(LessonsContract.MyLessonPartsEntry.CONTENT_URI, selectedLessonPartId);

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
                    String lessonPartTitle = cursor.getString(cursor.
                            getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE));

                    myEditText.setText(lessonPartTitle);
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
     * onClickEditPartLesson is called when the "EDIT" button is clicked.
     * It retrieves user input and edits that lesson part title data into the underlying database.
     */
    public void onClickEditPartLesson(View view) {

        // Copy the data from the database to the text view
        if (selectedLessonPartId == -1) {
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
        contentValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE, input);

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
