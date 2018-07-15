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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.example.androidstudio.capstoneproject.data.DownloadingImage;
import com.example.androidstudio.capstoneproject.data.Image;
import com.example.androidstudio.capstoneproject.data.Lesson;
import com.example.androidstudio.capstoneproject.data.LessonPart;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.data.UploadingImage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MyFirebaseFragment extends Fragment {


    private static final String TAG = MyFirebaseFragment.class.getSimpleName();

    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";
    private static final String USER_UID = "userUid";
    private static final String VIDEO = "video";
    private static final String IMAGE = "image";
    private static final String RELEASE = "release";

    // static vars to handle saving the state of the task
    private static final String UPLOADING_REFERENCE = "uploadingReference";
    private static final String UPLOADING_PART_ID = "uploadingPartId";
    private static final String UPLOADING_IMAGE_TYPE = "uploadingImageType";
    private static final String UPLOADING_FILE_URI_STRING = "uploadingFileUriString";
    private static final String UPLOADING_LESSON_ID = "uploadingLessonId";
    private static final String UPLOADING_SAVED_ITEMS = "uploadingSavedItems";

    private static final String DOWNLOADING_REFERENCE = "downloadingReference";
    private static final String DOWNLOADING_PART_ID = "downloadingPartId";
    private static final String DOWNLOADING_IMAGE_TYPE = "downloadingImageType";
    private static final String DOWNLOADING_FILE_URI_STRING = "downloadingFileUriString";
    private static final String DOWNLOADING_LESSON_ID = "downloadingLessonId";
    private static final String DOWNLOADING_SAVED_ITEMS = "downloadingSavedItems";

    // This limits the number of rows in the log table
    private static final int MAX_ROWS_LOG_TABLE = 100;

    private FirebaseFirestore mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;
    // global ref to the storage
    private StorageReference storageRef;

    private String userUid;
    private Context mContext;

    private static ContentResolver contentResolver;

    private List<UploadingImage> uploadingImages;
    private List<DownloadingImage> downloadingImages;

    private OnCloudListener mCallback;

    // log buffer
    private static List<String> logBuffer = new ArrayList<>();



    // Listener for sending information to the Activity
    public interface OnCloudListener {

        void onUploadImageSuccess();
        void onUploadImageFailure(@NonNull Exception e);
        void onUploadLessonSuccess();
        void onUploadLessonFailure(@NonNull Exception e);

        void onDownloadDatabaseSuccess(int nImagesToDownload);
        void onDownloadDatabaseFailure(@NonNull Exception e);
        void onDownloadImageSuccess();
        void onDownloadImageFailure(@NonNull Exception e);

        void onDeleteCloudDatabaseSuccess();
        void onDeleteCloudDatabaseFailure(@NonNull Exception e);
        void onDeleteCloudImagesSuccess(int nRowsDeleted);
        void onDeleteCloudImagesFailure(@NonNull Exception e);

    }

    // Constructor
    public MyFirebaseFragment() {
    }

    // Get the context and save it in mContext
    // Get the callback (activity that will send the data)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Saves the context of the caller
        mContext = context;

        try {
            mCallback = (OnCloudListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnCloudListener");
        }

    }

    // Get the values for Firebase user
    public void setFirebase(String userUid) {
        this.userUid = userUid;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null) {
            Log.d(TAG, "onCreate: recovering instance state");
            this.userUid  = savedInstanceState.getString(USER_UID);
        }

        // Initialize Firebase components
        mFirebaseDatabase = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mFirebaseDatabase.setFirestoreSettings(settings);
        mFirebaseStorage = FirebaseStorage.getInstance();

        // This is loading the saved position of the recycler view.
        // There is also a call on the post execute method in the loader, for updating the view.
        if(savedInstanceState != null) {
            Log.d(TAG, "recovering savedInstanceState");

//            processUploadingPendingTasks(savedInstanceState);
//            processDownloadingPendingTasks(savedInstanceState);
        }

    }


    // Helper method for uploading the images and the text
    // First, upload the images
    // Then, get the uri's obtained from Storage and save in the lessons table
    // Finally, upload the lesson table
