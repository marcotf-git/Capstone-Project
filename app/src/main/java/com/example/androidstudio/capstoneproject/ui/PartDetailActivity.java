package com.example.androidstudio.capstoneproject.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;


public class PartDetailActivity extends AppCompatActivity implements
            LoaderManager.LoaderCallbacks<Cursor>{

    private static final String TAG = PartDetailActivity.class.getSimpleName();


    private static final String PLAYER_VIEW_VISIBILITY = "player_view_visibility";
    private static final String THUMBNAIL_VIEW_VISIBILITY = "thumbnail_view_visibility";
    private static final String ERROR_VIEW_VISIBILITY = "error_view_visibility";
    private static final String CLICKED_LESSON_PART_ID = "clickedLessonPartId";
    private static final String DATABASE_VISIBILITY = "databaseVisibility";

    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";

    // Loader id
    private static final int ID_LESSON_PARTS_LOADER = 2;
    private static final int ID_GROUP_LESSON_PARTS_LOADER = 20;

    // state vars
    private static long referenceLesson_id;
    private static long clickedLessonPart_id;
    private static String databaseVisibility;
    private static int mPlayerViewVisibility;
    private static int imageViewVisibility;
    private static int  errorMessageViewVisibility;

    private String partText;
    private String videoURL;
    private String imageURL;

    // The views variables
    private View mPlayerView;
    private ImageView imageView;
    private TextView errorMessageView;

    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_part_detail);

        mContext = this;

        // The views variables
        mPlayerView =  (View) findViewById(R.id.player_container);
        imageView = (ImageView) findViewById(R.id.iv_image);
        errorMessageView = (TextView) findViewById(R.id.tv_illustration_not_available_label);

        // Recover the views state in case of device rotating
        if (savedInstanceState != null) {
            mPlayerView.setVisibility(mPlayerViewVisibility);
            imageView.setVisibility(imageViewVisibility);
            errorMessageView.setVisibility(errorMessageViewVisibility);
        }

        /* Initialize the data vars for this class */
        // Recover information from caller activity
        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra(CLICKED_LESSON_PART_ID)) {
            clickedLessonPart_id = intentThatStartedThisActivity.getLongExtra(CLICKED_LESSON_PART_ID, -1);
            databaseVisibility = intentThatStartedThisActivity.getStringExtra(DATABASE_VISIBILITY);
        }


        // Query the database and set the view with the cursor data
        if (databaseVisibility.equals(USER_DATABASE)) {
            this.getSupportLoaderManager().initLoader(ID_LESSON_PARTS_LOADER, null, this);
        } else if (databaseVisibility.equals(GROUP_DATABASE)) {
            this.getSupportLoaderManager().initLoader(ID_GROUP_LESSON_PARTS_LOADER, null, this);
        }

        /* Render the views with the data vars */

        // Create a new StepDetailFragment instance and display it using the FragmentManager
        // only create new fragment when there is no previously saved state
