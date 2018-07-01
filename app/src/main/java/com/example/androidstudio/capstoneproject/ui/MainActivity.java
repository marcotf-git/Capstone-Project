package com.example.androidstudio.capstoneproject.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.test.espresso.IdlingResource;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.androidstudio.capstoneproject.IdlingResource.SimpleIdlingResource;
import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.data.TestUtil;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


/**
 * The app has two modes: 'view' mode and 'create' mode.
 * If the user select 'create' mode, it will show only the content created by the user,
 * plus the options to create, edit and delete, in the action items of the app bar.
 * If the user shows 'view' mode, it will show the content that is synced with an remote server.
 * This class will start showing the lessons' titles queried from a local database, which is a copy
 * of the remote database. This corresponds to the 'view' mode, which can be selected in the
 * overflow menu.
 * According to the modes of the app, the local database has two tables.
 * The table "group_content" is a copy of the remote database.
 * The table "my_content" is the content created by the user.
 * The database and its tables are handled by a content provider LessonsContentProvider.
 * The provider is queried by a cursor loader, which returns a cursor object.
 * In the 'view' mode, the view layout will be the activity_main.xml, which has a RecyclerView,
 * populated with a GridLayoutManager and a custom adapter LessonsListAdapter.
 * The cursor provided by the loader is passed to the adapter with the data that will be shown.
 * In the 'view' mode, if the user shows the option to sync the database, it will call a task ....
 * In the 'create' mode, if the user selects the option to add a lesson title, it will open another
 * activity AddLessonActivity to add the title. If the user select to delete and then click on an item,
 * the item will be deleted after a confirmation. The confirmation is handled by a fragment.
 * If the user select to edit and then click, it will open an activity EditLessonActivity to edit that item.
 *
 *
 *
 *
 */