//    public void uploadImagesAndDatabase(final Long lesson_id) {
//
//        // First, upload the images
//        Log.d(TAG, "uploadImagesAndDatabase lesson+_id:" + lesson_id);
//
//        long mLessonId =lesson_id;
//        String mUserUid = userUid;
//
//        // Query the parts table with the same lesson_id
//        contentResolver = mContext.getContentResolver();
//
//        String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
//        String[] selectionArgs = {Long.toString(mLessonId)};
//        Cursor partsCursor = contentResolver.query(
//                LessonsContract.MyLessonPartsEntry.CONTENT_URI,
//                null,
//                selection,
//                selectionArgs,
//                null);
//
//        if (partsCursor == null) {
//            Log.e(TAG, "uploadImagesAndDatabase: failed to get parts cursor (database error)");
//            mCallback.onUploadImageFailure(new Exception("Failed to get parts cursor (database failure)"));
//            return;
//        }
//
//        long nRows = partsCursor.getCount();
//
//        if (nRows == 0) {
//            Log.d(TAG, "uploadImagesAndDatabase: no parts found in local database for the lesson _id:"
//                    + mLessonId);
//            //mCursor.close();
//            mCallback.onUploadImageFailure(new Exception("Error: no parts in this lesson"));
//            partsCursor.close();
//            return;
//        }
//
//        // Get all the parts and sore all image uri's in an array of Image instances
//        List<Image> images = new ArrayList<>();
//        Image image;
//
//        // Moves to the first part of that lesson
//        partsCursor.moveToFirst();
//
//        do {
//
//            Long item_id = partsCursor.getLong(partsCursor.
//                    getColumnIndex(LessonsContract.MyLessonPartsEntry._ID));
//            String localImageUri = partsCursor.getString(partsCursor.
//                    getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
//            String localVideoUri = partsCursor.getString(partsCursor.
//                    getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));
//
//            if (localImageUri == null && localVideoUri == null) {
//                partsCursor.moveToNext();
//                continue;
//            }
//
//            // Set the values in the Image instance
//            image = new Image();
//            image.setPart_id(item_id);
//            // The priority is the video
//            if (localVideoUri != null) {
//                image.setLocal_uri(localVideoUri);
//                image.setImageType(VIDEO);
//            } else {
//                image.setLocal_uri(localImageUri);
//                image.setImageType(IMAGE);
//            }
//
//            // Store the instance in the array
//            images.add(image);
//
//            // get the next part
//        } while (partsCursor.moveToNext());
//
//        partsCursor.close();
//
//
//        // If there aren't images, go to upload lesson directly
//        if (images.size() == 0) {
//            Log.d(TAG, "uploadImagesAndDatabase: no images in the database");
//            // Go directly to upload the lesson
//            uploadLesson(lesson_id);
//            return;
//        } else {
//            Log.d(TAG, "uploadImagesAndDatabase: uri's of the images stored in the Image array:" + images.toString());
//        }
//
//        // Upload the uri's stored in the images array, and after, upload the lesson
//
//        // Array for saving the state of the images being uploaded
//        uploadingImages = new ArrayList<>();
//
//        // This has an activity scope, so will unregister when the activity stops
//        mStorageReference =  mFirebaseStorage.getReference().child(mUserUid);
//
//        final String lessonRef = String.format( Locale.US, "%03d", mLessonId);
//
//        for (int imgIndex = 0; imgIndex < images.size(); imgIndex++) {
//
//            final Image imageToUpload = images.get(imgIndex);
//            final int currentImg = imgIndex;
//            final int finalImg = images.size() - 1;
//
//            Uri uri = Uri.parse(imageToUpload.getLocal_uri());
//
//            Log.d(TAG, "selected image/video uri:" + uri.toString());
//
//            String rootDir = null;
//            if (imageToUpload.getImageType().equals(VIDEO)) {
//                rootDir = "videos";
//            } else if (imageToUpload.getImageType().equals(IMAGE)) {
//                rootDir = "images";
//            }
//
//            // This has activity scope to unregister the listeners when activity stops
//            storageRef = mStorageReference.child(rootDir + "/" + lessonRef +
//                    "/" + uri.getLastPathSegment());
//            final String filePath = mUserUid + "/" + rootDir + "/" + lessonRef +
//                    "/" + uri.getLastPathSegment();
//
//            // // Refresh permissions (player will load a local file)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                try {
//                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//                    final int takeFlags = intent.getFlags()
//                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
//                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                    contentResolver.takePersistableUriPermission(uri, takeFlags);
//                } catch (Exception e) {
//                    Log.e(TAG, "uploadImagesAndDatabase takePersistableUriPermission error:" + e.getMessage());
//                }
//            }
//
//            // Upload file to Firebase Storage
//
//            // Saves the metadata of the images being uploaded in the uploadingImages list
//            UploadingImage uploadingImage = new UploadingImage();
//
//            uploadingImage.setStorageRefString(storageRef.toString());
//            uploadingImage.setFileUriString(imageToUpload.getLocal_uri());
//            uploadingImage.setPartId(imageToUpload.getPart_id());
//            uploadingImage.setImageType(imageToUpload.getImageType());
//            uploadingImage.setLessonId(lesson_id);
//
//            uploadingImages.add(uploadingImage);
//
//            // Call the task  (storage has activity scope to unregister the listeners when activity stops)
//            UploadTask uploadTask = storageRef.putFile(uri);
//
//            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
//                    Log.d(TAG, "Image part id: "  + imageToUpload.getPart_id() +
//                            " upload is " + String.format(Locale.US, "%.2f", progress) + "% done.");
//                    addToLog("Image part id: "  + imageToUpload.getPart_id() +
//                            " upload is " + String.format(Locale.US, "%.2f", progress) + "% done.");
//                }
//            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
//                    Log.d(TAG, "Image part id: "  + imageToUpload.getPart_id() +
//                            " upload is paused.");
//                    addToLog("Image part id: "  + imageToUpload.getPart_id() +
//                            " upload is paused.");
//                }
//            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                @Override
//                public void onSuccess(UploadTask.TaskSnapshot state) {
//                    //call helper function to handle the event.
//                    handleUploadTaskSuccess(
//                            state,
//                            imageToUpload.getPart_id(),
//                            imageToUpload.getImageType(),
//                            filePath,
//                            lesson_id);
//
//                    Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);
//                    // In MainActivity, there is a counter that will trigger the uploading of the
//                    // lesson table when the uploading of the images has been finished
//                    mCallback.onUploadImageSuccess();
//
//                }
//            }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception exception) {
//                    // Handle unsuccessful uploads
//                    handleUploadTaskFailure(
//                            exception,
//                            imageToUpload.getPart_id(),
//                            imageToUpload.getImageType(),
//                            filePath,
//                            lesson_id);
//
//                    Log.e(TAG, "Error: currentImg:" + currentImg + " finalImg:" + finalImg, exception);
//                    // In MainActivity, there is a counter that will trigger the uploading of the
//                    // lesson table when the uploading of the images has been finished
//                    mCallback.onUploadImageFailure(exception);
//                    }
//            });
//        }
//    }


    // Helper method to upload the lesson text to
    // Firebase Database
//    public void uploadLesson(long lesson_id) {
//
//        Log.d(TAG, "uploadLesson lesson_id:" + lesson_id);
//
//        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
//                .format(new Date());
//        addToLog( time_stamp + ":\nNow uploading lesson...");
//
//        contentResolver = mContext.getContentResolver();
//        Uri lessonUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, lesson_id);
//        Cursor cursorLesson = contentResolver.query(lessonUri,
//                null,
//                null,
//                null,
//                null);
//
//        if (cursorLesson == null) {
//            Log.e(TAG, "uploadImagesAndDatabase failed to get cursor");
//            mCallback.onUploadImageFailure(new Exception("Failed to get cursor (database failure)"));
//            return;
//        }
//
//        int nRows = cursorLesson.getCount();
//
//        if (nRows > 1) {
//            Log.e(TAG, "uploadImagesAndDatabase local database inconsistency nRows:"
//                    + nRows + " with the _id:" + lesson_id);
//        }
//
//        cursorLesson.moveToFirst();
//
//        // Pass the data cursor to Lesson instance
//        // lesson_id is parameter from method
//        String user_uid;
//        String lesson_title;
//
//        // The database is responsible by the consistency: only one row for lesson _id
//        user_uid = userUid;
//        lesson_title = cursorLesson.
//                getString(cursorLesson.getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE));
//
//        time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
//                .format(new Date());
//
//        // Close the lesson cursor
//        cursorLesson.close();
//
//        // Construct a Lesson instance and set with the data from database
//        Lesson lesson = new Lesson();
//
//        lesson.setLesson_id(lesson_id);
//        lesson.setUser_uid(user_uid);
//        lesson.setLesson_title(lesson_title);
//        lesson.setTime_stamp(time_stamp);
//
//        String jsonString = serialize(lesson);
//
//        Log.d(TAG, "uploadImagesAndDatabase lesson jsonString:" + jsonString);
//
//        // Load lesson parts from local database into the Lesson instance
//        String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
//        String[] selectionArgs = {Long.toString(lesson_id)};
//
//        //contentResolver.refresh(LessonsContract.MyLessonPartsEntry.CONTENT_URI, null, null);
//
//        Cursor cursorParts = contentResolver.query(
//                LessonsContract.MyLessonPartsEntry.CONTENT_URI,
//                null,
//                selection,
//                selectionArgs,
//                null);
//
//        // Return if there aren't parts
//        if (null == cursorParts) {
//            Log.d(TAG, "No parts in this lesson");
//            //mCallback.onUploadImageFailure(new Exception("No lesson parts"));
//        }
//
//        ArrayList<LessonPart> lessonParts = new ArrayList<>();
//
//        if (null != cursorParts) {
//            cursorParts.moveToFirst();
//
//            do {
//
//                Long item_id = cursorParts.getLong(cursorParts.
//                        getColumnIndex(LessonsContract.MyLessonPartsEntry._ID));
//                // lesson_id is already loaded! (don't need to load, is the lesson_id parameter)
//                String lessonPartTitle = cursorParts.getString(cursorParts.
//                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE));
//                String lessonPartText = cursorParts.getString(cursorParts.
//                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT));
//                String localImageUri = cursorParts.getString(cursorParts.
//                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
//                String cloudImageUri = cursorParts.getString(cursorParts.
//                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));
//                String localVideoUri = cursorParts.getString(cursorParts.
//                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));
//                String cloudVideoUri = cursorParts.getString(cursorParts.
//                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI));
//
//                LessonPart lessonPart = new LessonPart();
//                lessonPart.setPart_id(item_id);
//                lessonPart.setLesson_id(lesson_id);
//                lessonPart.setTitle(lessonPartTitle);
//                lessonPart.setText(lessonPartText);
//                lessonPart.setLocal_image_uri(localImageUri);
//                lessonPart.setCloud_image_uri(cloudImageUri);
//                lessonPart.setLocal_video_uri(localVideoUri);
//                lessonPart.setCloud_video_uri(cloudVideoUri);
//
//                Log.v(TAG, "uploadLesson (to database): lessonPart title:" + lessonPart.getTitle());
//
//                lessonParts.add(lessonPart);
//
//            } while (cursorParts.moveToNext());
//
//            cursorParts.close();
//        }
//
//
//        // set the lesson instance with the values read from the local database
//        lesson.setLesson_parts(lessonParts);
//
//        Log.v(TAG, "uploadLesson (to database): lesson title:" + lesson.getLesson_title());
//
//        // Upload the Lesson instance to Firebase Database
//        final String documentName = String.format( Locale.US, "%s_%03d",
//                lesson.getUser_uid(), lesson.getLesson_id());
//
//        Log.d(TAG, "uploadImagesAndDatabase documentName:" + documentName);
//
//        final String logText = lesson.getLesson_title();
//        mFirebaseDatabase.collection("lessons").document(documentName)
//                .set(lesson)
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Log.d(TAG, "DocumentSnapshot successfully written with name:" + documentName);
//                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
//                                .format(new Date());
//                        addToLog(time_stamp + ":\nLesson " + logText +
//                                "\nDocumentSnapshot successfully written with name:" + documentName);
//
//                        mCallback.onUploadLessonSuccess();
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Log.e(TAG, "Error writing document on Firebase:", e);
//                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
//                                .format(new Date());
//                        addToLog(time_stamp + ":\nLesson " + logText +
//                                "\nError writing document on Firebase!" +
//                                "\nDocument name:" + documentName +"\n" + e.getMessage());
//
//                        mCallback.onUploadLessonFailure(e);
//                    }
//                });
//
//    }


    // Helper method for refreshing the database from Cloud Firestore
    // Do not delete if existing in user table
    // Delete all in the group table (data and local files)
