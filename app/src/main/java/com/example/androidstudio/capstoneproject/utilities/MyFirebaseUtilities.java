package com.example.androidstudio.capstoneproject.utilities;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.example.androidstudio.capstoneproject.data.Lesson;
import com.example.androidstudio.capstoneproject.data.LessonPart;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.google.common.net.HostSpecifier.isValid;


public class MyFirebaseUtilities {


    private static final String TAG = MyFirebaseUtilities.class.getSimpleName();

    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";

    private FirebaseFirestore mFirebaseDatabase;
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mImagesStorageReference;
    private StorageReference mVideosStorageReference;

    private String userUid;

    private Context mContext;

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
    public MyFirebaseUtilities(Context context, FirebaseFirestore firestoreDatabase,
                               FirebaseStorage firebaseStorage, String userUid) {

        this.mFirebaseDatabase = firestoreDatabase;
        this.mFirebaseStorage = firebaseStorage;
        this.userUid = userUid;
        mCallback = (OnCloudListener) context;
        mContext = context;
    }

    // Helper method for uploading a specific lesson to Cloud Firestore
    public void uploadDatabase(Long lesson_id) {

        mImagesStorageReference = mFirebaseStorage.getReference().child("images");
        mVideosStorageReference = mFirebaseStorage.getReference().child("videos");

        ContentResolver contentResolver = mContext.getContentResolver();
        Uri lessonUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, lesson_id);
        Cursor lessonCursor = contentResolver.query(lessonUri,
                null,
                null,
                null,
                null);

        if (lessonCursor != null) {
            lessonCursor.moveToFirst();
        }

        int nRows = lessonCursor != null ? lessonCursor.getCount() : 0;

        // Pass the data cursor to Lesson instance
        String user_uid;
        String lesson_title;
        String time_stamp;

        Lesson lesson;

        for (int i = 0; i < nRows; i++) {

            lesson_id = lessonCursor.
                    getLong(lessonCursor.getColumnIndex(LessonsContract.MyLessonsEntry._ID));
            user_uid = userUid;
            lesson_title = lessonCursor.
                    getString(lessonCursor.getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE));

            time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                    .format(new Date());

            // Construct a Lesson instance and set with the data from database
            lesson = new Lesson();
            lesson.setLesson_id(lesson_id);
            lesson.setUser_uid(user_uid);
            lesson.setLesson_title(lesson_title);
            lesson.setTime_stamp(time_stamp);

            String jsonString = this.serialize(lesson);

            Log.v(TAG, "uploadDatabase lesson jsonString:" + jsonString);


            // Load lesson parts into the lesson
            String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
            String[] selectionArgs = {Long.toString(lesson_id)};
            Cursor partsCursor = contentResolver.query(
                    LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    null);

            ArrayList<LessonPart> lessonParts = new ArrayList<LessonPart>();

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

                    String cloudImageUri = uploadImageToFirestore(localImageUri);

//                    String cloudImageUri = partsCursor.getString(partsCursor.
//                            getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));

//                    String localVideoUri = partsCursor.getString(partsCursor.
//                            getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));

                    //String cloudVideoUri = uploadVideoToFirestore(localVideoUri);

//                    String cloudVideoUri = partsCursor.getString(partsCursor.
//                            getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI));

                    LessonPart lessonPart = new LessonPart();
                    lessonPart.setPart_id(item_id);
                    lessonPart.setLesson_id(lesson_id);
                    lessonPart.setTitle(lessonPartTitle);
                    lessonPart.setText(lessonPartText);

                    //lessonPart.setLocal_image_uri(localImageUri);

                    lessonPart.setCloud_image_uri(cloudImageUri);

                    //lessonPart.setLocal_video_uri(localVideoUri);

                    //lessonPart.setCloud_video_uri(cloudVideoUri);

                    Log.v(TAG, "lessonPart:" + lessonPart.toString());

