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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * This class handle the download of the user lessons or the group lessons, from the Firebase
 * Database (to local database data) and from the Firebase Storage (to local image/video files).
 */
public class MyDownloadService extends IntentService {

    private static final String TAG = MyDownloadService.class.getSimpleName();

    public static final String ACTION =
            "com.example.androidstudio.capstoneproject.sync.MyDownloadService"; // use same action


    private static final String DATABASE_VISIBILITY = "databaseVisibility";
    private static final String USER_UID = "userUid";
    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";
    private static final String VIDEO = "video";
    private static final String IMAGE = "image";

    private static final String IMMEDIATE_DOWNLOAD_SERVICE = "MyDownloadService";
    private static final String SCHEDULED_DOWNLOAD_SERVICE = "ScheduledDownloadService";

    // Automatic unregister listeners
    private FirebaseStorage mFirebaseStorage;
    private FirebaseDatabase mFirebaseDatabase;
    private List<FileDownloadTask> downloadTasks;

    private List<String> messages = new ArrayList<>();

    private int nImagesToDownload;
    private int nImagesDownloaded;

    private MyLog myLog;
    private String callerType;

    private Context mContext;


    // Default constructor
    public MyDownloadService() { super("MyDownloadService"); }

    // This constructor is called by Scheduled Tasks
    public MyDownloadService(Context context) {
        super("MyDownloadService");

        mContext = context;

        myLog = new MyLog(context);
        messages = new ArrayList<>();
    }

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

        String databaseVisibility = null;
        String userUid = null;


        // Recover information from caller activity
        if (intent != null && intent.hasExtra(DATABASE_VISIBILITY)) {
            databaseVisibility = intent.getStringExtra(DATABASE_VISIBILITY);
        }

        if (intent != null && intent.hasExtra(USER_UID)) {
            userUid = intent.getStringExtra(USER_UID);
        }

