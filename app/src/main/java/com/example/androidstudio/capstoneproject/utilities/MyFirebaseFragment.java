package com.example.androidstudio.capstoneproject.utilities;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.example.androidstudio.capstoneproject.data.DownloadingImage;
import com.example.androidstudio.capstoneproject.data.Image;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.data.UploadingImage;
import com.example.androidstudio.capstoneproject.sync.MyLog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MyFirebaseFragment extends Fragment {


    private static final String TAG = MyFirebaseFragment.class.getSimpleName();

    private static final String USER_UID = "userUid";

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

    private MyLog myLog;



    // Listener for sending information to the Activity
    public interface OnCloudListener {

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

        myLog = new MyLog(mContext);

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
        }

    }


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

                    myLog.addToLog("Image file " + fileRef + "deleted from cloud successfully!");

                    mCallback.onDeleteCloudImagesSuccess(numberOfRowsDeleted);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {

                    myLog.addToLog("Image file " + fileRef + " error while deleting from cloud:" +
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

    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {

        Log.d(TAG, "onSaveInstanceState: saving instance state");

        outState.putString(USER_UID, this.userUid);

        super.onSaveInstanceState(outState);
    }


}


