package com.example.androidstudio.capstoneproject.sync;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.example.androidstudio.capstoneproject.data.Image;
import com.example.androidstudio.capstoneproject.data.Lesson;
import com.example.androidstudio.capstoneproject.data.LessonPart;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.data.UploadingImage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MyUpload {

    private static final String TAG = MyUpload.class.getSimpleName();

    public static final String ACTION =
            "com.example.androidstudio.capstoneproject.sync.MyUpload";

    private static final String VIDEO = "video";
    private static final String IMAGE = "image";

    private FirebaseFirestore mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;

    private List<String> messages = new ArrayList<>();

    private int nImagesToUpload;
    private int nImagesUploaded;

    private long lesson_id;
    private String userUid;

    private MyLog myLog;

    private Context mContext;


    MyUpload(Context context, String userUid, long lesson_id) {

        mContext = context;
        this.lesson_id = lesson_id;
        this.userUid = userUid;

        mFirebaseDatabase = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mFirebaseDatabase.setFirestoreSettings(settings);
        mFirebaseStorage = FirebaseStorage.getInstance();

        myLog = new MyLog(context);

    }


    // First, upload the images
    // Then, get the uri's obtained from Storage and save in the lessons table
    // Finally, upload the lesson table
    public void uploadImagesAndDatabase() {

        if((userUid == null) || !(lesson_id > 0)) {
            Log.e(TAG, "uploadImagesAndDatabase: failed to get parameters (userUid or lesson_id");
            messages.add("Failed to get parameters (internal failure)");
            sendMessages();
            messages.add("UPLOAD LESSON FINISHED WITH ERROR");
            sendMessages();
            return;
        }


        // First, upload the images
        Log.d(TAG, "uploadImagesAndDatabase lesson+_id:" + lesson_id);

        // Query the parts table with the same lesson_id
        ContentResolver contentResolver = mContext.getContentResolver();

        String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
        String[] selectionArgs = {Long.toString(lesson_id)};
        Cursor partsCursor = contentResolver.query(
                LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        if (partsCursor == null) {
            Log.e(TAG, "uploadImagesAndDatabase: failed to get parts cursor (database error)");
            messages.add("Failed to get parts cursor (database failure)");
            sendMessages();
            messages.add("UPLOAD LESSON FINISHED WITH ERROR");
            sendMessages();
            return;
        }

        long nRows = partsCursor.getCount();

        if (nRows == 0) {
            Log.d(TAG, "uploadImagesAndDatabase: no parts found in local database for the" +
                    " lesson _id:" + lesson_id);
            partsCursor.close();
            messages.add("Error: no parts in this lesson");
            sendMessages();
            messages.add("UPLOAD LESSON FINISHED WITH ERROR");
            sendMessages();
            return;
        }

        // Get all the parts and sore all image uri's in an array of Image instances
        List<Image> images = new ArrayList<>();
        Image image;

        // Moves to the first part of that lesson
        partsCursor.moveToFirst();

        do {

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
        } while (partsCursor.moveToNext());

        partsCursor.close();


        // If there aren't images, go to upload lesson directly
        if (images.size() == 0) {
            Log.d(TAG, "uploadImagesAndDatabase: no images in the database");
            // Go directly to upload the lesson
            uploadLesson(lesson_id);
            myLog.addToLog ("uploadImagesAndDatabase: no images in the database");
            messages.add("UPLOAD LESSON FINISHED OK");
            sendMessages();
            return;
        } else {
            Log.d(TAG, "uploadImagesAndDatabase: uri's of the images stored in the Image array:" + images.toString());
        }

        // counter to control when the process has finished
        nImagesToUpload = images.size();
        nImagesUploaded = 0;

        // Upload the uri's stored in the images array, and after, upload the lesson
        // This has an activity scope, so will unregister when the activity stops
        StorageReference mStorageReference = mFirebaseStorage.getReference().child(userUid);

        final String lessonRef = String.format( Locale.US, "%03d", lesson_id);

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
            StorageReference storageRef = mStorageReference.child(rootDir + "/" + lessonRef +
                    "/" + uri.getLastPathSegment());
            final String filePath = userUid + "/" + rootDir + "/" + lessonRef +
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
                    Log.e(TAG, "uploadImagesAndDatabase takePersistableUriPermission error:" + e.getMessage());
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

            // Call the task  (storage has activity scope to unregister the listeners when activity stops)
            UploadTask uploadTask = storageRef.putFile(uri);

            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d(TAG, "Image part id: "  + imageToUpload.getPart_id() +
                            " upload is " + String.format(Locale.US, "%.2f", progress) + "% done.");
                    myLog.addToLog("Image part id: "  + imageToUpload.getPart_id() +
                            " upload is " + String.format(Locale.US, "%.2f", progress) + "% done.");
                }
            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.d(TAG, "Image part id: "  + imageToUpload.getPart_id() +
                            " upload is paused.");
                    myLog.addToLog("Image part id: "  + imageToUpload.getPart_id() +
                            " upload is paused.");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot state) {

                    nImagesUploaded++;

                    //call helper function to handle the event.
                    handleUploadTaskSuccess(
                            state,
                            imageToUpload.getPart_id(),
                            imageToUpload.getImageType(),
                            filePath,
                            lesson_id);

                    Log.d(TAG, "currentImg:" + currentImg + " finalImg:" + finalImg);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {

                    nImagesUploaded++;

                    // Handle unsuccessful uploads
                    handleUploadTaskFailure(
                            exception,
                            imageToUpload.getPart_id(),
                            imageToUpload.getImageType(),
                            filePath,
                            lesson_id);

                    Log.e(TAG, "Error: currentImg:" + currentImg + " finalImg:" + finalImg, exception);

                }
            });
        }

    }



    // Helper method to upload the lesson text to
    // Firebase Database
    private void uploadLesson(long lesson_id) {

        Log.d(TAG, "uploadLesson lesson_id:" + lesson_id);

        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());
        myLog.addToLog( time_stamp + ":\nNow uploading lesson text...");

        ContentResolver contentResolver = mContext.getContentResolver();

        Uri lessonUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, lesson_id);
        Cursor cursorLesson = contentResolver.query(lessonUri,
                null,
                null,
                null,
                null);

        if (cursorLesson == null) {
            Log.e(TAG, "uploadImagesAndDatabase failed to get cursor");
            messages.add("Failed to get cursor (database failure)");
            sendMessages();
            myLog.addToLog("Failed to get cursor (database failure)");
            return;
        }

        int nRows = cursorLesson.getCount();

        if (nRows > 1) {
            Log.e(TAG, "uploadImagesAndDatabase local database inconsistency nRows:"
                    + nRows + " with the _id:" + lesson_id);
        }

        cursorLesson.moveToFirst();

        // Pass the data cursor to Lesson instance
        // lesson_id is parameter from method
        String user_uid;
        String lesson_title;

        // The database is responsible by the consistency: only one row for lesson _id
        user_uid = userUid;
        lesson_title = cursorLesson.
                getString(cursorLesson.getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE));

        time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());

        // Close the lesson cursor
        cursorLesson.close();

        // Construct a Lesson instance and set with the data from database
        Lesson lesson = new Lesson();

        lesson.setLesson_id(lesson_id);
        lesson.setUser_uid(user_uid);
        lesson.setLesson_title(lesson_title);
        lesson.setTime_stamp(time_stamp);

        String jsonString = serialize(lesson);

        Log.d(TAG, "uploadImagesAndDatabase lesson jsonString:" + jsonString);

        // Load lesson parts from local database into the Lesson instance
        String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
        String[] selectionArgs = {Long.toString(lesson_id)};

        //contentResolver.refresh(LessonsContract.MyLessonPartsEntry.CONTENT_URI, null, null);

        Cursor cursorParts = contentResolver.query(
                LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        // Return if there aren't parts
        if (null == cursorParts) {
            Log.d(TAG, "No parts in this lesson");
            //mCallback.onUploadImageFailure(new Exception("No lesson parts"));
        }

        ArrayList<LessonPart> lessonParts = new ArrayList<>();

        if (null != cursorParts) {
            cursorParts.moveToFirst();

            do {

                Long item_id = cursorParts.getLong(cursorParts.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry._ID));
                // lesson_id is already loaded! (don't need to load, is the lesson_id parameter)
                String lessonPartTitle = cursorParts.getString(cursorParts.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE));
                String lessonPartText = cursorParts.getString(cursorParts.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT));
                String localImageUri = cursorParts.getString(cursorParts.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
                String cloudImageUri = cursorParts.getString(cursorParts.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));
                String localVideoUri = cursorParts.getString(cursorParts.
                        getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));
                String cloudVideoUri = cursorParts.getString(cursorParts.
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

            } while (cursorParts.moveToNext());

            cursorParts.close();
        }


        // set the lesson instance with the values read from the local database
        lesson.setLesson_parts(lessonParts);

        Log.v(TAG, "uploadLesson (to database): lesson title:" + lesson.getLesson_title());

        // Upload the Lesson instance to Firebase Database
        final String documentName = String.format( Locale.US, "%s_%03d",
                lesson.getUser_uid(), lesson.getLesson_id());

        Log.d(TAG, "uploadImagesAndDatabase documentName:" + documentName);

        final String logText = lesson.getLesson_title();
        mFirebaseDatabase.collection("lessons").document(documentName)
                .set(lesson)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written with name:" + documentName);
                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());
                        myLog.addToLog(time_stamp + ":\nLesson " + logText +
                                "\nDocumentSnapshot successfully written with name:" + documentName);

                        Log.d(TAG, "OnSuccessListener onSuccess");
                        myLog.addToLog("ALL UPLOAD TASKS HAVE FINISHED");
                        messages.add("UPLOAD LESSON FINISHED OK");
                        sendMessages();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error writing document on Firebase:", e);
                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());
                        myLog.addToLog(time_stamp + ":\nLesson " + logText +
                                "\nError writing document on Firebase!" +
                                "\nDocument name:" + documentName +"\n" + e.getMessage());

                        Log.e(TAG, "Error:" + e.getMessage());
                        myLog.addToLog("Error:" + e.getMessage());
                        messages.add("Error:" + e.getMessage());
                        sendMessages();
                        messages.add("UPLOAD LESSON FINISHED WITH ERROR");
                        sendMessages();

                    }
                });

    }


    // This handles the upload images task success
    private void handleUploadTaskSuccess(UploadTask.TaskSnapshot state,
                                         long partId,
                                         String imageType,
                                         String fileUriString,
                                         long lessonId) {

        Log.d(TAG, "handleUploadTaskSuccess complete. Uploaded " + imageType + " id:" +
                partId + " of lesson id:" + lessonId);

        if (state.getMetadata() != null) {
            Log.d(TAG, "bucket:" + state.getMetadata().getBucket());
        }

        myLog.addToLog( "upload count:" + nImagesUploaded + "/" + nImagesToUpload);

        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());

        myLog.addToLog( time_stamp + ":\nLesson id:" + lessonId + "\nSuccessfully uploaded " +
                imageType + " part id: " + partId + "\nfile:" + fileUriString);

        // Save the photoUrl in the database
        ContentValues contentValues = new ContentValues();
        // Put the lesson title into the ContentValues

        // test for integrity
        if(imageType == null) {
            messages.add("UPLOAD LESSON FINISHED WITH ERROR");
            sendMessages();
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
        long numberOfImagesUpdated = contentResolver.update(
                updateUri,
                contentValues,
                null,
                null);

        Log.d(TAG, "Updated " + numberOfImagesUpdated + " item(s) in the database");


        if (nImagesToUpload == nImagesUploaded) {
            // all tasks finished
            String message = "IMAGES/VIDEOS UPLOAD TASKS HAVE FINISHED";
            myLog.addToLog(message);
            messages.add(message);
            sendMessages();
            uploadLesson(lesson_id);
        }


    }


    // This handles the upload images task failure
    private void handleUploadTaskFailure(Exception e,
                                         long partId,
                                         String imageType,
                                         String fileUriString,
                                         long lessonId) {

        Log.e(TAG, "handleUploadTaskFailure failure: " + imageType + " id:" +
                partId + " of lesson id:" + lessonId + " fileUriString:" + fileUriString, e);

        myLog.addToLog( "upload count:" + nImagesUploaded + "/" + nImagesToUpload);

        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                .format(new Date());

        myLog.addToLog(time_stamp + ":\nLesson id:" + lessonId + "\nUpload failure " + imageType +
                " part id: " + partId + "\nfile:" + fileUriString + "\nError:" + e.getMessage());

        if (nImagesToUpload == nImagesUploaded) {
            // all tasks finished
            String message = "IMAGES/VIDEOS UPLOAD HAS FINISHED WITH ERROR";
            myLog.addToLog(message);
            messages.add(message);
            sendMessages();
            message = "LESSON TEXT WILL NOT BE UPLOADED!";
            myLog.addToLog(message);
            messages.add(message);
            sendMessages();
            messages.add("UPLOAD LESSON FINISHED WITH ERROR");
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