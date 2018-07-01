package com.example.androidstudio.capstoneproject.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;


public class MainFragment extends Fragment implements
        LessonsListAdapter.ListItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {


    private static final String TAG = MainFragment.class.getSimpleName();

    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;
    private RecyclerView mClassesList;
    private GridLayoutManager layoutManager;

    private View mSelectedView;
    private long selectedLesson_id;

    private LessonsListAdapter mAdapter;
    private Context mContext;

    // Fields for handling the saving and restoring of view state
    private static final String RECYCLER_VIEW_STATE = "recyclerViewState";
    private static final String SELECTED_LESSON_ID = "selectedLessonId";

    private Parcelable recyclerViewState;

    private static final int ID_LESSONS_LOADER = 1;

    // Callbacks to the main activity
    OnLessonListener mLessonCallback;
    OnIdlingResourceListener mIdlingCallback;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.v(TAG, "onCreateView");

        // Inflate the Ingredients fragment layout
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mContext = getContext();

        mErrorMessageDisplay = rootView.findViewById(R.id.tv_error_message_display);
        mLoadingIndicator = rootView.findViewById(R.id.pb_loading_indicator);
        mClassesList = rootView.findViewById(R.id.rv_lessons);

        // Set the layout manager
        int nColumns = numberOfColumns();
        layoutManager = new GridLayoutManager(mContext, nColumns);
        mClassesList.setLayoutManager(layoutManager);

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
        mClassesList.setHasFixedSize(true);

        /*
         * The Adapter is responsible for displaying each item in the list.
         */
        mAdapter = new LessonsListAdapter(this);
        mClassesList.setAdapter(mAdapter);

        // This is loading the saved position of the recycler view.
        // There is also a call on the post execute method in the loader, for updating the view.
        if(savedInstanceState != null) {
            Log.v(TAG, "recovering savedInstanceState");
            recyclerViewState = savedInstanceState.getParcelable(RECYCLER_VIEW_STATE);
            mClassesList.getLayoutManager().onRestoreInstanceState(recyclerViewState);
            selectedLesson_id = savedInstanceState.getLong(SELECTED_LESSON_ID);
        } else {
            selectedLesson_id = -1;
        }

        mLoadingIndicator.setVisibility(View.VISIBLE);

        // Return root view
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Query the database and set the adapter with the cursor data
        if (null != getActivity()) {
            getActivity().getSupportLoaderManager().initLoader(ID_LESSONS_LOADER, null, this);
        }
    }

    // This method is saving the position of the recycler view
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        Parcelable recyclerViewState = mClassesList.getLayoutManager().onSaveInstanceState();
        savedInstanceState.putParcelable(RECYCLER_VIEW_STATE, recyclerViewState);
        savedInstanceState.putLong(SELECTED_LESSON_ID, selectedLesson_id);

        super.onSaveInstanceState(savedInstanceState);
    }


    /**
     * This method will make the View for data visible and hide the error message.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showLessonsDataView() {
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
    public void onListItemClick(View view, int clickedItemIndex, long lesson_id, String lessonName) {

        Log.v(TAG, "onListItemClick lessonName:" + lessonName);

        // If the actual view is selected, deselect it and return
        if (view.isSelected()) {
            view.setSelected(false);
            deselectViews();
            return;
        }

        // Or clicks on the view
        mLessonCallback.onLessonClicked(lesson_id);

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

        Log.v(TAG, "onListItemLongClick lessonName:" + lessonName);

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
            // Save a reference to the view
            mSelectedView = view;
            // Save the _id of the lesson selected
            selectedLesson_id = lesson_id;
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

        // Send to the main activity the order to setting the idling resource state
        mIdlingCallback.onIdlingResource(true);

        // Pass the data to the adapter
        setCursor(data);
        mAdapter.setSelectedItemId(selectedLesson_id);

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
        this.setCursor(null);
    }


    // Interfaces for communication with the main activity (sending data)
    public interface OnLessonListener {
        void onLessonSelected(long _id);
        void onLessonClicked(long _id);
    }

    public interface OnIdlingResourceListener {
        void onIdlingResource(Boolean value);
    }

    // Functions for receiving the data from main activity
    // Receives the data from the main activity
    public void setCursor(Cursor cursor) {

        mLoadingIndicator.setVisibility(View.INVISIBLE);

        // Try to handle error on loading
        if(cursor == null){
            showErrorMessage();
        } else {
            // Saves a reference to the cursor
            // Set the data for the adapter
            mAdapter.setLessonsCursorData(cursor);
            showLessonsDataView();

        }
    }

    public void deselectViews() {
        // Deselect the last view selected
        if (null != mSelectedView) {
            mSelectedView.setSelected(false);
            mSelectedView = null;
        }
        selectedLesson_id = -1;
    }

}