                    lessonParts.add(lessonPart);
                    partsCursor.moveToNext();
                }
            }

            lesson.setLesson_parts(lessonParts);

            Log.v(TAG, "uploadDatabase lesson:" + lesson.toString());

            // Upload the lesson to firestore cloud
            final String documentName = String.format( Locale.US, "%s_%02d",
                    lesson.getUser_uid(), lesson.getLesson_id());

            Log.v(TAG, "uploadDatabase documentName:" + documentName);


            mFirebaseDatabase.collection("lessons").document(documentName)
                    .set(lesson)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written with name:" + documentName);
                        mCallback.onUploadSuccess();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                        mCallback.onUploadFailure(e);
                    }
                });

            if (!lessonCursor.moveToNext()){
                break;
            }

        }


        if (lessonCursor != null) {
            lessonCursor.close();
        }

    }


    // upload to Firestore and return the uri
    private String uploadImageToFirestore(String localUri) {

        Uri.Builder builder = new Uri.Builder();

        Uri uri = builder.appendPath(localUri).build();

        // Get a reference to store file at chat_photos/<FILENAME>
        // Example: content://local_images/foo/4 -> the file name that we're saving our stored
        // image at would be 4 because it's the last part of the path segment.
        final StorageReference imageRef =
                mImagesStorageReference.child(uri.getLastPathSegment());

        // Upload file to Firebase Storage
        UploadTask uploadTask = imageRef.putFile(uri);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return imageRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {

                    Uri downloadUri = task.getResult();

                    Log.d(TAG, "Cloud Uri of added image:" + downloadUri);

                } else {
                    // Handle failures
                    // ...
                    if (task.getException()!= null) {

                        Log.e(TAG, task.getException().toString());

                        Toast.makeText(mContext, "Error in uploading to cloud! " +
                                task.getException().toString(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


        Uri cloudUri = urlTask.getResult();

        Log.v(TAG, "uploadImageToFirestore cloudUri:" + cloudUri);

        return cloudUri.toString();


    }


//    private String uploadVideoToFirestore(String localUri) {
//
//
//
//    }




    // Helper method for refreshing the database from Cloud Firestore
    // Do not delete if existing
    public void refreshDatabase(final String databaseVisibility) {

        // Get a document by document name
//        final String documentName = String.format( Locale.US, "%s_%02d",
//                this.userUid, 1);
//        Log.v(TAG, "refreshDatabase documentName:" + documentName);
//        DocumentReference docRef = mFirebaseDatabase.collection("lessons").document(documentName);
//        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
//            @Override
//            public void onSuccess(DocumentSnapshot documentSnapshot) {
//                Lesson lesson = documentSnapshot.toObject(Lesson.class);
//                String jsonString = MyFirebaseUtilities.serialize(lesson);
//                Log.v(TAG, "refreshDatabase onSuccess lesson jsonString:" + jsonString);
//                MyFirebaseUtilities.refreshLesson(mContext, lesson);
//            }
//        });


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
                                    String jsonString = MyFirebaseUtilities.serialize(lesson);
                                    Log.v(TAG, "refreshDatabase onComplete lesson jsonString:" + jsonString);

                                    // refresh the lessons of the local user on its separate table
                                    // this gives consistency to the database
                                    if (userUid.equals(lesson.getUser_uid())) {
                                        MyFirebaseUtilities.refreshUserLesson(mContext, lesson);
                                    }
                                }

                            } else if (databaseVisibility.equals(GROUP_DATABASE)) {

                                // refresh the lessons of the group table on its separate table
                                MyFirebaseUtilities.refreshGroupLessons(mContext, task);
                            }

                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                            mCallback.onDownloadFailure(task.getException());
                        }
                    }
                });

    }


    static private void refreshUserLesson(Context context, Lesson lesson) {

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

            Long inserted_lesson_id = null;

            if (lessonUri != null) {
                Log.v(TAG, "insert uri:" + lessonUri.toString());
                // for inserting the parts, extract the _id of the uri!
                inserted_lesson_id = Long.parseLong(lessonUri.getPathSegments().get(1));
            }

            // insert all the parts of the lesson
            if (null != inserted_lesson_id) {
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

                        Uri partUri = contentResolver.insert(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                                insertLessonPartValues);

                        Log.v(TAG, "refreshUserLesson partUri inserted:" + partUri);
                    }
                }
            }
        }
    }


    // In case of group lessons, clear the existing table and insert new data
    static private void refreshGroupLessons(Context context, Task<QuerySnapshot> task) {

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
            String jsonString = MyFirebaseUtilities.serialize(lesson);
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


