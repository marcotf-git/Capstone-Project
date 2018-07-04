package com.example.androidstudio.capstoneproject.utilities;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.androidstudio.capstoneproject.data.Lesson;
import com.example.androidstudio.capstoneproject.data.LessonPart;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.google.common.net.HostSpecifier.isValid;


public class MyFirebaseUtilities {


    private static final String TAG = MyFirebaseUtilities.class.getSimpleName();

    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";

    private FirebaseFirestore cloudFirestore;
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
    public MyFirebaseUtilities(Context context, FirebaseFirestore firestoreDatabase, String userUid) {
        this.cloudFirestore = firestoreDatabase;
        this.userUid = userUid;
        mCallback = (OnCloudListener) context;
        mContext = context;
    }

    // Helper method for uploading a specific lesson to Cloud Firestore
    public void uploadDatabase(Long lesson_id) {

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


            // Set with data from lesson parts
            String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
            String[] selectionArgs = {Long.toString(lesson_id)};
            Cursor partsCursor = contentResolver.query(
                    LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    null);

            ArrayList<LessonPart> lessonParts = new ArrayList<LessonPart>();

            Long part_id;
            String part_title;
            LessonPart lessonPart = new LessonPart();

            if (null != partsCursor) {
                partsCursor.moveToFirst();
                int nPartsRows = partsCursor.getCount();
                for (int j = 0; j < nPartsRows; j++) {
                    part_id = partsCursor.
                            getLong(lessonCursor.getColumnIndex(LessonsContract.MyLessonPartsEntry._ID));
                    part_title = lessonCursor.
                            getString(lessonCursor.getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_PART_TITLE));

                    lessonPart.setPart_id(part_id);
                    lessonPart.setLesson_id(lesson_id);
                    lessonPart.setTitle(part_title);

                    lessonParts.add(lessonPart);
                }
            }

            lesson.setLesson_parts(lessonParts);

            Log.v(TAG, "uploadDatabase lesson:" + lesson.toString());

            // Upload the lesson to firestore cloud
            final String documentName = String.format( Locale.US, "%s_%02d",
                    lesson.getUser_uid(), lesson.getLesson_id());

            Log.v(TAG, "uploadDatabase documentName:" + documentName);


            cloudFirestore.collection("lessons").document(documentName)
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


    // Helper method for refreshing the database from Cloud Firestore
    // Do not delete if existing
    public void refreshDatabase(final String databaseVisibility) {

        // Get a document by document name
//        final String documentName = String.format( Locale.US, "%s_%02d",
//                this.userUid, 1);
//        Log.v(TAG, "refreshDatabase documentName:" + documentName);
//        DocumentReference docRef = cloudFirestore.collection("lessons").document(documentName);
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

        cloudFirestore.collection("lessons")
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
        // update if found
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

        // Update the row
        if (nRows > 0) {
            // open the database for updating
            Log.v(TAG, "refreshLesson updating lesson _id:" + lesson.getLesson_id());
            /* Create values to update */
            ContentValues editLessonValues = new ContentValues();
            editLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, lesson.getLesson_title());
            // update
            Uri updateUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, lesson.getLesson_id());

            int numberOfLessonsUpdated = 0;
            if (updateUri != null) {
                numberOfLessonsUpdated = contentResolver.update(updateUri,
                        editLessonValues, null, null);
            }

            if (numberOfLessonsUpdated >= 0) {
                Log.v(TAG, "refreshLesson " + numberOfLessonsUpdated +
                        " item(s) updated: lesson_id:" + lesson.getLesson_id());
            }

        } else {

            // create the row in the local database
            Log.v(TAG, "refreshLesson creating row for lesson _id:" + lesson.getLesson_id());

            /* Create values to insert */
            // insert with the same id (because it will make the consistency)
            ContentValues insertLessonValues = new ContentValues();
            insertLessonValues.put(LessonsContract.MyLessonsEntry._ID, lesson.getLesson_id());
            insertLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, lesson.getLesson_title());

            Uri uri = contentResolver.insert(LessonsContract.MyLessonsEntry.CONTENT_URI, insertLessonValues);

            if (uri != null) {
                Log.v(TAG, "insert uri:" + uri.toString());
            }

            // for inserting the parts, extract the _id of the uri!

        }
    }


    // In case of group lessons, clear the existing table and insert new data
    static private void refreshGroupLessons(Context context, Task<QuerySnapshot> task) {

        Log.v(TAG, "refreshGroupLesson");

        ContentResolver contentResolver = context.getContentResolver();

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

            Uri uri = contentResolver.insert(LessonsContract.GroupLessonsEntry.CONTENT_URI, insertLessonValues);

            // for inserting the parts, extract the _id of the uri!

            if (null != uri) {
                Log.v(TAG, "uri inserted:" + uri);
            }
        }

     }


    public void deleteLessonFromCloud(Long lesson_id) {

        final String documentName = String.format( Locale.US, "%s_%02d",
                userUid, lesson_id);

        Log.v(TAG, "deleteLessonFromCloud documentName:" + documentName);

        cloudFirestore.collection("lessons").document(documentName)
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


