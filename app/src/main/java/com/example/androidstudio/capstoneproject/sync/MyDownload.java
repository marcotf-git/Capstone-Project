package com.example.androidstudio.capstoneproject.sync;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.androidstudio.capstoneproject.data.DownloadingImage;
import com.example.androidstudio.capstoneproject.data.Image;
import com.example.androidstudio.capstoneproject.data.Lesson;
import com.example.androidstudio.capstoneproject.data.LessonPart;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
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
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MyDownload {


    public static final String ACTION =
            "com.example.androidstudio.capstoneproject.sync.MyDownload";

    private static final String TAG = MyDownloadService.class.getSimpleName();


    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";
    private static final String VIDEO = "video";
    private static final String IMAGE = "image";

    private FirebaseFirestore mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;

    private List<String> messages = new ArrayList<>();

    private int nImagesToDownload;
    private int nImagesDownloaded;

    private String databaseVisibility;
    private String userUid;

    private MyLog myLog;

    private Context mContext;



    MyDownload(Context context, String databaseVisibility, String userUid) {

        mContext = context;
        this.databaseVisibility = databaseVisibility;
        this.userUid = userUid;

        mFirebaseDatabase = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mFirebaseDatabase.setFirestoreSettings(settings);
        mFirebaseStorage = FirebaseStorage.getInstance();

        myLog = new MyLog(context);

    }



    // Helper method for refreshing the database from Cloud Firestore
    // Do not delete if existing in user table
    // Delete all in the group table (data and local files)
    public void refreshDatabase() {

        if((userUid == null) || !(databaseVisibility == null)) {
            Log.e(TAG, "uploadImagesAndDatabase: failed to get parameters " +
                    "(userUid or databaseVisibility");
            messages.add("Failed to get parameters (internal failure)");
            sendMessages();
            messages.add("UPLOAD LESSON FINISHED WITH ERROR");
            sendMessages();
            return;
        }

        // Get multiple documents (all the data in the database)
        mFirebaseDatabase.collection("lessons")
                //.whereEqualTo("field_name", true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            messages.add("onComplete collection task.isSuccessful");
                            //mCallback.onDownloadDatabaseSuccess();
                            sendMessages();

                            if (databaseVisibility.equals(USER_DATABASE)) {

                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, "refreshDatabase onComplete document.getId():" +
                                            document.getId() + " => " + document.getData());
                                    Lesson lesson = document.toObject(Lesson.class);
                                    String jsonString = serialize(lesson);
                                    Log.v(TAG, "refreshDatabase onComplete lesson jsonString:"
                                            + jsonString);
                                    // refresh the lessons of the local user on its separate table
                                    // this gives more security to the database
                                    // --> filter by the user uid of the lesson downloaded
                                    if (userUid.equals(lesson.getUser_uid())) {
                                        refreshUserLesson(lesson);
                                    }
                                }

                            } else if (databaseVisibility.equals(GROUP_DATABASE)) {

                                // refresh the lessons of the group table on its separate table
                                // write the data to the database table
                                // --> pass all the data
                                refreshGroupLessons(task);

                                // download the images and save in local files
                                // write the files uri's in the database table, in the parts table
                                downloadGroupImages(); // this will use the counter

                                // send the messages to the activity
                                sendMessages();
                            }

                        } else {
                            Log.d(TAG, "Error in getting documents: ", task.getException());
                            if (task.getException() != null) {
                                String message = task.getException().getMessage();
                                messages.add(message);
                                sendMessages();
                            } else {
                                String message = "Error while querying Firebase Database";
                                messages.add(message);
                                sendMessages();
                            }
                        }
                    }
                });

    }



    // Helper method called by refreshDatabase
    // Save the data in the database
    private void refreshUserLesson(Lesson lesson) {

        ContentResolver contentResolver = mContext.getContentResolver();

        Log.v(TAG, "refreshUserLesson lesson_id:" + lesson.getLesson_id());

        // query the local database to see if find the lesson with the _id
        // delete it and save another if found; create if didn't exist
        Uri queryUri;
        queryUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI,
                lesson.getLesson_id());

        Cursor mCursor;
        if (queryUri != null) {mCursor = contentResolver.query(queryUri,
                null,
                null,
                null,
                null);
        } else {
            Log.v(TAG, "Error: null cursor");
            messages.add("Error in saving documents downloaded from Firestore (null cursor)");
            sendMessages();
            return;
        }

        int nRows = -1;
        if (mCursor != null) {
            mCursor.moveToFirst();
            nRows = mCursor.getCount();
            mCursor.close();
        }

        if (nRows > 0) {
            // first, delete the parts by the lesson id
            Uri deleteUri = ContentUris.withAppendedId(LessonsContract.MyLessonPartsEntry.CONTENT_URI_BY_LESSON_ID,
                    lesson.getLesson_id());
            int numberOfPartsDeleted = contentResolver.delete(deleteUri,
                    null,
                    null);
            Log.v(TAG, "refreshUserLesson numberOfPartsDeleted:" + numberOfPartsDeleted);

            // second, delete the lesson itself
            Uri deleteLessonUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI,
                    lesson.getLesson_id());
            int numberOfLessonsDeleted = contentResolver.delete(deleteLessonUri,
                    null,
                    null);
            Log.v(TAG, "refreshLesson numberOfLessonsDeleted:" + numberOfLessonsDeleted);
        }


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
            Log.e(TAG, "refreshUserLesson insert uri:null");
            messages.add("Error refreshUserLesson insert uri:null");
            sendMessages();
            return;
        } else {
            Log.v(TAG, "refreshUserLesson insert uri:" + lessonUri.toString());
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
                    Log.e(TAG, "refreshUserLesson: Error on inserting part lessonPart.getTitle():" +
                            lessonPart.getTitle());
                }

                Log.v(TAG, "refreshUserLesson partUri inserted:" + partUri);
            }
        }

        // inform the main activity that the job finishes
        messages.add("REFRESH USER FINISHED OK");
        sendMessages();

    }



    // Helper method called by refreshDatabase
    // Save the data in the database
    // In case of group lessons, clear the existing table and insert new data
    private void refreshGroupLessons(Task<QuerySnapshot> task) {

        Log.v(TAG, "refreshGroupLesson");

        ContentResolver contentResolver = mContext.getContentResolver();

        // first, delete the file images of each lesson
        Cursor mCursor = contentResolver.query(LessonsContract.GroupLessonsEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (mCursor != null && mCursor.moveToFirst()) {
            // delete the images with the help of the deleteImageLocalFilesOfGroupLesson method
            do {
                long lesson_id = mCursor.getLong(mCursor.getColumnIndex(LessonsContract.GroupLessonsEntry._ID));

                // delete the local image files
                deleteImageLocalFilesOfGroupLesson(lesson_id);

                // get the next lesson_id
            } while (mCursor.moveToNext());
        }

        // grants that the cursor is closed
        if (mCursor != null) {
            mCursor.close();
        }

        // now delete the lesson parts table (from group tables)
        int numberOfLessonPartsDeleted = contentResolver.delete(
                LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                null,
                null);

        Log.v(TAG, "refreshGroupLesson numberOfLessonPartsDeleted:" + numberOfLessonPartsDeleted);

        // and delete the lesson table (from group tables)
        int numberOfLessonsDeleted = contentResolver.delete(
                LessonsContract.GroupLessonsEntry.CONTENT_URI,
                null,
                null);

        Log.v(TAG, "refreshGroupLesson numberOfLessonsDeleted:" + numberOfLessonsDeleted);

        // insert the new data
        for (QueryDocumentSnapshot document : task.getResult()) {

            Log.d(TAG, "refreshGroupLessons onComplete document.getId():" +
                    document.getId() + " => " + document.getData());

            // recover the Lesson instance
            Lesson lesson = document.toObject(Lesson.class);
            String jsonString = serialize(lesson);
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


    // Delete the files saved in the local folder
    // Delete the images of the lesson
    private void deleteImageLocalFilesOfGroupLesson(long lessonId) {

        ContentResolver contentResolver = mContext.getContentResolver();

        // Query the parts table with the same lesson_id
        // find the uri's of the images to delete
        // delete the local files

        String selection = LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID + "=?";
        String[] selectionArgs = {Long.toString(lessonId)};
        Cursor cursor = contentResolver.query(
                LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        if (cursor == null) { return; }

        long nRows = cursor.getCount();
        if (nRows == 0) { return; }

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



    // Helper method called by refreshDatabase
    // It will download all the images and save in local files
    // Then, will save the path (local uri's) in the group lesson table
    // The file will be read and showed in the view of the lesson part
    private void downloadGroupImages() {

        // open the group_lesson_parts table and for each row, download the file that has its
        // path stored in the cloud_video_uri or cloud_image_uri, saves the file into local directory
        // and write the file path uri into the local_video_uri or local_image_uri

        // PREPARE TO DOWNLOAD THE FILES

        // Load the images uri's into the images array

        ContentResolver contentResolver = mContext.getContentResolver();

        Cursor mCursor = contentResolver.query(
                LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (mCursor == null) {
            Log.d(TAG, "Failed to get cursor for group_lesson_parts");
            messages.add("Error: downloadGroupImages: Failed to get cursor for group_lesson_parts");
            sendMessages();
            return;
        }

        int nRows = mCursor.getCount();
        mCursor.moveToFirst();
        nImagesToDownload = 0;

        // Get all the parts and sore all image cloud uri's in an array of Image instances
        List<Image> images = new ArrayList<>();
        Image image;

        if (nRows > 0) {

            do {

                Long item_id = mCursor.getLong(mCursor.
                        getColumnIndex(LessonsContract.GroupLessonPartsEntry._ID));
                Long lesson_id = mCursor.getLong(mCursor.
                        getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID));
                String cloudImageUri = mCursor.getString(mCursor.
                        getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));
                String cloudVideoUri = mCursor.getString(mCursor.
                        getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI));

                if (cloudImageUri != null || cloudVideoUri != null) {
                    // Set the values in the Image instance
                    image = new Image();
                    image.setPart_id(item_id);
                    image.setLesson_id(lesson_id);
                    // The priority is the video
                    if (cloudVideoUri != null) {
                        image.setCloud_uri(cloudVideoUri);
                        image.setImageType(VIDEO);
                    } else {
                        image.setCloud_uri(cloudImageUri);
                        image.setImageType(IMAGE);
                    }

                    // Store the instance in the array
                    images.add(image);

                    // THE COUNTER OF IMAGES TO DOWNLOAD
                    nImagesToDownload++;
                }
                // get the next part
            } while (mCursor.moveToNext());

        } else {
            // return if there are no images to download
            mCursor.close();
            myLog.addToLog("DOWNLOAD IMAGES/VIDEOS FINISHED");
            messages.add("DOWNLOAD IMAGES/VIDEOS FINISHED");
            sendMessages();
            return;
        }

        // close the cursor
        mCursor.close();

        // tell to the main activity the state and the number of images to download for control
        //mCallback.onDownloadDatabaseSuccess(nImagesToDownload);
        if (nImagesToDownload == 0) { return; }

        // set the counter that will control the final message about the download
        nImagesDownloaded = 0;

        // BEGIN TO DOWNLOAD THE FILES

        // This has an activity scope, so will unregister when the activity stops
        StorageReference mStorageReference = mFirebaseStorage.getReference();

        for (int imgIndex = 0; imgIndex < images.size(); imgIndex++) {

            final Image imageToDownload = images.get(imgIndex);

            final int currentImg = imgIndex;
            final int finalImg = images.size() - 1;

            // This has global scope to unregister the listeners
            StorageReference storageRef = mStorageReference.child(imageToDownload.getCloud_uri());

            // create a local filename
            String filename = imageToDownload.getImageType() + String.format(Locale.US,
                    "_%03d_%03d", imageToDownload.getLesson_id(), imageToDownload.getPart_id());

            // create a local file: delete if it already exists (in the context of this app)
            File file = new File(mContext.getFilesDir(), filename);
            boolean fileDeleted;
            boolean fileCreated = false;
            if(file.exists()) {
                fileDeleted = file.delete();
                Log.d(TAG, "downloadGroupImages fileDeleted:" + fileDeleted);
            } else {
                try { fileCreated =  file.createNewFile(); }
                catch (IOException e) { e.printStackTrace(); }
            }

            if (!fileCreated) {
                Log.e(TAG, "downloadGroupImages error creating file: file not created");
                String message = "Error: downloadGroupImages error creating file: file not created";
                myLog.addToLog(message);
                messages.add(message);
                sendMessages();
                return;
            }

            URI fileUri = file.toURI();
            final String fileUriString = fileUri.toString();

            Log.d(TAG, "downloadGroupImages fileUriString:" + fileUriString);

            DownloadingImage downloadingImage = new DownloadingImage();
            downloadingImage.setPartId(imageToDownload.getPart_id());
            downloadingImage.setLessonId(imageToDownload.getLesson_id());
            downloadingImage.setImageType(imageToDownload.getImageType());
            downloadingImage.setStorageRefString(storageRef.toString());
            downloadingImage.setFileUriString(fileUriString);

            // Call the task  (storage has activity scope to unregister the listeners when activity stops)
            FileDownloadTask downloadTask = storageRef.getFile(file);

            downloadTask.addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "Image part id: "  + imageToDownload.getPart_id() +
                            " download is " + String.format(Locale.US, "%.2f", progress) + "% done.");
                    myLog.addToLog("Image part id: "  + imageToDownload.getPart_id() +
                            " download is " + String.format(Locale.US, "%.2f", progress) + "% done.");
                }
            }).addOnPausedListener(new OnPausedListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onPaused(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Log.d(TAG, "Image part id: "  + imageToDownload.getPart_id() +
                            " download is paused.");
                    myLog.addToLog( "Image part id: "  + imageToDownload.getPart_id() +
                            " download is paused.");
                }
            }).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot state) {

                    nImagesDownloaded++;

                    //call helper function to handle the event.
                    handleDownloadTaskSuccess(
                            imageToDownload.getPart_id(),
                            imageToDownload.getImageType(),
                            fileUriString,
                            imageToDownload.getLesson_id());

                    Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);


                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {

                    nImagesDownloaded++;

                    // Handle unsuccessful downloads
                    handleDownloadTaskFailure(
                            exception,
                            imageToDownload.getPart_id(),
                            imageToDownload.getImageType(),
                            fileUriString,
                            imageToDownload.getLesson_id());

                    Log.e(TAG, "Error: currentImg:" + currentImg + " finalImg:" + finalImg, exception);

                    String message = "Error:" + exception.getMessage();
                    messages.add(message);
                    sendMessages();

                }
            });
        }

    }



    // This handles the task success
    private void handleDownloadTaskSuccess(long partId,
                                           String imageType,
                                           String fileUriString,
                                           long lessonId) {

        Log.d(TAG, "handleDownloadTaskSuccess complete. Downloaded " + imageType + " id:" +
                partId + " of lesson id:" + lessonId);
        Log.d(TAG, "handleDownloadTaskSuccess fileUriString " + fileUriString);

        myLog.addToLog( "download count:" + nImagesDownloaded + "/" + nImagesToDownload);

        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());
        myLog.addToLog( time_stamp + ":\nLesson id:" + lessonId + "\nSuccessfully downloaded " +
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

        ContentResolver contentResolver =  mContext.getContentResolver();

        // Insert the content values via a ContentResolver
        Uri updateUri = ContentUris.withAppendedId(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                partId);
        long numberOfImagesUpdated = contentResolver.update(
                updateUri,
                contentValues,
                null,
                null);

        Log.d(TAG, "Updated " + numberOfImagesUpdated + " item(s) in the database");

        if (nImagesToDownload == nImagesDownloaded) {
            // all tasks finished
            String message = "ALL DOWNLOAD TASKS HAVE FINISHED";
            myLog.addToLog(message);
            messages.add(message);
            sendMessages();
            if (databaseVisibility.equals(USER_DATABASE)) {
                messages.add("REFRESH USER FINISHED OK");
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                messages.add("REFRESH GROUP FINISHED OK");
            }
            sendMessages();
        }

    }


    // This handles the upload images task failure
    private void handleDownloadTaskFailure(Exception e,
                                           long partId,
                                           String imageType,
                                           String fileUriString,
                                           long lessonId) {

        Log.e(TAG, "handleDownloadTaskFailure failure: " + imageType + " id:" +
                partId + " of lesson id:" + lessonId + " fileUriString:" + fileUriString, e);

        myLog.addToLog( "download count:" + nImagesDownloaded + "/" + nImagesToDownload);

        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());

        myLog.addToLog(time_stamp + ":\nError: lesson id:" + lessonId + "\nDownload failure " + imageType +
                " part id: " + partId + "\nfile:" + fileUriString + "\nMessage:" + e.getMessage());

        if (nImagesToDownload == nImagesDownloaded) {
            // all tasks finished
            String message = "ALL DOWNLOAD TASKS HAVE FINISHED";
            myLog.addToLog(message);
            messages.add(message);
            sendMessages();
            if (databaseVisibility.equals(USER_DATABASE)) {
                messages.add("REFRESH USER FINISHED WITH ERROR");
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                messages.add("REFRESH GROUP FINISHED WITH ERROR");
            }
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


    static private <T> String serialize(T obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }


}
