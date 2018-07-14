package com.example.androidstudio.capstoneproject.ui;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.Lesson;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.widget.LessonsWidgetProvider;
import com.example.androidstudio.capstoneproject.widget.ListRemoteViewsFactory;

import java.util.ArrayList;
import java.util.List;


public class MainFragment extends Fragment implements
        LessonsListAdapter.ListItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {


    private static final String TAG = MainFragment.class.getSimpleName();

    // Fields for handling the saving and restoring of view state
    private static final String RECYCLER_VIEW_STATE = "recyclerViewState";
    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";

    private static final String SELECTED_LESSON_ID = "selectedLessonId";
    private static final String DATABASE_VISIBILITY = "databaseVisibility";

    private static final String LOADING_INDICATOR = "loadingIndicator";


    // Loader id
    private static final int ID_LESSONS_LOADER = 1;
    private static final int ID_GROUP_LESSONS_LOADER = 10;

    // State vars
    private long selectedLesson_id;
    private String databaseVisibility;
    private boolean loadingIndicator;

    // Views
    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicatorView;
    private RecyclerView mClassesList;
    private View mSelectedView;

    private LessonsListAdapter mAdapter;
    private Context mContext;

    AppWidgetManager appWidgetManager;

    // Interfaces for communication with the main activity (sending data)
    OnLessonListener mLessonCallback;
    OnIdlingResourceListener mIdlingCallback;

    // Interfaces for communication with the main activity (sending data)
    public interface OnLessonListener {
        void onLessonSelected(long _id);
        void onLessonClicked(long _id);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        try {
            mIdlingCallback = (OnIdlingResourceListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnIdlingResourceListener");
        }
        try {
            mLessonCallback = (OnLessonListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnLessonListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // recovering the instance state
        if (savedInstanceState != null) {
            selectedLesson_id = savedInstanceState.getLong(SELECTED_LESSON_ID);
            databaseVisibility = savedInstanceState.getString(DATABASE_VISIBILITY);
            loadingIndicator = savedInstanceState.getBoolean(LOADING_INDICATOR);
            //lessonTitle = savedInstanceState.getString(LESSON_TITLE);
        } else {
            // Initialize the state vars
            selectedLesson_id = -1;
            // Recover the local user uid for handling the database global consistency
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            databaseVisibility = sharedPreferences.getString(DATABASE_VISIBILITY, USER_DATABASE);
            loadingIndicator = false;
        }



        // Query the database and set the adapter with the cursor data
        if (null != getActivity()) {
            // Get the support loader manager to init a new loader for this fragment, according to
            // the table being queried
            if (databaseVisibility.equals(USER_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_LESSONS_LOADER, null,
                        this);
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_GROUP_LESSONS_LOADER,
                        null, this);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        // Inflate the fragment view
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);


        mErrorMessageDisplay = rootView.findViewById(R.id.tv_error_message_display);
        mLoadingIndicatorView = rootView.findViewById(R.id.pb_loading_indicator);
        mClassesList = rootView.findViewById(R.id.rv_lessons);

        // recovering the instance state
        if (savedInstanceState != null) {
            loadingIndicator = savedInstanceState.getBoolean(LOADING_INDICATOR);
        } else {
            loadingIndicator = false;
        }

        if (loadingIndicator) {
            mLoadingIndicatorView.setVisibility(View.VISIBLE);
        } else {
            mLoadingIndicatorView.setVisibility(View.GONE);
        }

        // Set the layout of the recycler view
        int nColumns = numberOfColumns();
        GridLayoutManager layoutManager = new GridLayoutManager(mContext, nColumns);
        mClassesList.setLayoutManager(layoutManager);
        mClassesList.setHasFixedSize(true);
        // Set the adapter
        mAdapter = new LessonsListAdapter(this);
        mClassesList.setAdapter(mAdapter);

        // This is loading the saved position of the recycler view.
        // There is also a call on the post execute method in the loader, for updating the view.
        if(savedInstanceState != null) {
            Log.d(TAG, "recovering savedInstanceState");
            Parcelable recyclerViewState = savedInstanceState.getParcelable(RECYCLER_VIEW_STATE);
            mClassesList.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }


        // Return root view
        return rootView;
    }


    // This method is saving the position of the recycler view
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {

        Parcelable recyclerViewState = mClassesList.getLayoutManager().onSaveInstanceState();
        savedInstanceState.putParcelable(RECYCLER_VIEW_STATE, recyclerViewState);

        savedInstanceState.putLong(SELECTED_LESSON_ID, selectedLesson_id);
        savedInstanceState.putString(DATABASE_VISIBILITY, databaseVisibility);

        savedInstanceState.putBoolean(LOADING_INDICATOR, loadingIndicator);

        super.onSaveInstanceState(savedInstanceState);
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
    public void onListItemLongClick(View view, int clickedItemIndex, long lesson_id, String lessonName) {

        Log.d(TAG, "onListItemLongClick lessonName:" + lessonName);

        // If the actual view is selected, return
        if (view.isSelected()) {
            return;
        }

        // Deselect the last view selected
        deselectViews();

        // Select the view if the app is in create mode
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        String queryOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        if (queryOption.equals(this.getString(R.string.pref_mode_create))) {
            // Select the actual view
            view.setSelected(true);
            mAdapter.setSelectedItemId(selectedLesson_id);
            // Save a reference to the view
            mSelectedView = view;
            // Save the _id of the lesson selected
            selectedLesson_id = lesson_id;
            // Save in Main Activity
            mLessonCallback.onLessonSelected(selectedLesson_id);
        }

    }

    // Helper method for calc the number of columns based on screen
    private int numberOfColumns() {

        DisplayMetrics displayMetrics = new DisplayMetrics();

        if(null != getActivity()) {
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        }

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

        Log.d(TAG, "onCreateLoader loaderId:" + loaderId);

        switch (loaderId) {

            case ID_LESSONS_LOADER:
                /* URI for all rows of lessons data in our "my_lessons" table */
                Uri lessonsQueryUri = LessonsContract.MyLessonsEntry.CONTENT_URI;

                return new CursorLoader(mContext,
                        lessonsQueryUri,
                        null,
                        null,
                        null,
                        null);


            case ID_GROUP_LESSONS_LOADER:
                /* URI for all rows of lessons data in our "my_lessons" table */
                Uri groupLessonsQueryUri = LessonsContract.GroupLessonsEntry.CONTENT_URI;

                return new CursorLoader(mContext,
                        groupLessonsQueryUri,
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

        // Pass the data to the adapter
        mAdapter.swapCursor(data, databaseVisibility);
        mAdapter.setSelectedItemId(selectedLesson_id);

        if (data == null) {
            showErrorMessage();
        } else {
            showLessonsDataView();
        }

        updateWidget(data);

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
        mAdapter.swapCursor(null, databaseVisibility);
    }

    /**
     * This method will make the View for data visible and hide the error message.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showLessonsDataView() {
        // First, make sure the error is invisible
        mErrorMessageDisplay.setVisibility(View.GONE);
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
        Log.d(TAG, "showErrorMessage");
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
    public void onListItemClick(View view, int clickedItemIndex, long lesson_id, String lessonName) {

        Log.d(TAG, "onListItemClick lessonName:" + lessonName);

        // If the actual or other view view is selected, deselect it and return
        if (view.isSelected() || selectedLesson_id >= 0) {
            view.setSelected(false);
            deselectViews();
            return;
        }

        // Inform the MainActivity
        mLessonCallback.onLessonClicked(lesson_id);

    }


    // Helper function to update the widget
    // It will make a JSON string with all the lesson titles and set the widget provider
    private void updateWidget(Cursor mCursor) {

        if (!(mCursor != null && (mCursor.getCount() > 0)) ){
            return;
        }

        int nRows = mCursor.getCount();
        mCursor.moveToFirst();

        // Get all the parts and sore all image cloud uri's in an array of Image instances
        List<Lesson> lessons = new ArrayList<>();
        Lesson lesson;

        if (nRows > 0) {
            do {

                Long lessonId = null;
                String lessonTitle = null;

                if (databaseVisibility.equals(USER_DATABASE)) {
                    lessonId = mCursor.getLong(mCursor.
                            getColumnIndex(LessonsContract.MyLessonsEntry._ID));
                    lessonTitle = mCursor.getString(mCursor.
                            getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE));
                } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                    lessonId = mCursor.getLong(mCursor.
                            getColumnIndex(LessonsContract.GroupLessonsEntry._ID));
                    lessonTitle = mCursor.getString(mCursor.
                            getColumnIndex(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TITLE));
                }

                if (lessonTitle != null) {
                    // Set the values in the Image instance
                    lesson = new Lesson();
                    lesson.setLesson_id(lessonId);
                    lesson.setLesson_title(lessonTitle);
                    lessons.add(lesson);
                }
                // get the next image
            } while (mCursor.moveToNext());
            // don't close the cursor: it will be used by the adapter!
        }

        Log.d(TAG, "updateWidget lessons:" + lessons.toString());

        // send the data
        ListRemoteViewsFactory.setWidgetProviderData(lessons);


        //Trigger data update to handle the View widgets and force a data refresh
        appWidgetManager = AppWidgetManager.getInstance(mContext);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(mContext, LessonsWidgetProvider.class));
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
        LessonsWidgetProvider.updateLessonsWidgets(mContext, appWidgetManager, appWidgetIds);

    }


    public interface OnIdlingResourceListener {
        void onIdlingResource(Boolean value);
    }


    public void setDatabaseVisibility(String dbVisibility) {
        databaseVisibility = dbVisibility;

        Log.d(TAG,"setDatabaseVisibility databaseVisibility:" + databaseVisibility);

        // Query the database and set the adapter with the cursor data
        if (null != getActivity()) {

            getActivity().getSupportLoaderManager().destroyLoader(ID_LESSONS_LOADER);
            getActivity().getSupportLoaderManager().destroyLoader(ID_GROUP_LESSONS_LOADER);

            if (databaseVisibility.equals(USER_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_LESSONS_LOADER,
                        null, this);
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_GROUP_LESSONS_LOADER,
                        null, this);
            }
        }
    }


    public void deselectViews() {
        // Deselect the last view selected
        if (null != mSelectedView) {
            mSelectedView.setSelected(false);
            mSelectedView = null;
        }

        // change the state to deselected
        selectedLesson_id = -1;

        // Force deselecting all views
        int i = 0;
        while (mClassesList.getChildAt(i) != null) {
            mClassesList.getChildAt(i).setSelected(false);
            i++;
        }
    }

    public void setLoadingIndicator(Boolean value) {

        loadingIndicator = value;

        if (mLoadingIndicatorView != null) {
            if (value) {
                mLoadingIndicatorView.setVisibility(View.VISIBLE);
            } else {
                mLoadingIndicatorView.setVisibility(View.INVISIBLE);
            }
        }
    }

}