//        if (savedInstanceState == null) {
//            loadViews();
//        }

    }


    private void updateView(Cursor cursor) {

        // Set initial state of the player and thumbnail views (this method is only called in two pane)
        errorMessageView.setVisibility(View.GONE);
        mPlayerView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);

        // Remove previously loaded fragments
        FragmentManager myFragmentManager = getSupportFragmentManager();
        Fragment fragment = myFragmentManager.findFragmentById(R.id.part_detail_container);
        if (null != fragment) {
            myFragmentManager.beginTransaction().remove(fragment).commit();
        }
        fragment = myFragmentManager.findFragmentById(R.id.player_container);
        if (null != fragment) {
            myFragmentManager.beginTransaction().remove(fragment).commit();
        }

        Log.v(TAG, "loadViews videoURL:" + videoURL);

        // Create a new PartDetailFragment instance
        PartDetailFragment partDetailFragment = new PartDetailFragment();

        // load the vars data to display
        if (databaseVisibility.equals(USER_DATABASE)) {
            partText = cursor.getString(cursor.getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT));
            imageURL = cursor.getString(cursor.getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_IMAGE_URL));
        } else if (databaseVisibility.equals(GROUP_DATABASE)) {
            partText = cursor.getString(cursor.getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TEXT));
            imageURL = cursor.getString(cursor.getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_IMAGE_URL));
        }

        // Send the data to the fragment data
        if (partText != null) {
            partDetailFragment.setPartText(partText);
        } else {
            partDetailFragment.setPartText("No text available.");
        }

        // Use a FragmentManager and transaction to add the fragment to the screen
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.part_detail_container, partDetailFragment)
                .commit();

        // Create a new ExoPlayerFragment instance and display it using FragmentManager
        // or try to load and show the Thumbnail
        if (!videoURL.equals("")) {

            ExoPlayerFragment exoPlayerFragment = new ExoPlayerFragment();
            // Set the fragment data
            //exoPlayerFragment.setMediaUrl(videoURL);
            // Use a FragmentManager and transaction to add the fragment to the screen
            FragmentManager playerFragmentManager = getSupportFragmentManager();
            playerFragmentManager.beginTransaction()
                    .add(R.id.player_container, exoPlayerFragment)
                    .commit();
            mPlayerView.setVisibility(View.VISIBLE);

        } else {

            // Try to load the Thumbnail
            if (!imageURL.equals("")) {

                Log.v(TAG, "updateView loading image imageURL:" + imageURL);
                /*
                 * Use the call back of picasso to manage the error in loading thumbnail.
                 */
//                Picasso.with(this)
//                        .load(thumbnailURL)
//                        .into(thumbnailView, new Callback() {
//                            @Override
//                            public void onSuccess() {
//                                Log.v(TAG, "Thumbnail loaded");
//                                thumbnailView.setVisibility(View.VISIBLE);
//                            }
//
//                            @Override
//                            public void onError() {
//                                Log.e(TAG, "Error in loading thumbnail");
//                                thumbnailView.setVisibility(View.GONE);
//                                if(mPlayerView.getVisibility() == View.GONE) {
//                                    errorMessageView.setVisibility(View.VISIBLE);
//                                }
//                            }
//                        });
            } else {
                errorMessageView.setVisibility(View.VISIBLE);
            }

        }
    }





    /**
     * Called by the {@link android.support.v4.app.LoaderManagerImpl} when a new Loader needs to be
     * created. This Activity only uses one loader, so we don't necessarily NEED to check the
     * loaderId, but this is certainly best practice.
     *
     * @param loaderId The loader ID for which we need to create a loader
     * @param bundle   Any arguments supplied by the caller
     * @return A new Loader instance that is ready to start loading.
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {

        Log.d(TAG, "onCreateLoader loaderId:" + loaderId);

        switch (loaderId) {

            case ID_LESSON_PARTS_LOADER:
                /* URI for all rows of lesson parts data in our "my_lesson_parts" table */
                Uri partsQueryUri = LessonsContract.MyLessonPartsEntry.CONTENT_URI;

                String lessonPartsSelection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
                String[] lessonPartsSelectionArgs = {Long.toString(referenceLesson_id)};

                Log.v(TAG, "onCreateLoader lessonPartsSelection:" + lessonPartsSelection);
                Log.v(TAG, "onCreateLoader referenceLesson_id:" + referenceLesson_id);

                return new CursorLoader(mContext,
                        partsQueryUri,
                        null,
                        lessonPartsSelection,
                        lessonPartsSelectionArgs,
                        null);

            case ID_GROUP_LESSON_PARTS_LOADER:
                /* URI for all rows of lesson parts data in our "my_lesson_parts" table */
                Uri groupPartsQueryUri = LessonsContract.GroupLessonPartsEntry.CONTENT_URI;

                String groupPartsSelection = LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID + "=?";
                String[] groupPartsSelectionArgs = {Long.toString(referenceLesson_id)};

                return new CursorLoader(mContext,
                        groupPartsQueryUri,
                        null,
                        groupPartsSelection,
                        groupPartsSelectionArgs,
                        null);

            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);
        }
    }

    /**
     * Called when a Loader has finished loading its data.
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        Log.v(TAG, "onLoadFinished cursor:" + data.toString());
        // Pass the data to the view
        updateView(data);
    }

    /**
     * Called when a previously created loader is being reset, and thus making its data unavailable.
     * The application should at this point remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        /*
         * Since this Loader's data is now invalid, we need to clear the view that is
         * displaying the data.
         */
        updateView(null);
    }

    // This is for saving the step that is being viewed when the device is rotated
    @Override
    public void onSaveInstanceState(Bundle outState) {

        mPlayerViewVisibility =  mPlayerView.getVisibility();
        imageViewVisibility = imageView.getVisibility();
        errorMessageViewVisibility = errorMessageView.getVisibility();

        super.onSaveInstanceState(outState);

    }



}
