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
import java.util.Date;
import java.util.Locale;

import static com.google.common.net.HostSpecifier.isValid;


public class MyFirebaseUtilities {

    private FirebaseFirestore cloudFirestore;
    private String userUid;

    private static final String TAG = MyFirebaseUtilities.class.getSimpleName();

    OnCloudListener mCallback;

    private Context mContext;


    // Listener for sending information to the Activity
    public interface OnCloudListener {
        void onUploadSuccess();
        void onUploadFailure(@NonNull Exception e);
        void onDownloadComplete();
        void onDownloadFailure(@NonNull Exception e);
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
        Uri uploadUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, lesson_id);
        Cursor lessonCursor = contentResolver.query(uploadUri,
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

            lesson = new Lesson();
            lesson.setLesson_id(lesson_id);
            lesson.setUser_uid(user_uid);
            lesson.setLesson_title(lesson_title);
            lesson.setTime_stamp(time_stamp);

            String jsonString = this.serialize(lesson);

            Log.v(TAG, "uploadDatabase lesson jsonString:" + jsonString);


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
    public void refreshDatabase() {

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
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, "refreshDatabase onComplete document.getId():" +
                                        document.getId() + " => " + document.getData());
                                Lesson lesson = document.toObject(Lesson.class);
                                String jsonString = MyFirebaseUtilities.serialize(lesson);
                                Log.v(TAG, "refreshDatabase onComplete lesson jsonString:" + jsonString);
                                MyFirebaseUtilities.refreshLesson(mContext, lesson);
                                mCallback.onDownloadComplete();
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                            mCallback.onUploadFailure(task.getException());
                        }
                    }
                });

    }


    static private void refreshLesson(Context context, Lesson lesson) {

        if (null == lesson) {
            return;
        }

        Log.v(TAG, "refreshLesson lesson_id:" + lesson.getLesson_id());

        // query the local database to see if find the lesson
        // update if found
        // create if didn't exist


        ContentResolver contentResolver = context.getContentResolver();
        Cursor lessonCursor = contentResolver.query(LessonsContract.MyLessonsEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (lessonCursor != null) {
            lessonCursor.moveToFirst();
        }

        int nRows = -1;
        if (lessonCursor != null) {
            nRows = lessonCursor.getCount();
        }

        if (lessonCursor != null) {
            lessonCursor.close();
        }

        // Update the row
        if (nRows >= 0) {

            // open the database for updating

            /* Create values to update */
            ContentValues editLessonValues = new ContentValues();
            editLessonValues.put(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE, lesson.getLesson_title());

            Uri updateUri = ContentUris.withAppendedId(LessonsContract.MyLessonsEntry.CONTENT_URI, lesson.getLesson_id());

            int numberOfLessonsUpdated = contentResolver.update(updateUri,
                    editLessonValues, null, null);

            if (numberOfLessonsUpdated >= 0) {
                Log.v(TAG, "refreshLesson " + numberOfLessonsUpdated +
                        " item(s) updated: lesson_id:" + lesson.getLesson_id());
            }

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


