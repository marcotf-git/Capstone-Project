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


public class PartsFragment extends Fragment implements
        PartsListAdapter.ListItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor> {


    private static final String TAG = PartsFragment.class.getSimpleName();

    // Fields for handling the saving and restoring of view state
    private static final String RECYCLER_VIEW_STATE = "recyclerViewState";
    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";

    private static final String REFERENCE_LESSON_ID = "referenceLesson_id";
    private static final String SELECTED_LESSON_PART_ID = "selectedLessonPartId";
    private static final String DATABASE_VISIBILITY = "databaseVisibility";

    // Loader id
    private static final int ID_LESSON_PARTS_LOADER = 2;
    private static final int ID_GROUP_LESSON_PARTS_LOADER = 20;

    // State vars
    private View mSelectedView; // not saved
    private long referenceLesson_id;
    private long selectedLessonPart_id;
    private String databaseVisibility;

    // Views
    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;
    private RecyclerView mPartsList;

    private PartsListAdapter mAdapter;
    private Context mContext;


    // Callbacks to send data to the main activity
    OnLessonPartListener mPartCallback;
    OnIdlingResourceListener mIdlingCallback;


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
            mPartCallback = (OnLessonPartListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnLessonPartListener");
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // recovering the instance state
        if (savedInstanceState != null) {
            selectedLessonPart_id = savedInstanceState.getLong(SELECTED_LESSON_PART_ID);
            databaseVisibility = savedInstanceState.getString(DATABASE_VISIBILITY);
            referenceLesson_id = savedInstanceState.getLong(REFERENCE_LESSON_ID);
        } else {
            // Initialize the state vars
            selectedLessonPart_id = -1;
            // Recover the local user uid for handling the database global consistency
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            databaseVisibility = sharedPreferences.getString(DATABASE_VISIBILITY, USER_DATABASE);
        }

        // Init the loader
        if (null != getActivity()) {
            if (databaseVisibility.equals(USER_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_LESSON_PARTS_LOADER, null,
                        this);
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_GROUP_LESSON_PARTS_LOADER,
                        null, this);
            }
        }
    }


