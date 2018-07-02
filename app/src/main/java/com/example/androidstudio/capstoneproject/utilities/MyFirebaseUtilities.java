package com.example.androidstudio.capstoneproject.utilities;

import android.content.ContentResolver;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.google.common.net.HostSpecifier.isValid;


public class MyFirebaseUtilities {

    private FirebaseFirestore cloudFirestore;
    private String userUid;

    private static final String TAG = MyFirebaseUtilities.class.getSimpleName();

    OnCloudListener mCallback;

    private Context mContext;


    // Listener for sending information to the Activity
    public interface OnCloudListener {
        public void onUploadSuccess(DocumentReference documentReference);
        public void onUploadFailure(@NonNull Exception e);
        public void onDownloadComplete(@NonNull Task<QuerySnapshot> task);
    }

    // Constructor
    public MyFirebaseUtilities(Context context, FirebaseFirestore firestoreDatabase, String userUid) {
        this.cloudFirestore = firestoreDatabase;
        this.userUid = userUid;
        //mCallback = (OnCloudListener) context;
        mContext = context;
    }

    // Helper method for uploading the database to Cloud Firestore
    public void uploadDatabase() {

        ContentResolver contentResolver = mContext.getContentResolver();
        Cursor lessonCursor = contentResolver.query(LessonsContract.MyLessonsEntry.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (lessonCursor != null) {
            lessonCursor.moveToFirst();
        }

        int nRows = lessonCursor != null ? lessonCursor.getCount() : 0;

        // Pass the data cursor to Lesson instance
        long lesson_id;
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
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
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

        Lesson lesson = new Lesson();

        lesson.setUser_uid(this.userUid);
        lesson.setLesson_id(1);
        final String documentName = String.format( Locale.US, "%s_%02d",
                lesson.getUser_uid(), lesson.getLesson_id());

        Log.v(TAG, "refreshDatabase documentName:" + documentName);

        // Get a document
        DocumentReference docRef = cloudFirestore.collection("cities").document(documentName);
        docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Lesson lesson = documentSnapshot.toObject(Lesson.class);
            }
        });


//        ContentResolver contentResolver = mContext.getContentResolver();
//        Cursor lessonCursor = contentResolver.query(LessonsContract.MyLessonsEntry.CONTENT_URI,
//                null,
//                null,
//                null,
//                null);
//
//        if (lessonCursor != null) {
//            lessonCursor.moveToFirst();
//        }
//
//        int nRows = lessonCursor != null ? lessonCursor.getCount() : 0;
//
//        // Pass the data cursor to Lesson instance
//        long lesson_id;
//        String user_uid;
//        String lesson_title;
//        String time_stamp;
//
//        Lesson lesson;
//
//        for (int i = 0; i < nRows; i++) {
//
//            lesson_id = lessonCursor.
//                    getLong(lessonCursor.getColumnIndex(LessonsContract.MyLessonsEntry._ID));
//            user_uid = userUid;
//            lesson_title = lessonCursor.
//                    getString(lessonCursor.getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_TITLE));
//
//            time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
//                    .format(new Date());
//
//            lesson = new Lesson();
//            lesson.setLesson_id(lesson_id);
//            lesson.setUser_uid(user_uid);
//            lesson.setLesson_title(lesson_title);
//            lesson.setTime_stamp(time_stamp);
//
//            String jsonString = this.serialize(lesson);
//
//            Log.v(TAG, "uploadDatabase lesson jsonString:" + jsonString);
//
//
//            final String documentName = String.format( Locale.US, "%s_%02d",
//                    lesson.getUser_uid(), lesson.getLesson_id());
//
//            Log.v(TAG, "uploadDatabase documentName:" + documentName);
//
//
//            cloudFirestore.collection("lessons").document(documentName)
//                    .set(lesson)
//                    .addOnSuccessListener(new OnSuccessListener<Void>() {
//                        @Override
//                        public void onSuccess(Void aVoid) {
//                            Log.d(TAG, "DocumentSnapshot successfully written with name:" + documentName);
//                        }
//                    })
//                    .addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            Log.w(TAG, "Error writing document", e);
//                        }
//                    });
//
//            if (!lessonCursor.moveToNext()){
//                break;
//            }
//
//        }
//
//
//        if (lessonCursor != null) {
//            lessonCursor.close();
//        }

    }


    /**
     * Serialize string.
     *
     * @param <T> Type of the object passed
     * @param obj Object to serialize
     * @return Serialized string
     */
    private <T> String serialize(T obj) {
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
    private <T> T deSerialize(String jsonString, Class<T> tClass) throws ClassNotFoundException {
        if (!isValid(jsonString)) {
            return null;
        }
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonString, tClass);
    }

}


