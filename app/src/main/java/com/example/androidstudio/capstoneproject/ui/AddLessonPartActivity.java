package com.example.androidstudio.capstoneproject.ui;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;

public class AddLessonPartActivity extends AppCompatActivity {

    private static final String TAG = AddLessonPartActivity.class.getSimpleName();
    private static final String CLICKED_LESSON_ID = "clickedLessonId";

    private long clickedLesson_id;

    private Uri queryUri;
    private TextView myLessonTitleText;


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

        // Recover information from caller activity
        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra(CLICKED_LESSON_ID)) {
            clickedLesson_id = intentThatStartedThisActivity.getLongExtra(CLICKED_LESSON_ID, -1);
        } else {
            clickedLesson_id = -1;
        }

        myLessonTitleText = (TextView) findViewById(R.id.textLessonTitleView);

        // Copy the data from the database to the text view
        if (!(clickedLesson_id >= 0)) {
            Toast.makeText(getBaseContext(), "No Lesson selected!", Toast.LENGTH_LONG).show();
            finish();
        }

        queryUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, clickedLesson_id);

        Log.d(TAG, "onCreate queryUri:" + queryUri);

        if (null != getContentResolver()) {

            Cursor cursor = getContentResolver().query(
                    queryUri,
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

                    myLessonTitleText.setText(lessonTitle);
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

        // Finish activity (this returns back to MainActivity)
        finish();

    }


    public void onClickCancel(View view) {
        finish();
    }
}