//    @Override
//    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
//        super.onActivityCreated(savedInstanceState);
//
//        // Query the database and set the adapter with the cursor data
//        if (null != getActivity()) {
//            if (databaseVisibility.equals(USER_DATABASE)) {
//                getActivity().getSupportLoaderManager().initLoader(ID_LESSON_PARTS_LOADER, null, this);
//            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
//                getActivity().getSupportLoaderManager().initLoader(ID_GROUP_LESSON_PARTS_LOADER, null, this);
//            }
//        }
//    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Log.v(TAG, "onCreateView");

        // Inflate the fragment view
        View rootView = inflater.inflate(R.layout.fragment_parts, container, false);

        mErrorMessageDisplay = rootView.findViewById(R.id.tv_error_message_display);
        mLoadingIndicator = rootView.findViewById(R.id.pb_loading_indicator);
        mPartsList = rootView.findViewById(R.id.rv_parts);

        mLoadingIndicator.setVisibility(View.VISIBLE);

        // Set the layout of the recycler view
        int nColumns = numberOfColumns();
        GridLayoutManager layoutManager = new GridLayoutManager(mContext, nColumns);
        mPartsList.setLayoutManager(layoutManager);
        mPartsList.setHasFixedSize(true);
        // Set the adapter
        mAdapter = new PartsListAdapter(this);
        mPartsList.setAdapter(mAdapter);

        // This is loading the saved position of the recycler view.
        // There is also a call on the post execute method in the loader, for updating the view.
        if(savedInstanceState != null) {
            Log.v(TAG, "recovering savedInstanceState");
            Parcelable recyclerViewState = savedInstanceState.getParcelable(RECYCLER_VIEW_STATE);
            mPartsList.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }

        // Return root view
        return rootView;
    }


    // This method is saving the position of the recycler view
    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        Parcelable recyclerViewState = mPartsList.getLayoutManager().onSaveInstanceState();
        savedInstanceState.putParcelable(RECYCLER_VIEW_STATE, recyclerViewState);

        savedInstanceState.putLong(SELECTED_LESSON_PART_ID, selectedLessonPart_id);
        savedInstanceState.putLong(REFERENCE_LESSON_ID, referenceLesson_id);
        savedInstanceState.putString(DATABASE_VISIBILITY, databaseVisibility);

        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * This method will make the View for data visible and hide the error message.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showPartsDataView() {
        // First, make sure the error is invisible
        mErrorMessageDisplay.setVisibility(View.GONE);
        // Then, make sure the JSON data is visible
        mPartsList.setVisibility(View.VISIBLE);
    }


    /**
     * This method will make the error message visible and hide data View.
     *
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showErrorMessage() {
        Log.v(TAG, "showErrorMessage");
        // First, hide the currently visible data
        mPartsList.setVisibility(View.INVISIBLE);
        // Then, show the error
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    /**
     * This is where we receive our callback from the classes list adapter
     * {@link LessonsListAdapter.ListItemClickListener}
     *
     * This callback is invoked when you click on an item in the list.
     *
     * @param clickedItemIndex Index in the list of the item that was clicked.
     */
    @Override
    public void onListItemClick(View view, int clickedItemIndex, long lesson_part_id,
                                String lessonPartTitle) {

        Log.d(TAG, "onListItemClick lessonPartTitle:" + lessonPartTitle);

        // If the actual view or other view is selected, deselect it and return
        if (view.isSelected() || selectedLessonPart_id >=0) {
            view.setSelected(false);
            deselectViews();
            return;
        }

        mPartCallback.onPartClicked(lesson_part_id);
    }


    /**
     * This is where we receive our callback from the classes list adapter
     * {@link LessonsListAdapter.ListItemClickListener}
     *
     * This callback is invoked when you long click on an item in the list.
     *
     * @param clickedItemIndex Index in the list of the item that was clicked.
     */
    @Override
    public void onListItemLongClick(View view, int clickedItemIndex, long lesson_part_id,
                                    String lessonPartTitle) {

        Log.d(TAG, "onListItemLongClick lessonPartTitle:" + lessonPartTitle);

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
            selectedLessonPart_id = lesson_part_id;
            mPartCallback.onPartSelected(selectedLessonPart_id);
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
        Log.d(TAG, "onCreateLoader databaseVisibility:" + databaseVisibility);

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

        if (data != null) {
            Log.d(TAG, "onLoadFinished cursor:" + data.toString());
        } else {
            Log.e(TAG, "onLoadFinished cursor: null");
        }

        // Send to the main activity the order to setting the idling resource state
        mIdlingCallback.onIdlingResource(true);

        // Pass the data to the adapter
        setCursor(data);
        mAdapter.setSelectedItemId(selectedLessonPart_id);

        if (data == null) {
            showErrorMessage();
        } else {
            showPartsDataView();
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
        setCursor(null);
    }


    // Interfaces for communication with the main activity (sending data)
    public interface OnLessonPartListener {
        void onPartSelected(long _id);
        void onPartClicked(long _id);
    }

    public interface OnIdlingResourceListener {
        void onIdlingResource(Boolean value);
    }

    // Functions for receiving the data from main activity
    // Receives the data from the main activity
    public void setCursor(Cursor cursor) {

        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mAdapter.swapCursor(cursor, databaseVisibility);

    }

    public void setDatabaseVisibility(String dbVisibility) {
        databaseVisibility = dbVisibility;

        Log.v(TAG,"setDatabaseVisibility databaseVisibility:" + databaseVisibility);

        if(null != getActivity()) {

            // Query the database and set the adapter with the cursor data
            getActivity().getSupportLoaderManager().destroyLoader(ID_LESSON_PARTS_LOADER);
            getActivity().getSupportLoaderManager().destroyLoader(ID_GROUP_LESSON_PARTS_LOADER);

            if (databaseVisibility.equals(USER_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_LESSON_PARTS_LOADER,
                        null, this);
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_GROUP_LESSON_PARTS_LOADER,
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
        selectedLessonPart_id = -1;
        // Force deselecting all views
        int i = 0;
        while (mPartsList.getChildAt(i) != null) {
            mPartsList.getChildAt(i).setSelected(false);
            i++;
        }
    }

    // Set the reference to the selected lesson
    public void setReferenceLesson(long _id) {
        Log.v(TAG, "setReferenceLesson: lesson _id:" + _id);
        // Write the data that will be used by the loader
        referenceLesson_id = _id;

        if(null != getActivity()) {

            // Query the database and set the adapter with the cursor data
            getActivity().getSupportLoaderManager().destroyLoader(ID_LESSON_PARTS_LOADER);
            getActivity().getSupportLoaderManager().destroyLoader(ID_GROUP_LESSON_PARTS_LOADER);

            if (databaseVisibility.equals(USER_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_LESSON_PARTS_LOADER,
                        null, this);
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                getActivity().getSupportLoaderManager().initLoader(ID_GROUP_LESSON_PARTS_LOADER,
                        null, this);
            }

        }

    }

}
