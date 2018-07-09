package com.example.androidstudio.capstoneproject.utilities;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.Image;
import com.example.androidstudio.capstoneproject.data.Lesson;
import com.example.androidstudio.capstoneproject.data.LessonPart;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.data.UploadingImage;
import com.example.androidstudio.capstoneproject.ui.LogListAdapter;
import com.example.androidstudio.capstoneproject.ui.MainFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.google.common.net.HostSpecifier.isValid;


public class MyFirebaseFragment extends Fragment implements
        LogListAdapter.ListItemClickListener,
        LoaderManager.LoaderCallbacks<Cursor>{


    private static final String TAG = MyFirebaseFragment.class.getSimpleName();

    private static final String RECYCLER_VIEW_STATE = "recyclerViewState";
    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";
    private static final String USER_UID = "userUid";
    private static final String VIDEO = "video";
    private static final String IMAGE = "image";

    // static vars to handle saving the state of the task
    private static final String REFERENCE = "reference";
    private static final String PART_ID = "partId";
    private static final String IMAGE_TYPE = "imageType";
    private static final String FILE_URI_STRING = "fileUriString";
    private static final String LESSON_ID = "lessonId";
    private static final String SAVED_ITEMS = "savedItems";

    private FirebaseFirestore mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;
    private StorageReference storageRef;

    // Loader id
    private static final int ID_LOG_LOADER = 30;

    private String userUid;

    private Context mContext;

    private List<UploadTask> uploadTasks;
    private List<UploadingImage> uploadingImages;

    // Views
    private TextView mErrorMessageDisplay;
    private ProgressBar mLoadingIndicator;
    private RecyclerView mRecyclerView;

    private LogListAdapter mAdapter;

    private OnCloudListener mCallback;




    // Listener for sending information to the Activity
    public interface OnCloudListener {
        void onUploadSuccess();
        void onUploadFailure(@NonNull Exception e);
        void onDownloadComplete();
        void onDownloadFailure(@NonNull Exception e);
        void onDeletedSuccess();
        void onDeleteFailure(@NonNull Exception e);
    }

    // Constructor
    public MyFirebaseFragment() {
    }

    // Get the context and save it in mContext
    // Get the callback (activity that will send the data)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;

        try {
            mCallback = (OnCloudListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnCloudListener");
        }

    }

    // Get the values for Firebase variables
    public void setFirebase(FirebaseFirestore firebaseDatabase,
            FirebaseStorage firebaseStorage, String userUid) {
        this.mFirebaseDatabase = firebaseDatabase;
        this.mFirebaseStorage = firebaseStorage;
        this.userUid = userUid;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            Log.d(TAG, "onCreate: recovering instance state");
            this.userUid  = savedInstanceState.getString(USER_UID);
        }

        // Query the database and set the adapter with the cursor data for the log
        if (null != getActivity()) {
            // Get the support loader manager to init a new loader for this fragment, according to
            // the table being queried
            getActivity().getSupportLoaderManager().initLoader(ID_LOG_LOADER, null,
                    this);
        }

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the Ingredients fragment layout
        View rootView = inflater.inflate(R.layout.fragment_my_firebase, container, false);

        mErrorMessageDisplay = rootView.findViewById(R.id.tv_error_message_display);
        mLoadingIndicator = rootView.findViewById(R.id.pb_loading_indicator);
        mLoadingIndicator.setVisibility(View.VISIBLE);

        mRecyclerView = rootView.findViewById(R.id.rv_log);
        mRecyclerView.setHasFixedSize(true);

        // Set the layout of the recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(layoutManager);

        // Set the adapter
        mAdapter = new LogListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        // This is loading the saved position of the recycler view.
        // There is also a call on the post execute method in the loader, for updating the view.
        if(savedInstanceState != null) {
            Log.d(TAG, "recovering savedInstanceState");
            Parcelable recyclerViewState = savedInstanceState.getParcelable(RECYCLER_VIEW_STATE);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);

            processPendingTasks(savedInstanceState);

        }

        return rootView;
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

            case ID_LOG_LOADER:
                /* URI for all rows of lessons data in our "my_lessons" table */
                Uri logQueryUri = LessonsContract.MyLogEntry.CONTENT_URI;
                String sortOrder = LessonsContract.MyLogEntry._ID + " DESC";
                return new CursorLoader(mContext,
                        logQueryUri,
                        null,
                        null,
                        null,
                        sortOrder);

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

        // Pass the data to the adapter
        setCursor(data);

        if (data == null) {
            showErrorMessage();
        } else {
            showLogDataView();
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

    // Functions for receiving the data from main activity
    // Receives the data from the main activity
    public void setCursor(Cursor cursor) {

        mLoadingIndicator.setVisibility(View.INVISIBLE);

        // Saves a reference to the cursor
        // Set the data for the adapter
        mAdapter.setLogCursorData(cursor);

    }


    /**
     * This method will make the View for data visible and hide the error message.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showLogDataView() {
        // First, make sure the error is invisible
        mErrorMessageDisplay.setVisibility(View.GONE);
        // Then, make sure the JSON data is visible
        mRecyclerView.setVisibility(View.VISIBLE);
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
        mRecyclerView.setVisibility(View.INVISIBLE);
        // Then, show the error
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }


    // Helper method for uploading a specific lesson to Firebase Database and the images to
    // Firebase Storage (saving the link in the database before uploading the database)
    public void uploadDatabase(final Long lesson_id) {

        // First, upload the images
        Log.d(TAG, "uploadDatabase lesson+_id:" + lesson_id);

        long mLessonId =lesson_id;
        String mUserUid = userUid;

        // Query the parts table with the same lesson_id
        ContentResolver contentResolver = mContext.getContentResolver();

        String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
        String[] selectionArgs = {Long.toString(mLessonId)};
        Cursor partsCursor = contentResolver.query(
                LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        if (partsCursor == null) {
            Log.e(TAG, "uploadDatabase: failed to get parts cursor (database error)");
            mCallback.onUploadFailure(new Exception("Failed to get parts cursor (database failure)"));
            return;
        }

        int nRows = partsCursor.getCount();

        if (nRows == 0) {
            Log.d(TAG, "uploadDatabase: no parts found in local database for the lesson _id:"
                    + mLessonId);
            partsCursor.close();
            mCallback.onUploadFailure(new Exception("Error: no parts in this lesson"));
            return;
        }

        // Moves to the first part of that lesson
        partsCursor.moveToFirst();

        // Get all the parts and sore all image uri's in an array of Image instances
        List<Image> images = new ArrayList<>();
        Image image;

        for (int i = 0; i < nRows; i++) {

            Long item_id = partsCursor.getLong(partsCursor.
                    getColumnIndex(LessonsContract.MyLessonPartsEntry._ID));
            String localImageUri = partsCursor.getString(partsCursor.
                    getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
            String localVideoUri = partsCursor.getString(partsCursor.
                    getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));

            if (localImageUri == null && localVideoUri == null) {
                partsCursor.moveToNext();
                continue;
            }

            // Set the values in the Image instance
            image = new Image();
            image.setPart_id(item_id);
            // The priority is the video
            if (localVideoUri != null) {
                image.setLocal_uri(localVideoUri);
                image.setImageType(VIDEO);
            } else {
                image.setLocal_uri(localImageUri);
                image.setImageType(IMAGE);
            }

            // Store the instance in the array
            images.add(image);

            // get the next part
            partsCursor.moveToNext();
        }

        // close the table
        partsCursor.close();


        // If there aren't images, go to upload lesson directly

        // Verify the images array
        if (images.size() == 0) {
            Log.d(TAG, "uploadImages: no images in the database");
            // Go directly to upload the lesson
            uploadLesson(lesson_id);
            return;
        } else {
            Log.d(TAG, "uploadImages: uri's of the images stored in the Image array:" + images.toString());
        }


        // Upload the uri's stored in the images array, and after upload the lesson

        uploadTasks = new ArrayList<>();
        uploadingImages = new ArrayList<>();

        // This has an activity scope, so will unregister when the activity stops
        mStorageReference =  mFirebaseStorage.getReference().child(mUserUid);

        final String lessonRef = String.format( Locale.US, "%03d", mLessonId);

        for (int imgIndex = 0; imgIndex < images.size(); imgIndex++) {

            final Image imageToUpload = images.get(imgIndex);
            final int currentImg = imgIndex;
            final int finalImg = images.size() - 1;

            Uri uri = Uri.parse(imageToUpload.getLocal_uri());

            Log.d(TAG, "selected image/video uri:" + uri.toString());

            String rootDir = null;
            if (imageToUpload.getImageType().equals(VIDEO)) {
                rootDir = "videos";
            } else if (imageToUpload.getImageType().equals(IMAGE)) {
                rootDir = "images";
            }

            // This has activity scope to unregister the listeners when activity stops
            storageRef = mStorageReference.child(rootDir + "/" + lessonRef +
                    "/" + uri.getLastPathSegment());
            final String filePath = mUserUid + "/" + rootDir + "/" + lessonRef +
                    "/" + uri.getLastPathSegment();

            // // Refresh permissions (player will load a local file)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    final int takeFlags = intent.getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    contentResolver.takePersistableUriPermission(uri, takeFlags);
                } catch (Exception e) {
                    Log.e(TAG, "uploadImages takePersistableUriPermission error:" + e.getMessage());
                }
            }


            // Upload file to Firebase Storage

            // Saves the metadata of the images being uploaded in the uploadingImages list
            UploadingImage uploadingImage = new UploadingImage();
            uploadingImage.setStorageRefString(storageRef.toString());
            uploadingImage.setFileUriString(imageToUpload.getLocal_uri());
            uploadingImage.setPartId(imageToUpload.getPart_id());
            uploadingImage.setImageType(imageToUpload.getImageType());
            uploadingImage.setLessonId(lesson_id);
            uploadingImages.add(uploadingImage);

            // Call the task  (storage has activity scope to unregister the listeners when activity stops)
            UploadTask uploadTask = storageRef.putFile(uri);

            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "nUpload is " + progress + "% done");
                    addToLog("Image part id: "  + imageToUpload.getPart_id() +
                            " upload is " + progress + "% done.");
                }
            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.d(TAG, "Upload is paused");
                    addToLog("Image part id: "  + imageToUpload.getPart_id() +
                            " upload is paused.");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot state) {
                    //call helper function to handle the event.
                    handleTaskSuccess(
                            state,
                            imageToUpload.getPart_id(),
                            imageToUpload.getImageType(),
                            filePath,
                            lesson_id);

                    String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                            .format(new Date());
                    Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);

                    if (currentImg == finalImg) {
                        // Finished uploading images: call upload lesson
                        Log.d(TAG, "Call upload to lesson table");
                        addToLog(time_stamp + ":\nNow uploading lesson table.");
                        uploadLesson(lesson_id);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        handleTaskFailure(
                                exception,
                                imageToUpload.getPart_id(),
                                imageToUpload.getImageType(),
                                filePath,
                                lesson_id);

                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());
                        Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);

                        if (currentImg == finalImg) {
                            // Finished uploading images: call upload lesson
                            Log.d(TAG, "Call upload to lesson table");
                            addToLog(time_stamp + ":\nNow uploading lesson table.");
                            uploadLesson(lesson_id);
                        }
                    }
            });

            // get the next image (for loop)
        }

    }


    // Helper method to upload the lesson with lesson_id
    private void uploadLesson(Long lesson_id) {

        Log.d(TAG, "uploadLesson lesson_id:" + lesson_id);

        ContentResolver contentResolver = mContext.getContentResolver();
        Uri lessonUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, lesson_id);
        Cursor lessonCursor = contentResolver.query(lessonUri,
                null,
                null,
                null,
                null);

        if (lessonCursor == null) {
            Log.e(TAG, "uploadDatabase failed to get cursor");
            mCallback.onUploadFailure(new Exception("Failed to get cursor (database failure)"));
            return;
        }

        int nRows = lessonCursor.getCount();

        if (nRows > 1) {
            Log.e(TAG, "uploadDatabase local database inconsistency nRows:"
                    + nRows + " with the _id:" + lesson_id);
        }

        lessonCursor.moveToFirst();

        // Pass the data cursor to Lesson instance
        // lesson_id is parameter from method
        String user_uid;
        String lesson_title;
        String time_stamp;

        // The database is responsible by the consistency: only one row for lesson _id
        user_uid = userUid;
        lesson_title = lessonCursor.
                getString(lessonCursor.getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE));

        time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());

        // Construct a Lesson instance and set with the data from database
        Lesson lesson = new Lesson();
        lesson.setLesson_id(lesson_id);
        lesson.setUser_uid(user_uid);
        lesson.setLesson_title(lesson_title);
        lesson.setTime_stamp(time_stamp);

        String jsonString = serialize(lesson);

        Log.d(TAG, "uploadDatabase lesson jsonString:" + jsonString);

        // Load lesson parts from local database into the Lesson instance
        String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
        String[] selectionArgs = {Long.toString(lesson_id)};
        Cursor partsCursor = contentResolver.query(
                LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        // Return if there aren't parts
        if (null == partsCursor) {
            Log.d(TAG, "No parts in this lesson");
            //mCallback.onUploadFailure(new Exception("No lesson parts"));
        }

        ArrayList<LessonPart> lessonParts = new ArrayList<>();

        if (null != partsCursor) {
            partsCursor.moveToFirst();
            int nPartsRows = partsCursor.getCount();
            for (int j = 0; j < nPartsRows; j++) {

                Long item_id = partsCursor.getLong(partsCursor.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry._ID));
                // lesson_id is already loaded! (don't need to load, is the lesson_id parameter)
                String lessonPartTitle = partsCursor.getString(partsCursor.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE));
                String lessonPartText = partsCursor.getString(partsCursor.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT));
                String localImageUri = partsCursor.getString(partsCursor.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
                String cloudImageUri = partsCursor.getString(partsCursor.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));
                String localVideoUri = partsCursor.getString(partsCursor.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));
                String cloudVideoUri = partsCursor.getString(partsCursor.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI));

                LessonPart lessonPart = new LessonPart();
                lessonPart.setPart_id(item_id);
                lessonPart.setLesson_id(lesson_id);
                lessonPart.setTitle(lessonPartTitle);
                lessonPart.setText(lessonPartText);
                lessonPart.setLocal_image_uri(localImageUri);
                lessonPart.setCloud_image_uri(cloudImageUri);
                lessonPart.setLocal_video_uri(localVideoUri);
                lessonPart.setCloud_video_uri(cloudVideoUri);

                Log.v(TAG, "uploadLesson (to database): lessonPart title:" + lessonPart.getTitle());

                lessonParts.add(lessonPart);
                partsCursor.moveToNext();
            }

            partsCursor.close();
        }


        // set the lesson instance with the values read from the local database
        lesson.setLesson_parts(lessonParts);

        Log.v(TAG, "uploadLesson (to database): lesson title:" + lesson.getLesson_title());

        // Upload the Lesson instance to Firebase Database
        final String documentName = String.format( Locale.US, "%s_%03d",
                lesson.getUser_uid(), lesson.getLesson_id());

        Log.d(TAG, "uploadDatabase documentName:" + documentName);

        final String logText = lesson.getLesson_title();
        mFirebaseDatabase.collection("lessons").document(documentName)
                .set(lesson)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written with name:" + documentName);
                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());
                        addToLog(time_stamp + ":\nLesson " + logText +
                                "\nDocumentSnapshot successfully written with name:" + documentName);
                        mCallback.onUploadSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error writing document on Firebase:", e);
                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());
                        addToLog(time_stamp + ":\nLesson " + logText +
                                "\nError writing document on Firebase!" +
                                "\nDocument name:" + documentName +"\n" + e.getMessage());
                        mCallback.onUploadFailure(e);
                    }
                });

        // Close the lesson cursor
        lessonCursor.close();

    }


    // Helper method for refreshing the database from Cloud Firestore
    // Do not delete if existing
    public void refreshDatabase(final String databaseVisibility) {

        // Get multiple documents
        mFirebaseDatabase.collection("lessons")
            //.whereEqualTo("field_name", true)
            .get()
            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {

                        mCallback.onDownloadComplete();

                        if (databaseVisibility.equals(USER_DATABASE)) {

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, "refreshDatabase onComplete document.getId():" +
                                        document.getId() + " => " + document.getData());
                                Lesson lesson = document.toObject(Lesson.class);
                                String jsonString = MyFirebaseFragment.serialize(lesson);
                                Log.v(TAG, "refreshDatabase onComplete lesson jsonString:"
                                        + jsonString);
                                // refresh the lessons of the local user on its separate table
                                // this gives consistency to the database
                                if (userUid.equals(lesson.getUser_uid())) {
                                    refreshUserLesson(mContext, lesson);
                                }
                            }

                        } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                            // refresh the lessons of the group table on its separate table
                            refreshGroupLessons(mContext, task);
                            downloadGroupImages();
                        }

                    } else {
                        Log.d(TAG, "Error in getting documents: ", task.getException());
                        if (task.getException() != null) {
                            mCallback.onDownloadFailure(task.getException());
                        } else {
                            mCallback.onDownloadFailure(new Exception("Error in getting documents from" +
                                    " Firestore"));
                        }
                    }
                }
            });

    }


    private void refreshUserLesson(Context context, Lesson lesson) {

        if (null == lesson) {
            return;
        }
        Log.v(TAG, "refreshUserLesson lesson_id:" + lesson.getLesson_id());

        // query the local database to see if find the lesson with the _id
        // delete it and save another if found
        // create if didn't exist
        Uri queryUri;
        queryUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI,
            lesson.getLesson_id());

        ContentResolver contentResolver = context.getContentResolver();
        Cursor lessonCursor;
        if (queryUri != null) {
            lessonCursor = contentResolver.query(queryUri,
                    null,
                    null,
                    null,
                    null);
        } else {
            Log.v(TAG, "Error: null cursor");
            mCallback.onDownloadFailure(new Exception("Error in saving documents downloaded from" +
                    " Firestore (null cursor)"));
            return;
        }

        int nRows = -1;
        if (lessonCursor != null) {
            lessonCursor.moveToFirst();
            nRows = lessonCursor.getCount();
            lessonCursor.close();
        }


        if (nRows > 0) {

            // first, delete the parts by the lesson id
            Uri deleteUri = ContentUris.withAppendedId(LessonsContract.MyLessonPartsEntry.CONTENT_URI_BY_LESSON_ID,
                    lesson.getLesson_id());
            int numberOfPartsDeleted = contentResolver.delete(
                    deleteUri,
                    null,
                    null);
            Log.v(TAG, "refreshUserLesson numberOfPartsDeleted:" + numberOfPartsDeleted);

            // second, delete the lesson itself
            Uri deleteLessonUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI,
                    lesson.getLesson_id());
            int numberOfLessonsDeleted = contentResolver.delete(
                    deleteLessonUri,
                    null,
                    null);
            Log.v(TAG, "refreshLesson numberOfLessonsDeleted:" + numberOfLessonsDeleted);

            // now, insert the new lesson
            /* Create values to insert */
            // insert with the same id (because it will make the consistency)
            ContentValues insertLessonValues = new ContentValues();
            insertLessonValues.put(LessonsContract.MyLessonsEntry._ID, lesson.getLesson_id());
            insertLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, lesson.getLesson_title());

            Uri lessonUri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI, insertLessonValues);

            Long inserted_lesson_id;

            if (lessonUri == null) {
                // Returns with error if was not succeeded
                mCallback.onDownloadFailure(new Exception("Error in saving documents downloaded from" +
                        " Firestore.\nError in saving the lesson.\nNo part will be saved.\n" +
                        "(no document inserted into local database)"));
                return;
            } else {
                Log.v(TAG, "insert uri:" + lessonUri.toString());
                // for inserting the parts, extract the _id of the uri!
                inserted_lesson_id = Long.parseLong(lessonUri.getPathSegments().get(1));
            }

            // insert all the parts of the lesson into the database in its table
            ArrayList<LessonPart> lessonParts = lesson.getLesson_parts();
            // Insert all the parts, with the lesson_id value of the last _id inserted in the local database
            // This will give consistency and separates the local database consistency from the remote
            if (null != lessonParts) {
                for (LessonPart lessonPart : lessonParts) {
                    ContentValues insertLessonPartValues = new ContentValues();
                    insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID,
                            inserted_lesson_id);
                    insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE,
                            lessonPart.getTitle());
                    String text = lessonPart.getText();
                    insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT,
                            text);
                    insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI,
                            lessonPart.getLocal_image_uri());
                    insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI,
                            lessonPart.getCloud_image_uri());
                    insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI,
                            lessonPart.getLocal_video_uri());
                    insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI,
                            lessonPart.getCloud_video_uri());

                    Uri partUri = contentResolver.insert(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                            insertLessonPartValues);

                    if (partUri == null) {
                        Log.e(TAG, "Error on inserting part lessonPart.getTitle():" +
                                lessonPart.getTitle());
                    }

                    Log.v(TAG, "refreshUserLesson partUri inserted:" + partUri);
                }
            }

        }
    }


    // In case of group lessons, clear the existing table and insert new data
    private void refreshGroupLessons(Context context, Task<QuerySnapshot> task) {

        Log.v(TAG, "refreshGroupLesson");

        ContentResolver contentResolver = context.getContentResolver();

        int numberOfLessonPartsDeleted = contentResolver.delete(
                LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                null,
                null);

        Log.v(TAG, "refreshGroupLesson numberOfLessonPartsDeleted:" + numberOfLessonPartsDeleted);

        int numberOfLessonsDeleted = contentResolver.delete(
                LessonsContract.GroupLessonsEntry.CONTENT_URI,
                null,
                null);

        Log.v(TAG, "refreshGroupLesson numberOfLessonsDeleted:" + numberOfLessonsDeleted);


        for (QueryDocumentSnapshot document : task.getResult()) {

            Log.d(TAG, "refreshGroupLessons onComplete document.getId():" +
                    document.getId() + " => " + document.getData());

            Lesson lesson = document.toObject(Lesson.class);
            String jsonString = MyFirebaseFragment.serialize(lesson);
            Log.v(TAG, "refreshGroupLessons onComplete lesson jsonString:" + jsonString);

            // insert the data in the clean table
            /* Create values to insert */
            ContentValues insertLessonValues = new ContentValues();
            // The _id will be automatically generated for local consistency reasons.
            // The id of the cloud will be saved is in the COLUMN_LESSON_ID instead.
            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_ID, lesson.getLesson_id());
            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TITLE, lesson.getLesson_title());
            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TIME_STAMP, lesson.getTime_stamp());
            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_USER_UID, lesson.getUser_uid());

            Uri lessonUri = contentResolver.insert(LessonsContract.GroupLessonsEntry.CONTENT_URI, insertLessonValues);

            // for inserting the parts, extract the _id of the uri!
            Long inserted_lesson_id;
            if (lessonUri != null) {
                Log.v(TAG, "insert uri:" + lessonUri.toString());
                // for inserting the parts, extract the _id of the uri!
                inserted_lesson_id = Long.parseLong(lessonUri.getPathSegments().get(1));
                // insert all the parts of the lesson
                ArrayList<LessonPart> lessonParts = lesson.getLesson_parts();
                // Insert all the parts, with the lesson_id value of the last _id inserted in the local database
                // This will give consistency and separates the local database consistency from the remote
                if (null != lessonParts) {
                    for (LessonPart lessonPart : lessonParts) {
                        ContentValues insertLessonPartValues = new ContentValues();
                        // this is not the same as in the cloud: we use the lesson _id of the last lesson inserted
                        // this is for local database consistency
                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID,
                                inserted_lesson_id);
                        // this is the part id as in the cloud
                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_ID,
                                lessonPart.getPart_id());
                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TITLE,
                                lessonPart.getTitle());
                        String text = lessonPart.getText();
                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TEXT,
                                text);
                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI,
                                lessonPart.getLocal_image_uri());
                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI,
                                lessonPart.getCloud_image_uri());
                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI,
                                lessonPart.getLocal_video_uri());
                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI,
                                lessonPart.getCloud_video_uri());
                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_USER_UID,
                                lesson.getUser_uid());

                        Uri partUri = contentResolver.insert(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                                insertLessonPartValues);

                        Log.v(TAG, "refreshGroupLessons partUri inserted:" + partUri);

                    }
                }
            }
        }
     }


     private void downloadGroupImages() {
        // get the group table, query the url's and download the images on local files
        // write the file names in the table, in the local uri's

        // Load lesson parts from local database into the Lesson instance
//         String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
//         String[] selectionArgs = {Long.toString(lesson_id)};
//         Cursor partsCursor = contentResolver.query(
//                 LessonsContract.MyLessonPartsEntry.CONTENT_URI,
//                 null,
//                 selection,
//                 selectionArgs,
//                 null);
//
//         ArrayList<LessonPart> lessonParts = new ArrayList<>();
//
//         if (null != partsCursor) {
//             partsCursor.moveToFirst();
//             int nPartsRows = partsCursor.getCount();
//             for (int j = 0; j < nPartsRows; j++) {
//
//                 Long item_id = partsCursor.getLong(partsCursor.
//                         getColumnIndex(LessonsContract.MyLessonPartsEntry._ID));
//                 // lesson_id is already loaded! (don't need to load, is the lesson_id parameter)
//                 String lessonPartTitle = partsCursor.getString(partsCursor.
//                         getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE));
//                 String lessonPartText = partsCursor.getString(partsCursor.
//                         getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT));
//                 String localImageUri = partsCursor.getString(partsCursor.
//                         getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
//                 String cloudImageUri = partsCursor.getString(partsCursor.
//                         getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));
//                 String localVideoUri = partsCursor.getString(partsCursor.
//                         getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));
//                 String cloudVideoUri = partsCursor.getString(partsCursor.
//                         getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI));
//
//                 LessonPart lessonPart = new LessonPart();
//                 lessonPart.setPart_id(item_id);
//                 lessonPart.setLesson_id(lesson_id);
//                 lessonPart.setTitle(lessonPartTitle);
//                 lessonPart.setText(lessonPartText);
//                 lessonPart.setLocal_image_uri(localImageUri);
//                 lessonPart.setCloud_image_uri(cloudImageUri);
//                 lessonPart.setLocal_video_uri(localVideoUri);
//                 lessonPart.setCloud_video_uri(cloudVideoUri);
//
//                 Log.v(TAG, "lessonPart:" + lessonPart.toString());
//
//                 lessonParts.add(lessonPart);
//                 partsCursor.moveToNext();
//             }
//
//             partsCursor.close();

     }


    // Helper function for deleting a lesson from cloud
    public void deleteLessonFromCloud(Long lesson_id) {

        final String documentName = String.format( Locale.US, "%s_%02d",
                userUid, lesson_id);

        Log.v(TAG, "deleteLessonFromCloud documentName:" + documentName);

        mFirebaseDatabase.collection("lessons").document(documentName)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "deleteLessonFromCloud: DocumentSnapshot successfully deleted!");
                        mCallback.onDeletedSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "deleteLessonFromCloud: Error deleting document", e);
                        mCallback.onDeleteFailure(e);
                    }
                });

    }


    private void addToLog(String logText) {

        ContentValues contentValues = new ContentValues();
        contentValues.put(LessonsContract.MyLogEntry.COLUMN_LOG_ITEM_TEXT, logText);
        // Insert the content values via a ContentResolver
        Uri uri = mContext.getContentResolver().insert(LessonsContract.MyLogEntry.CONTENT_URI, contentValues);

        if (uri != null) {
            Log.d(TAG, "addToLog uri:" + uri.toString());
        } else {
            Log.e(TAG, "addToLog: error in inserting item on log",
                    new Exception("addToLog: error in inserting item on log"));
        }

    }


    @Override
    public void onListItemClick(View view, int clickedItemIndex, long log_id) {

    }

    @Override
    public void onListItemLongClick(View view, int clickedItemIndex, long log_id) {

    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {

        Log.d(TAG, "onSaveInstanceState: saving instance state");

        Parcelable recyclerViewState = mRecyclerView.getLayoutManager().onSaveInstanceState();
        outState.putParcelable(RECYCLER_VIEW_STATE, recyclerViewState);

        outState.putString(USER_UID, this.userUid);

        int savedItems = 0;

        if (null != uploadingImages) {
            for (int i = 0; i < uploadingImages.size(); i++) {

                UploadingImage uploadingImage = uploadingImages.get(i);

                if (uploadingImage.getStorageRefString() != null) {
                    outState.putString(REFERENCE + i, uploadingImage.getStorageRefString());
                }
                if (uploadingImage.getPartId() != null) {
                    outState.putLong(PART_ID + i, uploadingImage.getPartId());
                }
                if (uploadingImage.getImageType() != null) {
                    outState.putString(IMAGE_TYPE + i, uploadingImage.getImageType());
                }
                if (uploadingImage.getFileUriString() != null) {
                    outState.putString(FILE_URI_STRING + i, uploadingImage.getFileUriString());
                }
                if (uploadingImage.getFileUriString() != null) {
                    outState.putLong(LESSON_ID + i, uploadingImage.getLessonId());
                }

                savedItems++;
            }
        }

        outState.putInt(SAVED_ITEMS, savedItems);

        super.onSaveInstanceState(outState);
    }


    // Helper method called by the onCreate when recovering the fragment saved state
    // This method will resume and process all pending tasks
    private void processPendingTasks(Bundle savedInstanceState) {

        int savedItems = savedInstanceState.getInt(SAVED_ITEMS);

        if (!(savedItems > 0)) {
            return;
        }

        // Will resume each task saved
        for (int i = 0; i < savedItems; i++){

            // Each image has a unique storage reference, so a unique task is possible
           final String stringRef = savedInstanceState.getString(REFERENCE + i);
           final long partId = savedInstanceState.getLong(PART_ID + i);
           final String imageType = savedInstanceState.getString(IMAGE_TYPE + i);
           final String uriString = savedInstanceState.getString(FILE_URI_STRING + i);
           final long lessonId = savedInstanceState.getLong(LESSON_ID + i);

            // If there was an upload in progress, get its reference and create a new StorageReference
            if (stringRef == null) {
                return;
            }

            mStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);

            // Find all UploadTasks under this StorageReference (in this example, there should be one)
            List<UploadTask> tasks = mStorageReference.getActiveUploadTasks();
            if (tasks.size() > 0) {

                // cancel all tasks above the first task for this storageRef (this unique image)
                if (tasks.size() > 1) {
                    for (int j = 1; j < tasks.size(); j++) {
                        tasks.get(j).cancel();
                    }
                }

                // resume the first task

                // Get the task monitoring the upload
                UploadTask task = tasks.get(0);

//                // Add new listeners to the task using an Activity scope
//                task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(UploadTask.TaskSnapshot state) {
//                        //call helper function to handle the event.
//                        handleTaskSuccess(state, imageId, imageType, uriString, lessonId);
//                    }
//                });

                final int currentImg = i;
                final int finalImg = savedItems - 1;

                // Observe state change events such as progress, pause, and resume
                task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d(TAG, "Upload is " + progress + "% done");
                        addToLog("Image part id: "  + partId +
                                " (resumed) upload is " + progress + "% done.");
                    }
                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "Upload is paused");
                        addToLog("Image part id: "  +partId +
                                " (resumed) upload is paused.");
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot state) {
                        //call helper function to handle the event.
                        handleTaskSuccess(
                                state,
                                partId,
                                imageType,
                                uriString,
                                lessonId);

                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());
                        Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);

                        if (currentImg == finalImg) {
                            // Finished uploading images: call upload lesson
                            Log.d(TAG, "Call upload to lesson table");
                            addToLog(time_stamp + ":\nNow uploading lesson table.");
                            uploadLesson(lessonId);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        handleTaskFailure(
                                exception,
                                partId,
                                imageType,
                                uriString,
                                lessonId);

                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());
                        Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);

                        if (currentImg == finalImg) {
                            // Finished uploading images: call upload lesson
                            Log.d(TAG, "Call upload to lesson table");
                            addToLog(time_stamp + ":\nNow uploading lesson table.");
                            uploadLesson(lessonId);
                        }
                    }
                });

            }
        }
    }


    private void handleTaskSuccess(UploadTask.TaskSnapshot state,
                                   long partId,
                                   String imageType,
                                   String fileUriString,
                                   long lessonId) {

        Log.d(TAG, "handleTaskSuccess complete. Uploaded image/video id:" +
                partId + " of lesson id:" + lessonId);

        if (state.getMetadata() != null) {
            Log.d(TAG, "bucket:" + state.getMetadata().getBucket());
        }

        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());
        addToLog(time_stamp + ":\nLesson id:" + lessonId +
                "\nSuccessfully uploaded image/video id:" + partId);

        // Save the photoUrl in the database
        ContentValues contentValues = new ContentValues();
        // Put the lesson title into the ContentValues

        // test for integrity
        if(imageType == null) {
            return;
        }

        if (imageType.equals(VIDEO)) {
            contentValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI,
                    fileUriString);
        } else if (imageType.equals(IMAGE)) {
            contentValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI,
                    fileUriString);
        }

        // Insert the content values via a ContentResolver
        ContentResolver contentResolver = mContext.getContentResolver();
        Uri updateUri = ContentUris.withAppendedId(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                partId);
        int numberOfImagesUpdated = contentResolver.update(
                updateUri,
                contentValues,
                null,
                null);

        Log.d(TAG, "Updated " + numberOfImagesUpdated + " item(s) in the database");

    }


    private void handleTaskFailure(Exception e,
                                   long partId,
                                   String imageType,
                                   String fileUriString,
                                   long lessonId) {

        Log.e(TAG, "handleTaskFailure failure: image/video id:" +
                partId + " of lesson id:" + lessonId, e);
        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());
        addToLog(time_stamp + ":\nLesson id:" + lessonId +
                "\nUpload failure image/video id:" + partId + "\nError:" + e.getMessage());

    }


    /**
     * Serialize string.
     *
     * @param <T> Type of the object passed
     * @param obj Object to serialize
     * @return Serialized string
     */
    static private <T> String serialize(T obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }


    /**
     * De-serialize a string
     *
     * @param <T>        Type of the object
     * @param jsonString Serialized string
     * @param tClass     Class of the type
     * @return De-serialized object
     * @throws ClassNotFoundException the class not found exception
     */
    static private <T> T deSerialize(String jsonString, Class<T> tClass) throws ClassNotFoundException {
        if (!isValid(jsonString)) {
            return null;
        }
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonString, tClass);
    }



}