//    public void refreshDatabase(final String databaseVisibility) {
//
//        // Get multiple documents (all the data in the database)
//        mFirebaseDatabase.collection("lessons")
//            //.whereEqualTo("field_name", true)
//            .get()
//            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                @Override
//                public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                    if (task.isSuccessful()) {
//
//                        //mCallback.onDownloadDatabaseSuccess();
//
//                        if (databaseVisibility.equals(USER_DATABASE)) {
//
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                Log.d(TAG, "refreshDatabase onComplete document.getId():" +
//                                        document.getId() + " => " + document.getData());
//                                Lesson lesson = document.toObject(Lesson.class);
//                                String jsonString = MyFirebaseFragment.serialize(lesson);
//                                Log.v(TAG, "refreshDatabase onComplete lesson jsonString:"
//                                        + jsonString);
//                                // refresh the lessons of the local user on its separate table
//                                // this gives more security to the database
//                                // --> filter by the user uid of the lesson downloaded
//                                if (userUid.equals(lesson.getUser_uid())) {
//                                    refreshUserLesson(lesson);
//                                }
//                            }
//
//                        } else if (databaseVisibility.equals(GROUP_DATABASE)) {
//                            // refresh the lessons of the group table on its separate table
//                            // --> pass all the data
//                            refreshGroupLessons(task);
//                            downloadGroupImages(); // this will use the counter
//                        }
//
//                    } else {
//                        Log.d(TAG, "Error in getting documents: ", task.getException());
//                        if (task.getException() != null) {
//                            mCallback.onDownloadDatabaseFailure(task.getException());
//                        } else {
//                            mCallback.onDownloadDatabaseFailure(new Exception("Error in getting " +
//                                    "documents from Firestore"));
//                        }
//
//                    }
//                }
//            });
//
//    }


    // Helper method called by refreshDatabase
//    private void refreshUserLesson(Lesson lesson) {
//
//        Log.v(TAG, "refreshUserLesson lesson_id:" + lesson.getLesson_id());
//
//        // query the local database to see if find the lesson with the _id
//        // delete it and save another if found; create if didn't exist
//        Uri queryUri;
//        queryUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI,
//            lesson.getLesson_id());
//
//        Cursor mCursor;
//        contentResolver = mContext.getContentResolver();
//        if (queryUri != null) {mCursor = contentResolver.query(queryUri,
//                    null,
//                    null,
//                    null,
//                    null);
//        } else {
//            Log.v(TAG, "Error: null cursor");
//            mCallback.onDownloadDatabaseFailure(new Exception("Error in saving documents downloaded " +
//                    "from Firestore (null cursor)"));
//            return;
//        }
//
//        int nRows = -1;
//        if (mCursor != null) {
//            mCursor.moveToFirst();
//            nRows = mCursor.getCount();
//            mCursor.close();
//        }
//
//        if (nRows > 0) {
//            // first, delete the parts by the lesson id
//            Uri deleteUri = ContentUris.withAppendedId(LessonsContract.MyLessonPartsEntry.CONTENT_URI_BY_LESSON_ID,
//                    lesson.getLesson_id());
//            int numberOfPartsDeleted = contentResolver.delete(deleteUri,
//                    null,
//                    null);
//            Log.v(TAG, "refreshUserLesson numberOfPartsDeleted:" + numberOfPartsDeleted);
//
//            // second, delete the lesson itself
//            Uri deleteLessonUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI,
//                    lesson.getLesson_id());
//            int numberOfLessonsDeleted = contentResolver.delete(deleteLessonUri,
//                    null,
//                    null);
//            Log.v(TAG, "refreshLesson numberOfLessonsDeleted:" + numberOfLessonsDeleted);
//        }
//
//
//        // now, insert the new lesson
//        /* Create values to insert */
//        // insert with the same id (because it will make the consistency)
//        ContentValues insertLessonValues = new ContentValues();
//        insertLessonValues.put(LessonsContract.MyLessonsEntry._ID, lesson.getLesson_id());
//        insertLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, lesson.getLesson_title());
//
//        Uri lessonUri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI, insertLessonValues);
//
//        Long inserted_lesson_id;
//
//        if (lessonUri == null) {
//            // Returns with error if was not succeeded
//            mCallback.onDownloadDatabaseFailure(new Exception("Error in saving documents downloaded from" +
//                    " Firestore.\nError in saving the lesson.\nNo part will be saved.\n" +
//                    "(no document inserted into local database)"));
//            Log.e(TAG, "refreshUserLesson insert uri:null");
//            return;
//        } else {
//            Log.v(TAG, "refreshUserLesson insert uri:" + lessonUri.toString());
//            // for inserting the parts, extract the _id of the uri!
//            inserted_lesson_id = Long.parseLong(lessonUri.getPathSegments().get(1));
//        }
//
//        // insert all the parts of the lesson into the database in its table
//        ArrayList<LessonPart> lessonParts = lesson.getLesson_parts();
//        // Insert all the parts, with the lesson_id value of the last _id inserted in the local database
//        // This will give consistency and separates the local database consistency from the remote
//        if (null != lessonParts) {
//            for (LessonPart lessonPart : lessonParts) {
//                ContentValues insertLessonPartValues = new ContentValues();
//                insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID,
//                        inserted_lesson_id);
//                insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE,
//                        lessonPart.getTitle());
//                String text = lessonPart.getText();
//                insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT,
//                        text);
//                insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI,
//                        lessonPart.getLocal_image_uri());
//                insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI,
//                        lessonPart.getCloud_image_uri());
//                insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI,
//                        lessonPart.getLocal_video_uri());
//                insertLessonPartValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI,
//                        lessonPart.getCloud_video_uri());
//
//                Uri partUri = contentResolver.insert(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
//                        insertLessonPartValues);
//
//                if (partUri == null) {
//                    Log.e(TAG, "refreshUserLesson: Error on inserting part lessonPart.getTitle():" +
//                            lessonPart.getTitle());
//                }
//
//                Log.v(TAG, "refreshUserLesson partUri inserted:" + partUri);
//            }
//        }
//
//        // inform the main activity that the job finishes
//        mCallback.onDownloadDatabaseSuccess(0);
//
//    }


    // Helper method called by refreshDatabase
    // In case of group lessons, clear the existing table and insert new data
//    private void refreshGroupLessons(Task<QuerySnapshot> task) {
//
//        Log.v(TAG, "refreshGroupLesson");
//
//        // first, delete the file images of each lesson
//        contentResolver = mContext.getContentResolver();
//        Cursor mCursor = contentResolver.query(LessonsContract.GroupLessonsEntry.CONTENT_URI,
//                null,
//                null,
//                null,
//                null);
//
//        if (mCursor != null && mCursor.moveToFirst()) {
//            // delete the images with the help of the deleteImageLocalFilesOfGroupLesson method
//            do {
//                long lesson_id = mCursor.getLong(mCursor.getColumnIndex(LessonsContract.GroupLessonsEntry._ID));
//                deleteImageLocalFilesOfGroupLesson(lesson_id);
//                // get the next lesson_id
//            } while (mCursor.moveToNext());
//        }
//
//        // grants that the cursor is closed
//        if (mCursor != null) {
//            mCursor.close();
//        }
//
//        // now delete the lesson parts table (from group tables)
//        int numberOfLessonPartsDeleted = contentResolver.delete(
//                LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
//                null,
//                null);
//
//        Log.v(TAG, "refreshGroupLesson numberOfLessonPartsDeleted:" + numberOfLessonPartsDeleted);
//
//        // and delete the lesson table (from group tables)
//        int numberOfLessonsDeleted = contentResolver.delete(
//                LessonsContract.GroupLessonsEntry.CONTENT_URI,
//                null,
//                null);
//
//        Log.v(TAG, "refreshGroupLesson numberOfLessonsDeleted:" + numberOfLessonsDeleted);
//
//        // insert the new data
//        for (QueryDocumentSnapshot document : task.getResult()) {
//
//            Log.d(TAG, "refreshGroupLessons onComplete document.getId():" +
//                    document.getId() + " => " + document.getData());
//
//            // recover the Lesson instance
//            Lesson lesson = document.toObject(Lesson.class);
//            String jsonString = MyFirebaseFragment.serialize(lesson);
//            Log.v(TAG, "refreshGroupLessons onComplete lesson jsonString:" + jsonString);
//
//            // insert the data in the clean table
//            /* Create values to insert */
//            ContentValues insertLessonValues = new ContentValues();
//            // The _id will be automatically generated for local consistency reasons.
//            // The id of the cloud will be saved is in the COLUMN_LESSON_ID instead.
//            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_ID, lesson.getLesson_id());
//            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TITLE, lesson.getLesson_title());
//            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TIME_STAMP, lesson.getTime_stamp());
//            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_USER_UID, lesson.getUser_uid());
//
//            Uri lessonUri = contentResolver.insert(LessonsContract.GroupLessonsEntry.CONTENT_URI, insertLessonValues);
//
//            // for inserting the parts, extract the _id of the uri!
//            Long inserted_lesson_id;
//            if (lessonUri != null) {
//                Log.v(TAG, "insert uri:" + lessonUri.toString());
//                // for inserting the parts, extract the _id of the uri!
//                inserted_lesson_id = Long.parseLong(lessonUri.getPathSegments().get(1));
//                // insert all the parts of the lesson
//                ArrayList<LessonPart> lessonParts = lesson.getLesson_parts();
//                // Insert all the parts, with the lesson_id value of the last _id inserted in the local database
//                // This will give consistency and separates the local database consistency from the remote
//                if (null != lessonParts) {
//                    for (LessonPart lessonPart : lessonParts) {
//                        ContentValues insertLessonPartValues = new ContentValues();
//                        // this is not the same as in the cloud: we use the lesson _id of the last lesson inserted
//                        // this is for local database consistency
//                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID,
//                                inserted_lesson_id);
//                        // this is the part id as in the cloud
//                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_ID,
//                                lessonPart.getPart_id());
//                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TITLE,
//                                lessonPart.getTitle());
//                        String text = lessonPart.getText();
//                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TEXT,
//                                text);
//                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI,
//                                lessonPart.getLocal_image_uri());
//                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI,
//                                lessonPart.getCloud_image_uri());
//                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI,
//                                lessonPart.getLocal_video_uri());
//                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI,
//                                lessonPart.getCloud_video_uri());
//                        insertLessonPartValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_USER_UID,
//                                lesson.getUser_uid());
//
//                        Uri partUri = contentResolver.insert(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
//                                insertLessonPartValues);
//
//                        Log.v(TAG, "refreshGroupLessons partUri inserted:" + partUri);
//
//                    }
//                }
//            }
//        }
//    }


    // Helper method called by refreshDatabase
    // It will download all the images and save in local files
    // Then, will save the path (local uri's) in the group lesson table
    // The file will be read and showed in the view of the lesson part
//    private void downloadGroupImages() {
//
//         // open the group_lesson_parts table and for each row, download the file that has its
//         // path stored in the cloud_video_uri or cloud_image_uri, saves the file into local directory
//         // and write the file path uri into the local_video_uri or local_image_uri
//
//         // PREPARE TO DOWNLOAD THE FILES
//
//         // Load the images uri's into the images array
//         contentResolver = mContext.getContentResolver();
//         Cursor mCursor = contentResolver.query(
//                 LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
//                 null,
//                 null,
//                 null,
//                 null);
//
//         if (mCursor == null) {
//             Log.d(TAG, "Failed to get cursor for group_lesson_parts");
//             mCallback.onDownloadDatabaseSuccess(0);
//             return;
//         }
//
//         int nRows = mCursor.getCount();
//         mCursor.moveToFirst();
//        int nImagesToDownload = 0;
//
//        // Get all the parts and sore all image cloud uri's in an array of Image instances
//         List<Image> images = new ArrayList<>();
//         Image image;
//
//         if (nRows > 0) {
//
//             do {
//
//                 Long item_id = mCursor.getLong(mCursor.
//                         getColumnIndex(LessonsContract.GroupLessonPartsEntry._ID));
//                 Long lesson_id = mCursor.getLong(mCursor.
//                         getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID));
//                 String cloudImageUri = mCursor.getString(mCursor.
//                         getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));
//                 String cloudVideoUri = mCursor.getString(mCursor.
//                         getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI));
//
//                 if (cloudImageUri != null || cloudVideoUri != null) {
//                     // Set the values in the Image instance
//                     image = new Image();
//                     image.setPart_id(item_id);
//                     image.setLesson_id(lesson_id);
//                     // The priority is the video
//                     if (cloudVideoUri != null) {
//                         image.setCloud_uri(cloudVideoUri);
//                         image.setImageType(VIDEO);
//                     } else {
//                         image.setCloud_uri(cloudImageUri);
//                         image.setImageType(IMAGE);
//                     }
//
//                     // Store the instance in the array
//                     images.add(image);
//
//                     // THE COUNTER OF IMAGES TO DOWNLOAD
//                     nImagesToDownload++;
//                 }
//                 // get the next part
//             } while (mCursor.moveToNext());
//
//         } else {
//             // return if there are no images to download
//             mCursor.close();
//             mCallback.onDownloadDatabaseSuccess(0);
//             return;
//         }
//
//         // close the cursor
//         mCursor.close();
//
//         // tell to the main activity the state and the number of images to download for control
//         mCallback.onDownloadDatabaseSuccess(nImagesToDownload);
//         if (nImagesToDownload == 0) {
//             return;
//         }
//
//         // BEGIN TO DOWNLOAD THE FILES
//
//         // Download the file from Cloud Storage, with the uri's stored in the images array, and after,
//         // updates the parts table with the local file path
//         downloadingImages = new ArrayList<>();
//
//         // This has an activity scope, so will unregister when the activity stops
//         mStorageReference =  mFirebaseStorage.getReference();
//
//         for (int imgIndex = 0; imgIndex < images.size(); imgIndex++) {
//
//             final Image imageToDownload = images.get(imgIndex);
//             final int currentImg = imgIndex;
//             final int finalImg = images.size() - 1;
//
//             // This has activity scope to unregister the listeners when activity stops
//             storageRef = mStorageReference.child(imageToDownload.getCloud_uri());
//
//             // Download the file from Firebase Storage
//
//             // create a local filename
//             String filename = imageToDownload.getImageType() + String.format(Locale.US,
//                     "_%03d_%03d", imageToDownload.getLesson_id(), imageToDownload.getPart_id());
//
//             // create a local file
//             // delete if it already exists (in the context of this app)
//             File file = new File(mContext.getFilesDir(), filename);
//             boolean fileDeleted;
//             boolean fileCreated = false;
//             if(file.exists()) {
//                 fileDeleted = file.delete();
//                 Log.d(TAG, "downloadGroupImages fileDeleted:" + fileDeleted);
//             } else {
//                 try {
//                    fileCreated =  file.createNewFile();
//                 } catch (IOException e) {
//                     e.printStackTrace();
//                 }
//             }
//
//             if (!fileCreated) {
//                 Log.e(TAG, "downloadGroupImages error creating file: file not created");
//             }
//
//             URI fileUri = file.toURI();
//             final String fileUriString = fileUri.toString();
//
//             Log.d(TAG, "downloadGroupImages fileUriString:" + fileUriString);
//
//             DownloadingImage downloadingImage = new DownloadingImage();
//             downloadingImage.setPartId(imageToDownload.getPart_id());
//             downloadingImage.setLessonId(imageToDownload.getLesson_id());
//             downloadingImage.setImageType(imageToDownload.getImageType());
//             downloadingImage.setStorageRefString(storageRef.toString());
//             downloadingImage.setFileUriString(fileUriString);
//
//             // Save the image data for when onSavedInstance state need it
//             downloadingImages.add(downloadingImage);
//
//             // Call the task  (storage has activity scope to unregister the listeners when activity stops)
//             FileDownloadTask downloadTask = storageRef.getFile(file);
//
//             downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
//                 @Override
//                 public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                     double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
//                     Log.d(TAG, "Image part id: "  + imageToDownload.getPart_id() +
//                             " download is " + String.format(Locale.US, "%.2f", progress) + "% done.");
//                     addToLog("Image part id: "  + imageToDownload.getPart_id() +
//                             " download is " + String.format(Locale.US, "%.2f", progress) + "% done.");
//                 }
//             }).addOnPausedListener(new OnPausedListener<FileDownloadTask.TaskSnapshot>() {
//                 @Override
//                 public void onPaused(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                     Log.d(TAG, "Image part id: "  + imageToDownload.getPart_id() +
//                             " download is paused.");
//                     addToLog( "Image part id: "  + imageToDownload.getPart_id() +
//                             " download is paused.");
//                 }
//             }).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//                 @Override
//                 public void onSuccess(FileDownloadTask.TaskSnapshot state) {
//                     //call helper function to handle the event.
//                     handleDownloadTaskSuccess(
//                             imageToDownload.getPart_id(),
//                             imageToDownload.getImageType(),
//                             fileUriString,
//                             imageToDownload.getLesson_id());
//
//                     Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);
//
//                     // we don't need the counter in main activity, because we already have
//                     // downloaded the lesson table (but we will use for showing the message)
//                     mCallback.onDownloadImageSuccess();
//
//                 }
//             }).addOnFailureListener(new OnFailureListener() {
//                 @Override
//                 public void onFailure(@NonNull Exception exception) {
//                     // Handle unsuccessful downloads
//                     handleDownloadTaskFailure(
//                             exception,
//                             imageToDownload.getPart_id(),
//                             imageToDownload.getImageType(),
//                             fileUriString,
//                             imageToDownload.getLesson_id());
//
//                     Log.e(TAG, "Error: currentImg:" + currentImg + " finalImg:" + finalImg, exception);
//
//                     // we don't need the counter in main activity, because we already have
//                     // downloaded the lesson table
//                     mCallback.onDownloadImageFailure(exception);
//                 }
//             });
//         }
//     }


    // Helper function for deleting a lesson text from cloud database
    public void deleteLessonFromCloudDatabase(long lesson_id) {

        // First, save the cloud file reference in the form "images/001/file_name" or
        // "videos/001/file_name" where 001 is the lesson_id (not the part_id) in the
        // var fileReference
        // This is necessary to be able to delete from Firebase Storage
        contentResolver = mContext.getContentResolver();
        String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
        String[] selectionArgs = {Long.toString(lesson_id)};
        Cursor cursor = null;
        if (contentResolver != null) {
            cursor = contentResolver.query(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    null);
        }

        String fileReference;

        if (cursor == null) { return; }  // no files in the table

        cursor.moveToFirst();

        do {
            // get info to build the fileRef
            String cloud_image_uri = cursor.getString(cursor.
                    getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));
            String cloud_video_uri = cursor.getString(cursor.
                    getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI));

            // build the fileRef
            if (cloud_image_uri != null) {
                String[] filePathParts = cloud_image_uri.split("/");
                fileReference = filePathParts[1] + "/" + filePathParts[2] + "/" + filePathParts[3];

                // store the fileRef in the table my_cloud_files_to_delete
                ContentValues content = new ContentValues();
                content.put(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_FILE_REFERENCE, fileReference);
                content.put(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_LESSON_ID, lesson_id);
                Uri uri = contentResolver.insert(LessonsContract.MyCloudFilesToDeleteEntry.CONTENT_URI, content);
                Log.d(TAG, "deleteLessonFromCloudDatabase inserted uri:" + uri);
                Log.d(TAG, "deleteLessonFromCloudDatabase fileReference:" + fileReference);
            }

            if (cloud_video_uri != null) {
                String[] filePathParts = cloud_video_uri.split("/");
                fileReference = filePathParts[1] + "/" + filePathParts[2] + "/" + filePathParts[3];

                // store the fileRef in the table my_cloud_files_to_delete
                ContentValues content = new ContentValues();
                content.put(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_FILE_REFERENCE, fileReference);
                content.put(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_LESSON_ID, lesson_id);
                Uri uri = contentResolver.insert(LessonsContract.MyCloudFilesToDeleteEntry.CONTENT_URI, content);
                Log.d(TAG, "deleteLessonFromCloudDatabase inserted uri:" + uri);
                Log.d(TAG, "deleteLessonFromCloudDatabase fileReference:" + fileReference);
            }

        } while (cursor.moveToNext());

        cursor.close();


        // Delete the text from Database
        final String documentName = String.format(Locale.US, "%s_%03d",
                userUid, lesson_id);

        Log.v(TAG, "deleteLessonFromCloudDatabase documentName:" + documentName);

        // Delete in the Firebase Database
        mFirebaseDatabase.collection("lessons").document(documentName)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "deleteLessonFromCloudDatabase: DocumentSnapshot successfully deleted!");
                        mCallback.onDeleteCloudDatabaseSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "deleteLessonFromCloudDatabase: Error deleting document", e);
                        mCallback.onDeleteCloudDatabaseFailure(e);
                    }
                });


    }


    // Process the deletion of the files in the Firebase Storage
    // Load the images uri's from the table my_cloud_files_to_delete into the images array
    // These uri's were stored when the part was locally deleted
    public void deleteImageFilesFromStorage(long lesson_id) {

        Log.d(TAG, "deleteImageFilesFromStorage lesson_id:" + lesson_id);

        contentResolver = mContext.getContentResolver();
        String selection = LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_LESSON_ID + "=?";
        String selectionArgs[] = {Long.toString(lesson_id)};
        Cursor mCursor = contentResolver.query(
                LessonsContract.MyCloudFilesToDeleteEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        if (mCursor == null) {
            Log.d(TAG, "Failed to get cursor for group_lesson_parts");
            return;
        }

        int nRows = mCursor.getCount();
        mCursor.moveToFirst();

        // Get all the parts and sore all image cloud uri's in an array of Image instances
        List<Image> images = new ArrayList<>();

        if (nRows > 0) {
            do {

                long itemId = mCursor.getLong(mCursor.
                        getColumnIndex(LessonsContract.MyCloudFilesToDeleteEntry._ID));
                long lessonId = mCursor.getLong(mCursor.
                        getColumnIndex(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_LESSON_ID));
                String cloudImageRef = mCursor.getString(mCursor.
                        getColumnIndex(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_FILE_REFERENCE));

                if (cloudImageRef != null) {
                    // Set the values in the Image instance
                    Image image = new Image();
                    image.setPart_id(itemId);
                    image.setLesson_id(lessonId);
                    image.setCloud_uri(cloudImageRef);
                    // Store the instance in the array
                    images.add(image);
                }
                // get the next image
            } while (mCursor.moveToNext());
        }

        // close the cursor
        mCursor.close();

        if (!(nRows > 0)) {
            mCallback.onDeleteCloudImagesSuccess(nRows);
            return;
        }

        Log.d(TAG, "deleteImageFilesFromStorage images to delete:" + images.toString());

        // for each image, delete from cloud

        // This has an activity scope, so will unregister when the activity stops
        mStorageReference = mFirebaseStorage.getReference().child(userUid);

        for(Image imageToDelete: images) {

            final String fileRef = imageToDelete.getCloud_uri();
            final long _id = imageToDelete.getPart_id();

            storageRef = mStorageReference.child(fileRef);

            // Create a reference to the file to delete
            Log.d(TAG, "deleteImageFilesFromStorage storageRef:" + storageRef.toString());

            // Delete the images
            storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    // File deleted successfully

                    // delete from table
                    ContentResolver contentResolver = mContext.getContentResolver();
                    /* The delete method deletes the previously inserted row by its _id */
                    Uri uriToDelete = LessonsContract.MyCloudFilesToDeleteEntry.CONTENT_URI.buildUpon()
                            .appendPath("" + _id + "").build();
                    Log.d(TAG, "Uri to delete:" + uriToDelete.toString());
                    int numberOfRowsDeleted = contentResolver.delete(uriToDelete, null, null);
                    Log.d(TAG, "numberOfRowsDeleted:" + numberOfRowsDeleted);

                    addToLog("Image file " + fileRef + "deleted from cloud successfully!");

                    mCallback.onDeleteCloudImagesSuccess(numberOfRowsDeleted);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {

                    addToLog("Image file " + fileRef + " error while deleting from cloud:" +
                            "\n" + exception.getMessage());

                    mCallback.onDeleteCloudImagesFailure(exception);

                }
            });
        }
    }


    public void deleteImageLocalFilesOfGroupLesson(long lessonId) {

        // Query the parts table with the same lesson_id
        // find the uri's of the images to delete
        // delete the local files
        contentResolver = mContext.getContentResolver();

        String selection = LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID + "=?";
        String[] selectionArgs = {Long.toString(lessonId)};
        Cursor cursor = contentResolver.query(
                LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        if (cursor == null) {
            return;
        }

        long nRows = cursor.getCount();

        if (nRows == 0) {
            return;
        }

        // Moves to the first part of that lesson
        cursor.moveToFirst();

        do {

            String localImageUri = cursor.getString(cursor.
                    getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
            String localVideoUri = cursor.getString(cursor.
                    getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));

            Log.d(TAG, "deleteImageLocalFilesOfGroupLesson localImageUri:" + localImageUri);
            Log.d(TAG, "deleteImageLocalFilesOfGroupLesson localVideoUri:" + localVideoUri);

            if (localImageUri != null) {
                Uri uri = Uri.parse(localImageUri);
                String fileName = uri.getLastPathSegment();
                Log.d(TAG, "localImageUri file name:" + fileName);
                File file = new File(mContext.getFilesDir(), fileName);
                Log.d(TAG, "localImageUri file exists:" + file.exists());
                try {
                    boolean fileDeleted = file.delete();
                    Log.d(TAG, "localImageUri fileDeleted:" + fileDeleted);
                } catch (Exception e) {
                    Log.e(TAG, "localImageUri:" + e.getMessage());
                }

            }

            if (localVideoUri != null) {
                Uri uri = Uri.parse(localVideoUri);
                String fileName = uri.getLastPathSegment();
                Log.d(TAG, "localVideoUri file name:" + fileName);
                File file = new File(mContext.getFilesDir(), fileName);
                Log.d(TAG, "localVideoUri file exists:" + file.exists());
                try {
                    boolean fileDeleted = file.delete();
                    Log.d(TAG, "localVideoUri fileDeleted:" + fileDeleted);
                } catch (Exception e) {
                    Log.e(TAG, "localVideoUri:" + e.getMessage());
                }
            }

            // get the next part
        } while (cursor.moveToNext());

        cursor.close();
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {

        Log.d(TAG, "onSaveInstanceState: saving instance state");

        outState.putString(USER_UID, this.userUid);

        // save data about the files that are being uploaded
        int uploadingSavedItems = 0;

        if (null != uploadingImages) {
            for (int i = 0; i < uploadingImages.size(); i++) {

                UploadingImage uploadingImage = uploadingImages.get(i);

                if (uploadingImage.getStorageRefString() != null) {
                    outState.putString(UPLOADING_REFERENCE + i, uploadingImage.getStorageRefString());
                }
                if (uploadingImage.getPartId() != null) {
                    outState.putLong(UPLOADING_PART_ID + i, uploadingImage.getPartId());
                }
                if (uploadingImage.getImageType() != null) {
                    outState.putString(UPLOADING_IMAGE_TYPE + i, uploadingImage.getImageType());
                }
                if (uploadingImage.getFileUriString() != null) {
                    outState.putString(UPLOADING_FILE_URI_STRING + i, uploadingImage.getFileUriString());
                }
                if (uploadingImage.getFileUriString() != null) {
                    outState.putLong(UPLOADING_LESSON_ID + i, uploadingImage.getLessonId());
                }

                uploadingSavedItems++;
            }
        }

        outState.putInt(UPLOADING_SAVED_ITEMS, uploadingSavedItems);


        // Save data about the files that are being downloaded
        int downloadingSavedItems = 0;

        if (null != downloadingImages) {
            for (int i = 0; i < downloadingImages.size(); i++) {

               DownloadingImage downloadingImage = downloadingImages.get(i);

                if (downloadingImage.getStorageRefString() != null) {
                    outState.putString(DOWNLOADING_REFERENCE + i, downloadingImage.getStorageRefString());
                }
                if (downloadingImage.getPartId() != null) {
                    outState.putLong(DOWNLOADING_PART_ID + i, downloadingImage.getPartId());
                }
                if (downloadingImage.getImageType() != null) {
                    outState.putString(DOWNLOADING_IMAGE_TYPE + i, downloadingImage.getImageType());
                }
                if (downloadingImage.getFileUriString() != null) {
                    outState.putString(DOWNLOADING_FILE_URI_STRING + i, downloadingImage.getFileUriString());
                }
                if (downloadingImage.getFileUriString() != null) {
                    outState.putLong(DOWNLOADING_LESSON_ID + i, downloadingImage.getLessonId());
                }

                downloadingSavedItems++;
            }
        }

        outState.putInt(DOWNLOADING_SAVED_ITEMS, downloadingSavedItems);


        super.onSaveInstanceState(outState);
    }


    // Helper method called by the onCreate when recovering the fragment saved state
    // This method will resume and process all pending tasks
//    private void processUploadingPendingTasks(Bundle savedInstanceState) {
//
//        int uploadingSavedItems = savedInstanceState.getInt(UPLOADING_SAVED_ITEMS);
//
//        if (!(uploadingSavedItems > 0)) {
//            return;
//        }
//
//        // Will resume each task saved
//        for (int i = 0; i < uploadingSavedItems; i++){
//
//            // Each image has a unique storage reference, so a unique task is possible
//           final String stringRef = savedInstanceState.getString(UPLOADING_REFERENCE + i);
//           final long partId = savedInstanceState.getLong(UPLOADING_PART_ID + i);
//           final String imageType = savedInstanceState.getString(UPLOADING_IMAGE_TYPE + i);
//           final String uriString = savedInstanceState.getString(UPLOADING_FILE_URI_STRING + i);
//           final long lessonId = savedInstanceState.getLong(UPLOADING_LESSON_ID + i);
//
//            // If there was an upload in progress, get its reference and create a new StorageReference
//            if (stringRef == null) {
//                return;
//            }
//
//            mStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);
//
//            // Find all UploadTasks under this StorageReference (in this example, there should be one)
//            List<UploadTask> tasks = mStorageReference.getActiveUploadTasks();
//            if (tasks.size() > 0) {
//
//                // cancel all tasks above the first task for this storageRef (this unique image)
//                if (tasks.size() > 1) {
//                    for (int j = 1; j < tasks.size(); j++) {
//                        tasks.get(j).cancel();
//                    }
//                }
//
//                // resume the first task
//
//                // Get the task monitoring the upload
//                UploadTask task = tasks.get(0);
//
//                final int currentImg = i;
//                final int finalImg = uploadingSavedItems - 1;
//
//                // Observe state change events such as progress, pause, and resume
//                task.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
//                        Log.d(TAG, "Image part id: "  + partId +
//                                " (resumed) upload is " + String.format(Locale.US, "%.2f", progress) + "% done.");
//                        addToLog("Image part id: "  + partId +
//                                " (resumed) upload is " + String.format(Locale.US, "%.2f", progress) + "% done.");
//                    }
//                }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
//                        Log.d(TAG, "Image part id: "  +partId + " (resumed) upload is paused.");
//                        addToLog("Image part id: "  +partId + " (resumed) upload is paused.");
//                    }
//                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//                    @Override
//                    public void onSuccess(UploadTask.TaskSnapshot state) {
//                        //call helper function to handle the event.
//                        handleUploadTaskSuccess(
//                                state,
//                                partId,
//                                imageType,
//                                uriString,
//                                lessonId);
//
//                        Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);
//
//                        mCallback.onUploadImageSuccess();
//
//                    }
//                }).addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception exception) {
//                        // Handle unsuccessful uploads
//                        handleUploadTaskFailure(
//                                exception,
//                                partId,
//                                imageType,
//                                uriString,
//                                lessonId);
//
//                        Log.e(TAG, "Error: currentImg:" + currentImg + " finalImg:" + finalImg, exception);
//
//                        mCallback.onUploadImageFailure(exception);
//                    }
//                });
//
//            }
//        }
//    }


    // This handles the upload images task success
//    private void handleUploadTaskSuccess(UploadTask.TaskSnapshot state,
//                                         long partId,
//                                         String imageType,
//                                         String fileUriString,
//                                         long lessonId) {
//
//        Log.d(TAG, "handleUploadTaskSuccess complete. Uploaded " + imageType + " id:" +
//                partId + " of lesson id:" + lessonId);
//
//        if (state.getMetadata() != null) {
//            Log.d(TAG, "bucket:" + state.getMetadata().getBucket());
//        }
//
//        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
//                .format(new Date());
//        addToLog( time_stamp + ":\nLesson id:" + lessonId + "\nSuccessfully uploaded " +
//                imageType + " part id: " + partId + "\nfile:" + fileUriString);
//
//        // Save the photoUrl in the database
//        ContentValues contentValues = new ContentValues();
//        // Put the lesson title into the ContentValues
//
//        // test for integrity
//        if(imageType == null) {
//            return;
//        }
//
//        if (imageType.equals(VIDEO)) {
//            contentValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI,
//                    fileUriString);
//        } else if (imageType.equals(IMAGE)) {
//            contentValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI,
//                    fileUriString);
//        }
//
//        // Insert the content values via a ContentResolver
//        contentResolver = mContext.getContentResolver();
//        Uri updateUri = ContentUris.withAppendedId(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
//                partId);
//        long numberOfImagesUpdated = contentResolver.update(
//                updateUri,
//                contentValues,
//                null,
//                null);
//
//        Log.d(TAG, "Updated " + numberOfImagesUpdated + " item(s) in the database");
//
//    }


    // This handles the upload images task failure
//    private void handleUploadTaskFailure(Exception e,
//                                         long partId,
//                                         String imageType,
//                                         String fileUriString,
//                                         long lessonId) {
//
//        Log.e(TAG, "handleUploadTaskFailure failure: " + imageType + " id:" +
//                partId + " of lesson id:" + lessonId + " fileUriString:" + fileUriString, e);
//        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
//                .format(new Date());
//        addToLog(time_stamp + ":\nLesson id:" + lessonId + "\nUpload failure " + imageType +
//                " part id: " + partId + "\nfile:" + fileUriString + "\nError:" + e.getMessage());
//
//    }


    // Helper method called by the onCreate when recovering the fragment saved state
    // This method will resume and process all pending tasks
    private void processDownloadingPendingTasks(Bundle savedInstanceState) {

        int downloadingSavedItems = savedInstanceState.getInt(UPLOADING_SAVED_ITEMS);

        if (!(downloadingSavedItems > 0)) {
            return;
        }

        // Will resume each task saved
        for (int i = 0; i < downloadingSavedItems; i++){

            // Each image has a unique storage reference, so a unique task is possible
            final String stringRef = savedInstanceState.getString(DOWNLOADING_REFERENCE + i);
            final long partId = savedInstanceState.getLong(DOWNLOADING_PART_ID + i);
            final String imageType = savedInstanceState.getString(DOWNLOADING_IMAGE_TYPE + i);
            final String uriString = savedInstanceState.getString(DOWNLOADING_FILE_URI_STRING + i);
            final long lessonId = savedInstanceState.getLong(DOWNLOADING_LESSON_ID + i);

            // If there was an download in progress, get its reference and create a new StorageReference
            if (stringRef == null) {
                return;
            }

            mStorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);

            // Find all UploadTasks under this StorageReference (in this example, there should be one)
            List<FileDownloadTask> tasks = mStorageReference.getActiveDownloadTasks();
            if (tasks.size() > 0) {

                // cancel all tasks above the first task for this storageRef (this unique image)
                if (tasks.size() > 1) {
                    for (int j = 1; j < tasks.size(); j++) {  tasks.get(j).cancel();  }
                }

                // resume the first task
                // Get the task monitoring the upload
                FileDownloadTask task = tasks.get(0);

                final int currentImg = i;
                final int finalImg = downloadingSavedItems - 1;

                // Observe state change events such as progress, pause, and resume
                task.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        Log.d(TAG, "Image part id: "  + partId +
                                " (resumed) download is " + String.format(Locale.US, "%.2f", progress) + "% done.");
                        addToLog("Image part id: "  + partId +
                                " (resumed) download is " + String.format(Locale.US, "%.2f", progress) + "% done.");
                    }
                }).addOnPausedListener(new OnPausedListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onPaused(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "Image part id: "  + partId + " (resumed) download is paused.");
                        addToLog("Image part id: " + partId + " (resumed) download is paused.");
                    }
                }).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot state) {
                        //call helper function to handle the event.
                        handleDownloadTaskSuccess(
                                partId,
                                imageType,
                                uriString,
                                lessonId);

                        Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);

                        mCallback.onDownloadImageSuccess();

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        handleDownloadTaskFailure(
                                exception,
                                partId,
                                imageType,
                                uriString,
                                lessonId);

                        Log.e(TAG, "Error: currentImg:" + currentImg + " finalImg:" + finalImg, exception);

                        mCallback.onDownloadImageFailure(exception);

                    }
                });
            }
        }
    }



    // This handles the upload images task success
    private void handleDownloadTaskSuccess(long partId,
                                           String imageType,
                                           String fileUriString,
                                           long lessonId) {

        Log.d(TAG, "handleDownloadTaskSuccess complete. Downloaded " + imageType + " id:" +
                partId + " of lesson id:" + lessonId);
        Log.d(TAG, "handleDownloadTaskSuccess fileUriString " + fileUriString);

        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());
        addToLog( time_stamp + ":\nLesson id:" + lessonId + "\nSuccessfully downloaded " +
                imageType + " part id: " + partId + "\nfile:" + fileUriString);

        // Save the photoUrl in the database
        ContentValues contentValues = new ContentValues();
        // Put the lesson title into the ContentValues

        // test for integrity
        if(imageType == null) { return; }

        if (imageType.equals(VIDEO)) {
            contentValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI,
                    fileUriString);
        } else if (imageType.equals(IMAGE)) {
            contentValues.put(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI,
                    fileUriString);
        }

        // Insert the content values via a ContentResolver
        contentResolver = mContext.getContentResolver();
        Uri updateUri = ContentUris.withAppendedId(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                partId);
        long numberOfImagesUpdated = contentResolver.update(
                updateUri,
                contentValues,
                null,
                null);

        Log.d(TAG, "Updated " + numberOfImagesUpdated + " item(s) in the database");

    }


    // This handles the upload images task failure
    private void handleDownloadTaskFailure(Exception e,
                                           long partId,
                                           String imageType,
                                           String fileUriString,
                                           long lessonId) {

        Log.e(TAG, "handleDownloadTaskFailure failure: " + imageType + " id:" +
                partId + " of lesson id:" + lessonId + " fileUriString:" + fileUriString, e);
        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());
        addToLog(time_stamp + ":\nLesson id:" + lessonId + "\nDownload failure " + imageType +
                " part id: " + partId + "\nfile:" + fileUriString + "\nError:" + e.getMessage());

    }


    // Add data to the log table and limit its size
    public void addToLog(String logText) {

        if(logText.equals(RELEASE)) {

            // Limit the size of the log table
            contentResolver = mContext.getContentResolver();
            Cursor mCursor = contentResolver.query(LessonsContract.MyLogEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null);

            if (mCursor != null) {

                // find the size of the table
                int nRows = mCursor.getCount();
                mCursor.moveToFirst();

                // limit the number of deletions
                int maxToDelete = nRows / 5;
                List<Long> idsToDelete = new ArrayList<>();
                // get the id_s of the rows to delete and save in the array
                for (int i = 0; i < maxToDelete; i++) {
                    idsToDelete.add(mCursor.getLong(mCursor.getColumnIndex(LessonsContract.MyLogEntry._ID)));
                    mCursor.moveToNext();
                }

                mCursor.close();

                // delete that rows
                int count = 0;
                while (nRows > MAX_ROWS_LOG_TABLE && count < idsToDelete.size()) {
                    long log_id = idsToDelete.get(count);
                    // delete the row with that log_id
                    Uri uriToDelete = LessonsContract.MyLogEntry.CONTENT_URI.buildUpon()
                            .appendPath(Long.toString(log_id)).build();
                    if (uriToDelete != null) {
                        Log.d(TAG, "uriToDelete:" + uriToDelete.toString());
                        int nRowsDeleted = contentResolver.delete(uriToDelete, null, null);
                        Log.d(TAG, "addToLog nRowsDeleted:" + nRowsDeleted);
                        nRows--;
                    }
                    // count the number of tries
                    count++;
                }
            }

            if (mCursor != null) {
                mCursor.close();
            }

            // write buffer to database
            for (int j = 0; j < logBuffer.size(); j++) {
                // Now add the new value to the log table
                ContentValues contentValues = new ContentValues();
                contentValues.put(LessonsContract.MyLogEntry.COLUMN_LOG_ITEM_TEXT, logBuffer.get(j));

                // Insert the content values via a ContentResolver
                Uri uri = contentResolver.insert(LessonsContract.MyLogEntry.CONTENT_URI, contentValues);

                if (uri == null) {
                    Log.e(TAG, "addToLog: error in inserting item on log",
                            new Exception("addToLog: error in inserting item on log"));
                }
            }

            logBuffer.clear();

        } else {

            logBuffer.add(logText);

        }

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


}


