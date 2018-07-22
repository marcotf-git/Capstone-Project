package com.example.androidstudio.capstoneproject.sync;

import android.app.Activity;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.androidstudio.capstoneproject.data.Image;
import com.example.androidstudio.capstoneproject.data.Lesson;
import com.example.androidstudio.capstoneproject.data.LessonPart;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.utilities.NotificationUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class handle the deletion of the user lessons from cloud, from the Firebase
 * Database (text) and from the Firebase Storage (image/video files).
 */
public class MyDeleteService extends IntentService {

    private static final String TAG = MyDeleteService.class.getSimpleName();

    public static final String ACTION =
            "com.example.androidstudio.capstoneproject.sync.MyDeleteService"; // use same action


    private static final String SELECTED_LESSON_ID = "selectedLessonId";
    private static final String USER_UID = "userUid";

    // Automatic unregister listeners
    //private FirebaseFirestore mFirebaseDatabase;
    private FirebaseDatabase mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStorageReference;
    private StorageReference storageRef;

    private List<String> messages = new ArrayList<>();

    private int nImagesToDelete;
    private int nImagesDeleted;

    private MyLog myLog;

    private Context mContext;


    // Default constructor
    public MyDeleteService() { super("MyDeleteService"); }

    @Override
    public void onCreate() {
        super.onCreate();
    }


     // this is called by Intent (in MainActivity)
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        mContext = this;

        myLog = new MyLog(this);
        messages = new ArrayList<>();

        long lesson_id = -1;
        String userUid = null;

        // Initialize Firebase components
        //mFirebaseDatabase = FirebaseFirestore.getInstance();
//        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
////                .setTimestampsInSnapshotsEnabled(true)
////                .build();
////        mFirebaseDatabase.setFirestoreSettings(settings);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        // Recover information from caller activity
        if (intent != null && intent.hasExtra(SELECTED_LESSON_ID)) {
            lesson_id = intent.getLongExtra(SELECTED_LESSON_ID, -1);
        }

        if (intent != null && intent.hasExtra(USER_UID)) {
            userUid = intent.getStringExtra(USER_UID);
        }

        if (lesson_id!= -1 && userUid != null) {
            try {
                deleteLessonFromCloudDatabase(userUid, lesson_id);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                myLog.addToLog(e.getMessage());
                messages.add(e.getMessage());
                sendMessages();
                messages.add("DELETE LESSON FINISHED WITH ERROR");
                sendMessages();
            }
        }

    }


    // Helper method to delete a lesson text from cloud database
    // and after, delete the images from cloud storage.
    private void deleteLessonFromCloudDatabase(final String userUid, final long lesson_id) {

        // Test the parameters
        if((userUid == null) || (lesson_id == -1)) {
            Log.e(TAG, "deleteLessonFromCloudDatabase: failed to get parameters " +
                    "(userUid or lesson_id");
            messages.add("Failed to get parameters (internal failure)");
            sendMessages();
            messages.add("DELETE LESSON FINISHED WITH ERROR");
            sendMessages();
            return;
        }

        // First, get the cloud file reference from table lesson parts and save it in the form
        // "images/001/file_name" or "videos/001/file_name", where 001 is the lesson_id
        // (not the part_id) in the var fileReference, in the table my_cloud_files_to_delete.

        ContentResolver contentResolver = mContext.getContentResolver();
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


        if ((cursor != null) && (cursor.getCount() >0)) {

            // Save the Firebase Storage file reference in this var
            String fileReference;

            cursor.moveToFirst();

            do {
                // get info to build the fileRef
                String cloud_image_uri = cursor.getString(cursor.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));
                String cloud_video_uri = cursor.getString(cursor.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI));

                // build the fileRef for images and store
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

                // build the fileRef for videos and store
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

        }

        // Delete the lesson from Firebase Database

        // The root reference
        DatabaseReference databaseRef = mFirebaseDatabase.getReference();

        // The lesson reference is the the (userUid).(lesson_id formatted as %03d)
        final DatabaseReference lessonRef = databaseRef
                .child(userUid)
                .child(String.format( Locale.US, "%03d", lesson_id));

        // Write the object and add the listeners
        lessonRef.setValue(null)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        Log.d(TAG, "deleteLessonFromCloudDatabase: DocumentSnapshot successfully deleted!");

                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());

                        myLog.addToLog( time_stamp + ":\nLesson id:" + lesson_id + "\nText " +
                                "successfully deleted from cloud");
                        myLog.addToLog("Now it will try to delete the images/videos...");

                        deleteImageFilesFromStorage(userUid, lesson_id);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "deleteLessonFromCloudDatabase: Error deleting document", e);

                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());

                        myLog.addToLog( time_stamp + ":\nLesson id:" + lesson_id + "\nError " +
                                "on deleting the text from cloud:" + "\n" + e.getMessage());

                        myLog.addToLog("Now it will try to delete the images/videos...");

                        deleteImageFilesFromStorage(userUid, lesson_id);
                    }
                });


//        // Delete the text from Database
//        final String documentName = String.format(Locale.US, "%s_%03d",
//                userUid, lesson_id);
//
//        Log.v(TAG, "deleteLessonFromCloudDatabase documentName:" + documentName);

        // Delete in the Firebase Database
