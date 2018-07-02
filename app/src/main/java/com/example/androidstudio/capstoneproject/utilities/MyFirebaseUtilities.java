package com.example.androidstudio.capstoneproject.utilities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MyFirebaseUtilities {

    private FirebaseFirestore cloudFirestore;
    private String userUid;

    private static final String TAG = MyFirebaseUtilities.class.getSimpleName();

    OnCloudListener mCallback;


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
        mCallback = (OnCloudListener) context;
    }





    public void uploadDatabase() {


        Map<String, Object> lesson = new HashMap<>();
        lesson.put("user_id", userUid);
        lesson.put("lesson_id", "11");
        lesson.put("title", "Lesson 1");


//        ArrayList<String> part = new ArrayList<>();
//        part.add("part 1");
//        part.add("part 2");
//        part.add("part 3");

        Map<String, Object> part = new HashMap<>();

        part.put("1", "Part 1");
        part.put("2", "Part 1");
        part.put("3", "Part 1");

        lesson.put("parts", part);

        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        lesson.put("timeStamp", timeStamp);

        Log.v(TAG, "lesson:" + lesson.toString());

        // Add a new document with a generated ID
        cloudFirestore.collection("lessons")
                .add(lesson)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

        String documentName = "cEL2YZFlpadknKTObyT2";
        cloudFirestore.collection("lessons").document(documentName)
                .set(lesson)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });


        cloudFirestore.collection("lessons")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " => " + document.getData());

                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

    }

}