public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        MainFragment.OnLessonListener,
        MainFragment.OnIdlingResourceListener,
        PartsFragment.OnLessonPartListener,
        PartsFragment.OnIdlingResourceListener,
        DeleteLessonDialogFragment.DeleteLessonDialogListener {


    private static final String TAG = MainActivity.class.getSimpleName();

    // Final string to store state information
    private static final String CLICKED_LESSON_ID = "clickedLessonId";
    private static final String SELECTED_LESSON_ID = "selectedLessonId";
    private static final String CLICKED_LESSON_PART_ID = "clickedLessonPartId";
    private static final String SELECTED_LESSON_PART_ID = "selectedLessonPartId";
    private static final String MAIN_VISIBILITY = "mainVisibility";
    private static final String PARTS_VISIBILITY = "partsVisibility";

    private long clickedLesson_id;
    private long selectedLesson_id;
    private long clickedLessonPart_id;
    private long selectedLessonPart_id;

    private int mainVisibility;
    private int partsVisibility;

    // Menus and buttons
    private Menu mMenu;
    private Toolbar mToolbar;
    private ActionBar actionBar;
    private FloatingActionButton mButton;

    // flag for preference updates
    private static boolean flag_preferences_updates = false;

    private Context mContext;

    private FrameLayout lessonsContainer;
    private FrameLayout partsContainer;

    private MainFragment mainFragment;
    private PartsFragment partsFragment;

    // The Idling Resource which will be null in production.
    @Nullable
    private SimpleIdlingResource mIdlingResource;


    /**
     * Only called from test, creates and returns a new {@link SimpleIdlingResource}.
     */
    @VisibleForTesting
    @NonNull
    public IdlingResource getIdlingResource() {
        if (mIdlingResource == null) {
            mIdlingResource = new SimpleIdlingResource();
        }
        return mIdlingResource;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        // Add the toolbar as the default app bar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        // Get a support ActionBar corresponding to this toolbar
        actionBar = getSupportActionBar();
        if (null != actionBar) {
            // Disable the Up button
            actionBar.setDisplayHomeAsUpEnabled(false);
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
                Log.d(TAG, "onClick clickedLesson_id:" + clickedLesson_id);
                // Try to action first on the more specific item
                if (partsContainer.getVisibility() == GONE) {
                    // Create a new intent to start an AddLessonActivity
                    Intent addLessonIntent = new Intent(MainActivity.this, AddLessonActivity.class);
                    startActivity(addLessonIntent);
                } else if (clickedLesson_id >= 0) {
                    // Create a new intent to start an AddLessonPartActivity
                    Intent addLessonPartIntent = new Intent(MainActivity.this, AddLessonPartActivity.class);
                    addLessonPartIntent.putExtra(CLICKED_LESSON_ID, clickedLesson_id);
                    startActivity(addLessonPartIntent);
                } else {
                    // Create a new intent to start an AddLessonActivity
                    Intent addLessonIntent = new Intent(MainActivity.this, AddLessonActivity.class);
                    startActivity(addLessonIntent);
                }
                // Clear selections on fragments
                mainFragment.deselectViews();
                partsFragment.deselectViews();
            }
        });

        // Initialize the activity views
        lessonsContainer = findViewById(R.id.lessons_container);
        partsContainer = findViewById(R.id.parts_container);

        // Initialize the data vars for this class
        if (null == savedInstanceState) {
            clickedLesson_id = -1;
            selectedLesson_id = -1;
            clickedLessonPart_id = -1;
            selectedLessonPart_id = -1;
            // Phone visibility
            mainVisibility = VISIBLE;
            partsVisibility = GONE;
        } else {
            clickedLesson_id = savedInstanceState.getLong(CLICKED_LESSON_ID);
            selectedLesson_id = savedInstanceState.getLong(SELECTED_LESSON_ID);
            clickedLessonPart_id = savedInstanceState.getLong(CLICKED_LESSON_PART_ID);
            selectedLessonPart_id = savedInstanceState.getLong(SELECTED_LESSON_PART_ID);
            mainVisibility = savedInstanceState.getInt(MAIN_VISIBILITY);
            partsVisibility = savedInstanceState.getInt(PARTS_VISIBILITY);

            Log.d(TAG, "recovering mainVisibility:" + mainVisibility);
            Log.d(TAG, "recovering partsVisibility:" + partsVisibility);

        }

        // Initialize the fragments and its views
        FragmentManager fragmentManager = getSupportFragmentManager();
        // Only create fragment when needed
        if (savedInstanceState == null) {

            Log.v(TAG, "creating MainFragment");
            mainFragment = new MainFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.lessons_container, mainFragment, "MainFragment")
                    .commit();

            Log.v(TAG, "creating PartsFragment");
            partsFragment = new PartsFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.parts_container, partsFragment, "PartsFragment")
                    .commit();

        } else {

            mainFragment = (MainFragment) fragmentManager.findFragmentByTag("MainFragment");
            partsFragment = (PartsFragment) fragmentManager.findFragmentByTag("PartsFragment");

            lessonsContainer.setVisibility(mainVisibility);
            partsContainer.setVisibility(partsVisibility);

            if (partsVisibility == VISIBLE) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        Log.d(TAG, "lessonsContainer visibility:" + lessonsContainer.getVisibility());
        Log.d(TAG, "partsContainer visibility:" + partsContainer.getVisibility());

        // Get the IdlingResource instance for testing
        getIdlingResource();

        /*
         * The IdlingResource is null in production as set by the @Nullable annotation which means
         * the value is allowed to be null.
         *
         * If the idle state is true, Espresso can perform the next action.
         * If the idle state is false, Espresso will wait until it is true before
         * performing the next action.
         */
        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(false);
        }

        // Load data from http with the Retrofit library
        //Controller controller = new Controller();
        //controller.start(this);

    }


    // This method is saving the position of the recycler view
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putLong(CLICKED_LESSON_ID, clickedLesson_id);
        savedInstanceState.putLong(SELECTED_LESSON_ID, selectedLesson_id);
        savedInstanceState.putLong(CLICKED_LESSON_PART_ID, clickedLessonPart_id);
        savedInstanceState.putLong(SELECTED_LESSON_PART_ID, selectedLessonPart_id);

        Log.d(TAG, "saving mainVisibility:" + mainVisibility);
        Log.d(TAG, "saving partsVisibility:" + partsVisibility);

        savedInstanceState.putInt(MAIN_VISIBILITY, lessonsContainer.getVisibility());
        savedInstanceState.putInt(PARTS_VISIBILITY, partsContainer.getVisibility());

        super.onSaveInstanceState(savedInstanceState);
    }


    // In onStart, if preferences have been changed, refresh the view
    @Override
    protected void onStart() {
        super.onStart();

        Log.v("onStart", "on start");

        if (flag_preferences_updates) {
            Log.d("onStart", "preferences changed");
            updateView();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * This method sets the option menu that choose which kind of movie search will be executed,
     * if popular or top rated
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onPrepareOptionsMenu(menu);

        // Save a reference to the menu
        mMenu = menu;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String queryOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        if (queryOption.equals(this.getString(R.string.pref_mode_view))) {
            menu.findItem(R.id.select_view).setChecked(true);
            // Prepare the visibility of the creation action items
            mMenu.findItem(R.id.action_delete).setVisible(false);
            mMenu.findItem(R.id.action_edit).setVisible(false);
            mMenu.findItem(R.id.action_upload).setVisible(false);
            mMenu.findItem(R.id.action_refresh).setVisible(true);
            mButton.setVisibility(GONE);
        }

        if (queryOption.equals(this.getString(R.string.pref_mode_create))) {
            menu.findItem(R.id.select_create).setChecked(true);
            // Prepare the visibility of the creation action items
            mMenu.findItem(R.id.action_delete).setVisible(true);
            mMenu.findItem(R.id.action_edit).setVisible(true);
            mMenu.findItem(R.id.action_upload).setVisible(true);
            mMenu.findItem(R.id.action_refresh).setVisible(false);
            mButton.setVisibility(VISIBLE);
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemThatWasClickedId = item.getItemId();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        switch (itemThatWasClickedId) {

            case R.id.select_view:
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_view)).apply();
                // Set visibility of action icons
                mMenu.findItem(R.id.action_delete).setVisible(false);
                mMenu.findItem(R.id.action_edit).setVisible(false);
                mMenu.findItem(R.id.action_upload).setVisible(false);
                mMenu.findItem(R.id.action_refresh).setVisible(true);
                mButton.setVisibility(GONE);
                // Deselect the last view selected
                mainFragment.deselectViews();
                partsFragment.deselectViews();
                selectedLesson_id = -1;
                selectedLessonPart_id = -1;
                Log.d(TAG, "View mode selected");
                break;

            case R.id.select_create:
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_create)).apply();
                // Set visibility of action icons
                mMenu.findItem(R.id.action_delete).setVisible(true);
                mMenu.findItem(R.id.action_edit).setVisible(true);
                mMenu.findItem(R.id.action_upload).setVisible(true);
                mMenu.findItem(R.id.action_refresh).setVisible(false);
                mButton.setVisibility(VISIBLE);
                Log.v(TAG, "Create mode selected");
                break;

            case R.id.action_refresh:
                Toast.makeText(this, "Reloading the data", Toast.LENGTH_LONG)
                        .show();
                refreshActivity();
                break;

            case R.id.action_insert_fake_data:
                Log.d(TAG, "Insert fake data action selected");
                TestUtil.insertFakeData(this);
                    Toast.makeText(this,
                            "Fake data inserted!", Toast.LENGTH_LONG).show();
                break;

            case R.id.action_delete:
                Log.d(TAG, "Deletion action selected");
                // Try to action first on the more specific item
                if (selectedLessonPart_id != -1) {
                    deleteLessonPart(selectedLessonPart_id);
                } else if (selectedLesson_id != -1) {
                    deleteLesson(selectedLesson_id);
                } else {
                    Toast.makeText(this,
                            "Please, select an item to delete!", Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.action_edit:
                Log.d(TAG, "Deletion action selected");
                // Try to action first on the more specific item
                if (selectedLessonPart_id != -1) {
                    editLessonPart(selectedLessonPart_id);
                } else if (selectedLesson_id != -1) {
                    editLesson(selectedLesson_id);
                } else {
                    Toast.makeText(this,
                            "Please, select an item to delete!", Toast.LENGTH_LONG).show();
                }
                break;

            case android.R.id.home:
                closePartsFragment();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        flag_preferences_updates = true;
        String lessonsQueryOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));
        Log.d(TAG, "onSharedPreferenceChanged lessonsQueryOption:" + lessonsQueryOption);
        updateView();

    }

    // Reload the activity
    private void refreshActivity() {
        //Controller.clearRecipesList();
        finish();
        startActivity(getIntent());
    }


    /**
     * Helper function to reload data and update the view, called by the shared preference listener
     */
    public void updateView() {
        Log.v(TAG, "updateView");
        flag_preferences_updates = false;
    }


    /**
     * Helper function to delete data and update the view
     */
    private void deleteLesson(long _id) {

        Log.v(TAG, "deleteLesson _id:" + _id);

        // Call the fragment for showing the delete dialog
        DialogFragment deleteLessonFragment = new DeleteLessonDialogFragment();

        // Pass the _id of the lesson
        Bundle bundle = new Bundle();
        bundle.putLong("_id", _id);
        deleteLessonFragment.setArguments(bundle);

        // Show the dialog box
        deleteLessonFragment.show(getSupportFragmentManager(), "DeleteLessonDialogFragment");
    }


    private void editLesson(long _id) {

        Log.d(TAG, "editLesson _id:" + _id);

        // Create a new intent to start an EditTaskActivity
        Class destinationActivity = EditLessonActivity.class;
        Intent editLessonIntent = new Intent(mContext, destinationActivity);
        editLessonIntent.putExtra(SELECTED_LESSON_ID, _id);
        startActivity(editLessonIntent);

        // Deselect the last view selected
        mainFragment.deselectViews();
        selectedLesson_id = -1;
    }


    private void deleteLessonPart(long _id) {

        Toast.makeText(this,
                "deleteLessonPart", Toast.LENGTH_LONG).show();

    }


    private void editLessonPart(long _id) {

        Toast.makeText(this,
                "editLessonPart", Toast.LENGTH_LONG).show();

    }

    // Helper method for hiding the PartsFragment
    public void closePartsFragment() {

        Log.d(TAG, "closePartsFragment");

        // deselect the views on the fragment that will be closed
        partsFragment.deselectViews();

        // clear the reference var
        selectedLessonPart_id = -1;

        // Change the views
        partsContainer.setVisibility(GONE);
        lessonsContainer.setVisibility(VISIBLE);

        // Disable the Up button
        if (null != actionBar) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }


    // Method for receiving communication from the MainFragment
    @Override
    public void onLessonSelected(long _id) {
        selectedLesson_id = _id;
    }

    // Method for receiving communication from the MainFragment
    @Override
    public void onLessonClicked(long _id) {

        Log.d(TAG, "onLessonClicked _id:" + _id);

        clickedLesson_id = _id;

        // Show the lesson parts fragment
        lessonsContainer.setVisibility(GONE);
        partsContainer.setVisibility(VISIBLE);

        // Enable the Up button
        if (null != actionBar) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Send the data to the fragment
        partsFragment.setReferenceLesson(_id);
    }

    // Method for receiving communication from the DeleteLessonFragment
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, long _id) {

        ContentResolver contentResolver = mContext.getContentResolver();
        /* The delete method deletes the row by its _id */
        Uri uriToDelete = LessonsContract.MyLessonsEntry.CONTENT_URI.buildUpon()
                .appendPath("" + _id + "").build();

        Log.d(TAG, "onDialogPositiveClick: Uri to delete:" + uriToDelete.toString());

        int numberOfLessonsDeleted = contentResolver.delete(uriToDelete, null, null);

        if (numberOfLessonsDeleted > 0) {
            Toast.makeText(this,
                    numberOfLessonsDeleted + " item(s) removed!", Toast.LENGTH_LONG).show();

//            Snackbar mySnackbar = Snackbar.make(lessonsContainer,
//                    numberOfLessonsDeleted + " item removed", Snackbar.LENGTH_LONG);
//            mySnackbar.show();

            // Deselect the last view selected
            mainFragment.deselectViews();
            selectedLesson_id = -1;
        }

    }

    // Method for receiving communication from the DeleteLessonFragment
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Toast.makeText(mContext,
                "Canceled!", Toast.LENGTH_LONG).show();
    }

    // Receive from the MainFragment and PartsFragment the order to setting the idling resource
    @Override
    public void onIdlingResource(Boolean value) {
        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(value);
        }
    }

    // Receive communication from the PartsFragment
    @Override
    public void onPartSelected(long _id) {
        selectedLessonPart_id = _id;
    }

    // Receive communication from the PartsFragment
    @Override
    public void onPartClicked(long _id) {
        clickedLessonPart_id = _id;
    }

}
