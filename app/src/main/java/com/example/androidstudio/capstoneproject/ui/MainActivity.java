package com.example.androidstudio.capstoneproject.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidstudio.capstoneproject.IdlingResource.SimpleIdlingResource;
import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.DatabaseUtil;
import com.example.androidstudio.capstoneproject.data.Lesson;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.data.LessonsDbHelper;
import com.example.androidstudio.capstoneproject.data.TestUtil;

import java.io.IOException;
import java.net.URL;
import java.util.List;

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
 * the item will be deleted after a confirmation. If the user select to edit and then click, it will
 * open an activity EditLessonActivity to edit that item.
 *
 *
 *
 *
 */
public class MainActivity extends AppCompatActivity implements
        LessonsListAdapter.ListItemClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener,
        LoaderManager.LoaderCallbacks<Cursor> {


    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;
    private RecyclerView mClassesList;

    private View mSelectedView;

    // flag for preference updates
    private static boolean flag_preferences_updates = false;

    private LessonsListAdapter mAdapter;
    private Context mContext;

    // holds the contextual menu
    private ActionMode mActionMode;

    // Fields for handling the saving and restoring of view state
    private static final String RECYCLER_VIEW_STATE = "recyclerViewState";
    private Parcelable recyclerViewState;

    private static final int ID_LESSONS_LOADER = 1;

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

        mErrorMessageDisplay = findViewById(R.id.tv_error_message_display);
        mLoadingIndicator = findViewById(R.id.pb_loading_indicator);
        mClassesList = findViewById(R.id.rv_classes);

        // Set the layout manager
        int nColumns = numberOfColumns();
        GridLayoutManager layoutManager = new GridLayoutManager(this, nColumns);
        mClassesList.setLayoutManager(layoutManager);

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
        mClassesList.setHasFixedSize(true);

        /*
         * The GreenAdapter is responsible for displaying each item in the list.
         */
        mAdapter = new LessonsListAdapter(this);
        mClassesList.setAdapter(mAdapter);


        // Get the IdlingResource instance
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

        // Insert data for testing
        //TestUtil.insertFakeData(this);

        mLoadingIndicator.setVisibility(View.VISIBLE);

        // Query the database and set the adapter with the cursor data
        getSupportLoaderManager().initLoader(ID_LESSONS_LOADER, null, this);

    }


    // This method is saving the position of the recycler view
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Parcelable recyclerViewState = mClassesList.getLayoutManager().onSaveInstanceState();
        savedInstanceState.putParcelable(RECYCLER_VIEW_STATE, recyclerViewState);
        super.onSaveInstanceState(savedInstanceState);
    }

    // This method is loading the saved position of the recycler view
    // There is also a call on the post execute method in the loader, for updating the view
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        recyclerViewState = savedInstanceState.getParcelable(RECYCLER_VIEW_STATE);
        mClassesList.getLayoutManager().onRestoreInstanceState(recyclerViewState);
    }

    // In onStart, if preferences have been changed, refresh the view
    @Override
    protected void onStart() {
        super.onStart();

        Log.v("onStart", "on start");

        if (flag_preferences_updates) {
            Log.v("onStart", "preferences changed");
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String queryOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        if (queryOption.equals(this.getString(R.string.pref_mode_view))) {
            menu.findItem(R.id.select_view).setChecked(true);
        }

        if (queryOption.equals(this.getString(R.string.pref_mode_create))) {
            menu.findItem(R.id.select_create).setChecked(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemThatWasClickedId = item.getItemId();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        switch (itemThatWasClickedId) {

            case R.id.action_refresh:
                Toast.makeText(this, "Reloading the data", Toast.LENGTH_LONG)
                        .show();
                refreshActivity();
                break;

            case R.id.select_view:
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_view)).apply();
                Log.v(TAG, "View mode selected");
                break;

            case R.id.select_create:
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_create)).apply();
                Log.v(TAG, "Create mode selected");
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Interface implementation for the contextual menu
     */
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {


        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;// Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            switch (item.getItemId()) {
                case R.id.action_delete:
                    //deleteCurrentItem();
                    mode.finish(); // Action picked, so close the CAB
                    mSelectedView.setSelected(false);
                    return true;
                default:
                    return false;
            }

        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mSelectedView.setSelected(false);
        }
    };


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        flag_preferences_updates = true;

        String lessonsQueryOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        Log.v(TAG, "onSharedPreferenceChanged lessonsQueryOption:" + lessonsQueryOption);

        updateView();

    }


    /**
     * This method will make the View for data visible and hide the error message.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showClassesDataView() {
        // First, make sure the error is invisible
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);
        // Then, make sure the JSON data is visible
        mClassesList.setVisibility(View.VISIBLE);
    }


    /**
     * This method will make the error message visible and hide data View.
     *
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showErrorMessage() {
        // First, hide the currently visible data
        mClassesList.setVisibility(View.INVISIBLE);
        // Then, show the error
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }


    /**
     * This is where we receive our callback from the classes list adapter
     * {@link com.example.androidstudio.capstoneproject.ui.LessonsListAdapter.ListItemClickListener}
     *
     * This callback is invoked when you click on an item in the list.
     *
     * @param clickedItemIndex Index in the list of the item that was clicked.
     */
    @Override
    public void onListItemClick(int clickedItemIndex, int lesson_id, String lessonName) {

        Log.v(TAG, "onListItemClick lessonName:" + lessonName);

//        // Start RecipeDetail activity passing the specific recipe JSON string
//        Context context = MainActivity.this;
//        Class destinationActivity = RecipeDetailActivity.class;
//        Intent startChildActivityIntent = new Intent(context, destinationActivity);
//
//        startChildActivityIntent.putExtra("clickedItemIndex", clickedItemIndex);
//        startChildActivityIntent.putExtra("recipeName", recipeName);
//        startChildActivityIntent.putExtra("ingredientsJSONString", ingredientsJSONString);
//        startChildActivityIntent.putExtra("stepsJSONString", stepsJSONString);
//        startChildActivityIntent.putExtra("servings", servings);
//
//        startActivity(startChildActivityIntent);

    }


    /**
     * This is where we receive our callback from the classes list adapter
     * {@link com.example.androidstudio.capstoneproject.ui.LessonsListAdapter.ListItemClickListener}
     *
     * This callback is invoked when you long click on an item in the list.
     *
     * @param clickedItemIndex Index in the list of the item that was clicked.
     */
    @Override
    public void onListItemLongClick(View view, int clickedItemIndex, int lesson_id, String lessonName) {

        Log.v(TAG, "onListItemLongClick lessonName:" + lessonName);

        if (mActionMode != null) {
            return;
        }

        // Start the CAB using the ActionMode.Callback defined above
        mActionMode = this.startActionMode(mActionModeCallback);
        mSelectedView = view;
        mSelectedView.setSelected(true);

    }



    // Helper method for calc the number of columns based on screen
    private int numberOfColumns() {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        // You can change this divider to adjust the size of the recipe card
        int widthDivider = 600;
        int width = displayMetrics.widthPixels;
        int nColumns = width / widthDivider;
        if (nColumns < 1) return 1;

        return nColumns;
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

        Log.v(TAG, "onCreateLoader loaderId:" + loaderId);

        switch (loaderId) {

            case ID_LESSONS_LOADER:
                /* URI for all rows of lessons data in our "my_lessons" table */
                Uri lessonsQueryUri = LessonsContract.MyLessonsEntry.CONTENT_URI;

                return new CursorLoader(this,
                        lessonsQueryUri,
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

        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(true);
        }

        mLoadingIndicator.setVisibility(View.INVISIBLE);

        // Try to handle error on loading
        if(data == null){
            showErrorMessage();
        } else {
            // Saves a reference to the cursor
            // Set the data for the adapter
            mAdapter.setLessonsCursorData(data);
            showClassesDataView();
        }

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
         * Since this Loader's data is now invalid, we need to clear the Adapter that is
         * displaying the data.
         */
        mAdapter.setLessonsCursorData(null);
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

}