        if (databaseVisibility != null && userUid != null) {

            try {
                downloadDatabase(userUid, databaseVisibility);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                myLog.addToLog(e.getMessage());
                messages.add(e.getMessage());
                sendMessages();
                messages.add("REFRESH USER FINISHED WITH ERROR");
                sendMessages();
            }
        }

    }


    // Helper method for refreshing the database from Firebase Realtime Database
    // Do not delete if existing in user table
    // Delete all in the group table (data and local files)
    public void downloadDatabase(final String userUid, final String databaseVisibility) {

        // Test the parameters
        if((userUid == null) || (databaseVisibility == null)) {
            Log.e(TAG, "uploadImagesAndDatabase: failed to get parameters " +
                    "(userUid or databaseVisibility");
            messages.add("Failed to get parameters (internal failure)");
            sendMessages();
            messages.add("REFRESH GROUP FINISHED WITH ERROR");
            sendMessages();
            return;
        }

        // Save the origin that is calling this service
        String callerContext = mContext.toString();
        Log.d(TAG, "index of MyDownloadService:" +
                callerContext.indexOf("MyDownloadService"));
        Log.d(TAG, "index of ScheduledDownloadService:" +
                callerContext.indexOf("ScheduledDownloadService"));

        if (callerContext.indexOf(IMMEDIATE_DOWNLOAD_SERVICE) > 0) {
            callerType = IMMEDIATE_DOWNLOAD_SERVICE;
        } else if (callerContext.indexOf(SCHEDULED_DOWNLOAD_SERVICE) > 0) {
            callerType = SCHEDULED_DOWNLOAD_SERVICE;
        }

        Log.d(TAG, "callerType:" + callerType);

        myLog.addToLog("Starting the download of lessons...");

        // Initialize Firebase instances
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        // get a root reference
        DatabaseReference mDatabaseRef = mFirebaseDatabase.getReference();

        // get the user lesson reference
        DatabaseReference mUserLessonRef = mDatabaseRef.child(userUid);

        // get the group lessons reference (all the lessons)
        DatabaseReference mGroupLessonsRef = mDatabaseRef.getRoot();

        Log.d(TAG, "downloadDatabase mGroupLessonsRef:" + mGroupLessonsRef.toString());

        // load only the user lesson
        if (databaseVisibility.equals(USER_DATABASE)) {

            // return the entire list s a single snapshot
            ValueEventListener loadUserLesson = new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // the data exist
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot lessonSnapshot : dataSnapshot.getChildren()) {

                            Log.d(TAG, "lessonSnapshot:" + lessonSnapshot.toString());

                            Lesson lesson = lessonSnapshot.getValue(Lesson.class);
                            if (lesson != null) {
                                Log.d(TAG, "lesson:" + lesson.toString());
                                // with the lesson downloaded, refresh the user lesson tables
                                refreshUserLesson(lesson);
                            }
                        }
                    }
                }


                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "loadUserLesson:onCancelled", databaseError.toException());

                    String message = "Error while querying cloud database for user lessons" +
                            "\nError:" + databaseError.toString();
                    myLog.addToLog(message);

                    informFailure(databaseVisibility);
                }

            };

            // load only the user lesson: read once
            mUserLessonRef.addListenerForSingleValueEvent(loadUserLesson);

        // load all the lessons
        } else if (databaseVisibility.equals(GROUP_DATABASE)) {

            // return the entire list s a single snapshot
            ValueEventListener loadGroupLessons = new ValueEventListener() {

                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    // the data exist
                    if (dataSnapshot.exists()) {

                        List<Lesson> lessons = new ArrayList<>();

                        for (DataSnapshot lessonsSnapshot : dataSnapshot.getChildren()) {

                            for (DataSnapshot lessonSnapshot : lessonsSnapshot.getChildren()) {

                                Log.d(TAG, "lessonSnapshot:" + lessonSnapshot.toString());

                                Lesson lesson = lessonSnapshot.getValue(Lesson.class);
                                if (lesson != null) {
                                    Log.d(TAG, "lesson:" + lesson.toString());
                                }

                                lessons.add(lesson);

                            }

                        }

                        // Now we have the List of all the lessons in the lessons array list

                        // refresh the lessons of the group table on its separate table
                        // write the data to the database table
                        // --> pass all the data

                        refreshGroupLessons(lessons);

                        // download the images and save in local files
                        // write the files uri's in the database table, in the parts table

                        downloadGroupImages(databaseVisibility); // this will use the counter

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.e(TAG, "loadLessons:onCancelled", databaseError.toException());

                    String message = "Error while querying cloud database for group lessons:" +
                            "\nError:" + databaseError.toString();
                    myLog.addToLog(message);

                    informFailure(databaseVisibility);

                }

            };

            // load all the lessons: read once
            mGroupLessonsRef.addListenerForSingleValueEvent(loadGroupLessons);

        }

    }


    // Helper method called by downloadDatabase
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


        // Now, insert the new lesson row in the database
        // Create values to insert
        // Insert with the same id (because it will make the consistency)
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

        // Insert all the parts of the lesson into the database in its table
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

        // Inform the main activity that the job finishes
        messages.add("REFRESH USER FINISHED OK");
        sendMessages();
        if(callerType.equals(SCHEDULED_DOWNLOAD_SERVICE)) {

            Log.d(TAG, "notify the user that the task (synchronized) has finished");
            // notify the user that the task (synchronized) has finished
            NotificationUtils.notifyUserBecauseSyncUserFinished(mContext);
        }

    }


    // Helper method called by downloadDatabase
    // Save the data in the database
    // In case of group lessons, clear the existing table and insert new data
    private void refreshGroupLessons(List<Lesson> lessons) {

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


        // Now delete the lesson parts table (from group tables)
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

        // Insert the new data
        for (Lesson lesson:lessons) {

            Log.d(TAG, "refreshGroupLessons onComplete lesson.getLesson_id():" +
                    lesson.getLesson_id());

            // recover the Lesson instance
            Log.v(TAG, "refreshGroupLessons onComplete lesson:" + lesson.toString());

            // Insert the data in the clean table
            /* Create values to insert */
            ContentValues insertLessonValues = new ContentValues();
            // The _id will be automatically generated for local consistency reasons.
            // The id of the cloud will be saved is in the COLUMN_LESSON_ID instead.
            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_ID, lesson.getLesson_id());
            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TITLE, lesson.getLesson_title());
            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_LESSON_TIME_STAMP, lesson.getTime_stamp());
            insertLessonValues.put(LessonsContract.GroupLessonsEntry.COLUMN_USER_UID, lesson.getUser_uid());

            Uri lessonUri = contentResolver.insert(LessonsContract.GroupLessonsEntry.CONTENT_URI, insertLessonValues);

            // For inserting the parts, extract the _id of the uri!
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

        // Move to the first part of that lesson
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


    // Helper method called by downloadDatabase
    // It will download all the images and save in local files
    // Then, will save the path (local uri's) in the group lesson table
    // The file will be read and shown in the view of the lesson part
    private void downloadGroupImages(final String databaseVisibility) {

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
            myLog.addToLog("Failed to get cursor for group_lesson_parts");
            informFailure(databaseVisibility);
            return;
        }

        int nRows = mCursor.getCount();
        mCursor.moveToFirst();

        // control the number of images to download
        nImagesToDownload = 0;

        // Get all the parts and sore all image cloud uri's in an array of Image instances
        List<Image> images = new ArrayList<>();
        Image image;

        downloadTasks = new ArrayList<>();

        // nRows is the number of lesson parts
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

            // return if there are no parts to download
            myLog.addToLog("No parts to download!");
            myLog.addToLog("Download of Images/Videos has finished.");

            informSuccess(databaseVisibility);

            return;
        }

        if (nImagesToDownload == 0) {

            // return if there isn't images to download
            myLog.addToLog("There aren't images to download!");
            myLog.addToLog("ALL DOWNLOAD TASKS FINISHED");

            informSuccess(databaseVisibility);

            return;
        }

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
                informFailure(databaseVisibility);
                return;
            }

            URI fileUri = file.toURI();
            final String fileUriString = fileUri.toString();

            Log.d(TAG, "downloadGroupImages fileUriString:" + fileUriString);

            // Call the task  (storage has activity scope to unregister the listeners when activity stops)
            FileDownloadTask downloadTask = storageRef.getFile(file);

            // save a reference to the tasks for use when canceling the tasks (when error occurs)
            downloadTasks.add(downloadTask);

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
                            imageToDownload.getLesson_id(),
                            databaseVisibility);

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
                            imageToDownload.getLesson_id(),
                            databaseVisibility);

                    Log.e(TAG, "Error: currentImg:" + currentImg + " finalImg:" +
                            finalImg, exception);

                }
            });

        }
    }


    // Handle the download image task success
    private void handleDownloadTaskSuccess(long partId,
                                           String imageType,
                                           String fileUriString,
                                           long lessonId,
                                           String databaseVisibility) {

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

            // log that all tasks finished
            String message = "ALL DOWNLOAD TASKS HAVE FINISHED";
            myLog.addToLog(message);

            // inform main activity that the task has finished ok
            informSuccess(databaseVisibility);

        }

    }


    // handle the download image task failure
    private void handleDownloadTaskFailure(Exception e,
                                           long partId,
                                           String imageType,
                                           String fileUriString,
                                           long lessonId,
                                           String databaseVisibility) {

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

            // inform to main activity
            informFailure(databaseVisibility);

        } else {

            String message = "Error: download has been stopped!";
            myLog.addToLog(message);

            // cancel all tasks
            if (downloadTasks != null) {
                for (FileDownloadTask task : downloadTasks) {
                    task.cancel();
                }
            }

            // inform to main activity
            informFailure(databaseVisibility);

            // stop the service (don't try to load more images)
            stopSelf();

        }

    }


    // Inform the MainActivity about the end of the service with error
    private void informFailure(String databaseVisibility) {

        // Trigger the snack bar in MainActivity
        if (databaseVisibility.equals(USER_DATABASE)) {
            messages.add("REFRESH USER FINISHED WITH ERROR");
        } else if (databaseVisibility.equals(GROUP_DATABASE)) {
            messages.add("REFRESH GROUP FINISHED WITH ERROR");
        }
        sendMessages();

        // Notify the user in case if Job Scheduled
        if(callerType.equals(SCHEDULED_DOWNLOAD_SERVICE)) {
            if (databaseVisibility.equals(USER_DATABASE)) {
                // notify the user that the task (synchronized) has finished
                NotificationUtils.notifyUserBecauseSyncUserFinished(mContext);
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                // notify the user that the task (synchronized) has finished
                NotificationUtils.notifyUserBecauseSyncGroupFinished(mContext);
            }
        }
    }

    // Inform the MainActivity about the end of the service with success
    private void informSuccess(String databaseVisibility) {

        // Trigger the snack bar in MainActivity
        if (databaseVisibility.equals(USER_DATABASE)) {
            messages.add("REFRESH USER FINISHED OK");
        } else if (databaseVisibility.equals(GROUP_DATABASE)) {
            messages.add("REFRESH GROUP FINISHED OK");
        }
        sendMessages();

        // Notify the user in case if Job Scheduled
        if(callerType.equals(SCHEDULED_DOWNLOAD_SERVICE)) {
            if (databaseVisibility.equals(USER_DATABASE)) {
                // notify the user that the task (synchronized) has finished
                NotificationUtils.notifyUserBecauseSyncUserFinished(mContext);
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                // notify the user that the task (synchronized) has finished
                NotificationUtils.notifyUserBecauseSyncGroupFinished(mContext);
            }
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
