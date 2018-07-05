package com.example.androidstudio.capstoneproject.ui;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.data.TestUtil;
import com.example.androidstudio.capstoneproject.utilities.MyFirebaseUtilities;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


public class PartDetailActivity extends AppCompatActivity implements
            LoaderManager.LoaderCallbacks<Cursor>{

    private static final String TAG = PartDetailActivity.class.getSimpleName();

    private static final String CLICKED_LESSON_PART_ID = "clickedLessonPartId";
    private static final String DATABASE_VISIBILITY = "databaseVisibility";

    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";

    // Loader id
    private static final int ID_LESSON_PARTS_LOADER = 2;
    private static final int ID_GROUP_LESSON_PARTS_LOADER = 20;

    // state vars
    private static long clickedLessonPart_id;
    private static String databaseVisibility;
    private static int mPlayerViewVisibility;
    private static int imageViewVisibility;
    private static int  errorMessageViewVisibility;

    private String partText;
    private String videoURL;
    private String imageURL;

    // Menus and buttons
    private Menu mMenu;
    private Toolbar mToolbar;
    private ActionBar actionBar;
    private FloatingActionButton mButton;

    // The views variables
    private View mPlayerView;
    private ImageView imageView;
    private ProgressBar mLoadingIndicator;
    private TextView errorMessageView;

    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_part_detail);

        mContext = this;

        // Add the toolbar as the default app bar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        // Get a support ActionBar corresponding to this toolbar
        actionBar = getSupportActionBar();
        if (null != actionBar) {
            // Enable the Up button (icon will be set in onPrepareMenu
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        /*
         Set the Floating Action Button (FAB) to its corresponding View.
         Attach an OnClickListener to it, so that when it's clicked, a new intent will be created
         to launch the AddLessonActivity.
         */
        mButton = findViewById(R.id.fab_add);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(mContext,"FAB clicked!", Toast.LENGTH_LONG).show();
            }
        });

        // The views variables
        mPlayerView =  (View) findViewById(R.id.player_container);
        imageView = (ImageView) findViewById(R.id.iv_image);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);
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

        mLoadingIndicator.setVisibility(View.VISIBLE);

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

        partText = null;
        imageURL = null;

        if (null != cursor) {

            Log.v(TAG, "updateView cursor.getCount():" + cursor.getCount());

            cursor.moveToLast();

            // load the vars data to display
            if (databaseVisibility.equals(USER_DATABASE)) {
                if (!cursor.isNull(cursor.getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT))) {
                    partText = cursor.getString(cursor.getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT));
                }
                if (!cursor.isNull(cursor.getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_IMAGE_URL))) {
                    imageURL = cursor.getString(cursor.getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_IMAGE_URL));
                }
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                if (!cursor.isNull(cursor.getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TEXT))) {
                    partText = cursor.getString(cursor.getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TEXT));
                }
                if (!cursor.isNull(cursor.getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_IMAGE_URL))) {
                    imageURL = cursor.getString(cursor.getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_IMAGE_URL));
                }
            }

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
        if (null != videoURL && !videoURL.equals("")) {

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
            if (null != imageURL && !imageURL.equals("")) {

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

        cursor.close();
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

        Uri partUri;

        switch (loaderId) {

            case ID_LESSON_PARTS_LOADER:

                /* URI for one lesson part by its _id in "my_lesson_parts" table */
                partUri = ContentUris.withAppendedId(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                        clickedLessonPart_id);

                Log.v(TAG, "onCreateLoader partUri:" + partUri.toString());
                Log.v(TAG, "onCreateLoader clickedLessonPart_id:" + clickedLessonPart_id);

                return new CursorLoader(mContext,
                        partUri,
                        null,
                        null,
                        null,
                        null);

            case ID_GROUP_LESSON_PARTS_LOADER:

                /* URI for one lesson part by its _id in "my_lesson_parts" table */
                partUri = ContentUris.withAppendedId(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                        clickedLessonPart_id);

                Log.v(TAG, "onCreateLoader partUri:" + partUri.toString());
                Log.v(TAG, "onCreateLoader clickedLessonPart_id:" + clickedLessonPart_id);

                return new CursorLoader(mContext,
                        partUri,
                        null,
                        null,
                        null,
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
        mLoadingIndicator.setVisibility(View.INVISIBLE);
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

    // Inflate the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Save a reference to the menu
        mMenu = menu;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String modeOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        if (modeOption.equals(this.getString(R.string.pref_mode_view))) {
            menu.findItem(R.id.select_view).setChecked(true);
        }

        if (modeOption.equals(this.getString(R.string.pref_mode_create))) {
            menu.findItem(R.id.select_create).setChecked(true);
        }

         // Prepare the visibility of the action items
        contextualizeMenu();

        return true;

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemThatWasClickedId = item.getItemId();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        switch (itemThatWasClickedId) {

            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected");
                // Set the action according to the views visibility
//                if (mainVisibility == VISIBLE) {
//                    mDrawerLayout.openDrawer(GravityCompat.START);
//                    return true;
//                } else if (mainVisibility == GONE && partsVisibility == VISIBLE){
//                    closePartsFragment();
//                }
                break;

            case R.id.select_view:
                mMenu.findItem(R.id.select_view).setChecked(true);
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_view)).apply();
                // Set visibility of action icons
                contextualizeMenu();
//                // Deselect the last view selected
//                mainFragment.deselectViews();
//                partsFragment.deselectViews();
//                selectedLesson_id = -1;
//                selectedLessonPart_id = -1;
                Log.d(TAG, "View mode selected");
                break;

            case R.id.select_create:
                mMenu.findItem(R.id.select_create).setChecked(true);
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_create)).apply();
                // Set visibility of action icons
                contextualizeMenu();
                Log.v(TAG, "Create mode selected");
                break;

            case R.id.action_refresh:
//                mainFragment.setLoadingIndicator(true);
//                MyFirebaseUtilities myFirebaseUtilities = new MyFirebaseUtilities(this, mFirestoreDatabase, mUserUid);
//                myFirebaseUtilities.refreshDatabase(databaseVisibility);
//                deselectViews();
                break;

            case R.id.action_edit:
                Log.d(TAG, "Deletion action selected");
                // Try to action first on the more specific item
//                if (selectedLessonPart_id != -1) {
//                    editLessonPart(selectedLessonPart_id);
//                } else if (selectedLesson_id != -1) {
//                    editLesson(selectedLesson_id);
//                } else {
//                    Toast.makeText(this,
//                            "Please, select an item to delete!", Toast.LENGTH_LONG).show();
//                }
                break;

            case R.id.action_upload:
//                mainFragment.setLoadingIndicator(true);
//                if (selectedLesson_id != -1) {
//                    myFirebaseUtilities = new MyFirebaseUtilities(this, mFirestoreDatabase, mUserUid);
//                    myFirebaseUtilities.uploadDatabase(selectedLesson_id);
//                } else {
//                    Toast.makeText(this,
//                            "Please, select a lesson to upload!", Toast.LENGTH_LONG).show();
//                }
                break;

            case R.id.action_delete:
                Log.d(TAG, "Deletion action selected");
//                // Try to action first on the more specific item
//                if (selectedLessonPart_id != -1) {
//                    deleteLessonPart(selectedLessonPart_id);
//                } else if (selectedLesson_id != -1) {
//                    deleteLesson(selectedLesson_id);
//                } else {
//                    Toast.makeText(this,
//                            "Please, select an item to delete!", Toast.LENGTH_LONG).show();
//                }
                break;

            case R.id.action_delete_from_cloud:
                Log.d(TAG, "Delete from Cloud action selected");
//                // Try to action first on the more specific item
//                if  (selectedLesson_id != -1) {
//                    deleteLessonFromCloud(selectedLesson_id);
//                } else {
//                    Toast.makeText(this,
//                            "Please, select a lesson to delete from Cloud!", Toast.LENGTH_LONG).show();
//                }
                break;

            case R.id.action_insert_fake_data:
//                Log.d(TAG, "Insert fake data action selected");
//                TestUtil.insertFakeData(this);
//                Toast.makeText(this,
//                        "Fake data inserted!", Toast.LENGTH_LONG).show();
                break;

            case R.id.action_cancel:
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    // Set the visibility of the options according to the app state
    private void contextualizeMenu() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String modeOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        Log.v(TAG,"contextualizeMenu modeOption:" + modeOption + " databaseVisibility:" +
                databaseVisibility);

//        if (modeOption.equals(this.getString(R.string.pref_mode_create))) {
//            mMenu.findItem(R.id.action_delete).setVisible(true);
//        } else {
            mMenu.findItem(R.id.action_delete).setVisible(false);
//        }



        if (databaseVisibility.equals(USER_DATABASE) &&
                modeOption.equals(this.getString(R.string.pref_mode_create))) {
            //mMenu.findItem(R.id.action_insert_fake_data).setVisible(true);
            mMenu.findItem(R.id.action_edit).setVisible(true);
            mButton.setVisibility(VISIBLE);
        } else {
            //mMenu.findItem(R.id.action_insert_fake_data).setVisible(false);
            mMenu.findItem(R.id.action_edit).setVisible(false);
            mButton.setVisibility(GONE);
        }

//        if (databaseVisibility.equals(USER_DATABASE) &&
//                modeOption.equals(this.getString(R.string.pref_mode_create)) &&
//                !mUsername.equals(ANONYMOUS)) {
//            mMenu.findItem(R.id.action_delete_from_cloud).setVisible(true);
//            mMenu.findItem(R.id.action_upload).setVisible(true);
//        } else {
            mMenu.findItem(R.id.action_delete_from_cloud).setVisible(false);
            mMenu.findItem(R.id.action_upload).setVisible(false);
//        }



//        if (!mUsername.equals(ANONYMOUS)) {
//            mMenu.findItem(R.id.action_refresh).setVisible(true);
//        } else {
            mMenu.findItem(R.id.action_refresh).setVisible(false);
//        }


        // Set the drawer menu icon according to views
//        if (mainVisibility == VISIBLE) {
//            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
//        } else {
//            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
//        }

        mMenu.findItem(R.id.action_insert_fake_data).setVisible(false);

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