//        mFirebaseDatabase.collection("lessons").document(documentName)
//                .delete()
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Log.d(TAG, "deleteLessonFromCloudDatabase: DocumentSnapshot successfully deleted!");
//
//                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
//                                .format(new Date());
//
//                        myLog.addToLog( time_stamp + ":\nLesson id:" + lesson_id + "\nText " +
//                                "successfully deleted from cloud");
//                        myLog.addToLog("Now it will try to delete the images/videos...");
//
//                        deleteImageFilesFromStorage(userUid, lesson_id);
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//
//                        Log.e(TAG, "deleteLessonFromCloudDatabase: Error deleting document", e);
//
//                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
//                                .format(new Date());
//
//                        myLog.addToLog( time_stamp + ":\nLesson id:" + lesson_id + "\nError " +
//                                "on deleting the text from cloud:" + "\n" + e.getMessage());
//
//                        myLog.addToLog("Now it will try to delete the images/videos...");
//
//                        deleteImageFilesFromStorage(userUid, lesson_id);
//                    }
//                });

    }


    // Process the deletion of the files in the Firebase Storage
    // Load the images uri's from the table my_cloud_files_to_delete into the images array
    // These uri's were stored when the part was locally deleted
    public void deleteImageFilesFromStorage(String userUid, final long lesson_id) {

        Log.d(TAG, "deleteImageFilesFromStorage lesson_id:" + lesson_id);

        // Get all the parts (with the same lesson_id) and sore all image cloud uri's in an array
        // of Image instances
        ContentResolver contentResolver = mContext.getContentResolver();
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
            myLog.addToLog("Failed to get cursor for MyCloudFilesToDelete");
            messages.add("DELETE LESSON FINISHED WITH ERROR");
            sendMessages();
            return;
        }

        int nRows = mCursor.getCount();
        mCursor.moveToFirst();

        // Store in the array
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
            myLog.addToLog("No images to delete from cloud.");
            myLog.addToLog("Task has finished.");
            messages.add("DELETE LESSON FINISHED OK");
            sendMessages();
            return;
        }

        // count the images to control
        nImagesToDelete = nRows;
        nImagesDeleted = 0;

        Log.d(TAG, "deleteImageFilesFromStorage images to delete:" + images.toString());

        // Now, for each image uri stored, delete it from cloud

        // This has an global scope
        mStorageReference = mFirebaseStorage.getReference().child(userUid);

        for(Image imageToDelete: images) {

            final String fileRef = imageToDelete.getCloud_uri();
            final long part_id = imageToDelete.getPart_id();

            storageRef = mStorageReference.child(fileRef);

            // Create a reference to the file to delete
            Log.d(TAG, "deleteImageFilesFromStorage storageRef:" + storageRef.toString());

            storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {

                    nImagesDeleted++;

                    handleDeleteTaskSuccess(part_id, fileRef, lesson_id);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {

                    nImagesDeleted++;

                    handleDeleteTaskFailure(exception, part_id, fileRef, lesson_id);

                }
            });
        }

    }


    // Handle the download image task success
    private void handleDeleteTaskSuccess(long partId,
                                         String fileRef,
                                         long lessonId) {

        Log.d(TAG, "handleDeleteTaskSuccess complete. Deleted image id:" +
                partId + " of lesson id:" + lessonId);

        Log.d(TAG, "handleDeleteTaskSuccess fileRef " + fileRef);

        myLog.addToLog( "delete count:" + nImagesDeleted + "/" + nImagesToDelete);

        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());
        myLog.addToLog( time_stamp + ":\nLesson id:" + lessonId + "\nSuccessfully deleted " +
                "image part id: " + partId + "\nfile:" + fileRef);

        // File deleted successfully from cloud
        // Now, delete it from the local table my_cloud_files_to_delete
        ContentResolver contentResolver = mContext.getContentResolver();

        /* The delete method deletes the previously inserted row by its _id */
        Uri uriToDelete = LessonsContract.MyCloudFilesToDeleteEntry.CONTENT_URI.buildUpon()
                .appendPath("" + partId + "").build();

        Log.d(TAG, "Uri to delete:" + uriToDelete.toString());

        int numberOfRowsDeleted = contentResolver.delete(uriToDelete, null, null);

        Log.d(TAG, "numberOfRowsDeleted:" + numberOfRowsDeleted);

        if (numberOfRowsDeleted > 0) {
            myLog.addToLog("Image id:" + partId + " deleted from local reference table successfully!");
        } else {
            myLog.addToLog("Image id:" + partId + " error on deleting from local reference table!");
        }


        if (nImagesToDelete == nImagesDeleted) {
            // all tasks finished
            String message = "ALL DELETE TASKS HAVE FINISHED";
            myLog.addToLog(message);

            // Trigger the snack bar in MainActivity
            messages.add("DELETE LESSON FINISHED OK");
            sendMessages();

        }

    }


    // handle the delete image task failure
    private void handleDeleteTaskFailure(Exception e,
                                           long partId,
                                           String fileRef,
                                           long lessonId) {

        Log.e(TAG, "handleDeleteTaskFailure failure: image id:" +
                partId + " of lesson id:" + lessonId + " fileRef:" + fileRef, e);

        myLog.addToLog( "delete count:" + nImagesDeleted + "/" + nImagesToDelete);

        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());
        myLog.addToLog(time_stamp + ":\nError: lesson id:" + lessonId + "\nDeletion failure image" +
                " part id: " + partId + "\nfile:" + fileRef + "\nMessage:" + e.getMessage());

        if (nImagesToDelete == nImagesDeleted) {
            // all tasks finished
            String message = "ALL DELETE TASKS HAVE FINISHED";
            myLog.addToLog(message);

            // Trigger the snack bar in MainActivity
            messages.add("DELETE LESSON FINISHED WITH ERROR");
            sendMessages();

        }

    }


    private void sendMessages() {

        Log.d(TAG, "sendMessages messages:" + messages.toString());

        Intent in = new Intent(ACTION);
        in.putExtra("resultCode", Activity.RESULT_OK);
        in.putExtra("resultValue", messages.toString());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(in);

        messages.clear();
    }


}
