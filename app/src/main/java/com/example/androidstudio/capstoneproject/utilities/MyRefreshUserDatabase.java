package com.example.androidstudio.capstoneproject.utilities;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.androidstudio.capstoneproject.data.Lesson;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;


public class MyRefreshUserDatabase extends AsyncTask<String, String, String> {

    private static final String TAG = MyRefreshUserDatabase.class.getSimpleName();

    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";

    private OnRefreshUserListener mCallback;
    private FirebaseFirestore mFirebaseDatabase;
    private List <String> messages = new ArrayList<>();


    public interface OnRefreshUserListener {
        void onRefreshUserSuccess(List<String> result);
        void onRefreshUserFailure(Exception exception);
    }


    public MyRefreshUserDatabase(Context context) {

        try {
            mCallback = (OnRefreshUserListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnRefreshUserListener");
        }
    }


    @Override
    protected void onPreExecute() {
        Log.d(TAG, "onPreExecute");

        mFirebaseDatabase = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mFirebaseDatabase.setFirestoreSettings(settings);

        super.onPreExecute();
    }



    protected String doInBackground(String... params) {

        String databaseVisibility = params[0];
        String userUid = params[1];

        refreshDatabase(databaseVisibility, userUid);

        return "OK";
    }



    protected void onProgressUpdate(String... progress) {

        String message = progress[0];

        messages.add(message);

        Log.d(TAG, "onProgressUpdate message:" + message);

    }



    protected void onPostExecute(String result) {

        if (result != null) {
            messages.add(result);
        }

        mCallback.onRefreshUserSuccess(messages);

    }




    // Helper method for refreshing the database from Cloud Firestore
    // Do not delete if existing in user table
    // Delete all in the group table (data and local files)
    private void refreshDatabase(final String databaseVisibility, final String userUid) {

        // Get multiple documents (all the data in the database)
        mFirebaseDatabase.collection("lessons")
                //.whereEqualTo("field_name", true)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            publishProgress("onComplete collection task.isSuccessful");
                            //mCallback.onDownloadDatabaseSuccess();

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
//                                    if (userUid.equals(lesson.getUser_uid())) {
//                                        refreshUserLesson(lesson);
//                                    }
                                }

                            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                                // refresh the lessons of the group table on its separate table
                                // --> pass all the data
//                                refreshGroupLessons(task);
//                                downloadGroupImages(); // this will use the counter
                            }

                        } else {
                            Log.d(TAG, "Error in getting documents: ", task.getException());
                            if (task.getException() != null) {
                                String message = task.getException().getMessage();
                                publishProgress(message);
                            } else {
                                String message = "Error while querying Firebase Database";
                                publishProgress(message);
                            }
                        }
                    }
                });

    }


    static private <T> String serialize(T obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }


}
