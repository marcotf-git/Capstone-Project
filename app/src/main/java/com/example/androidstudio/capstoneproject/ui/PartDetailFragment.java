package com.example.androidstudio.capstoneproject.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.androidstudio.capstoneproject.R;


public class PartDetailFragment extends Fragment {

    private static final String TAG = PartDetailFragment.class.getSimpleName();

    // Final Strings to store state information
    private static final String PART_TEXT = "partText";

    // Variables to store resources that this fragment displays
    private String partText;
    private TextView mTextView;


    // Mandatory constructor for instantiating the fragment
    public PartDetailFragment() {
    }

    /**
     * Inflates the fragment layout and sets any view resources
      */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView");

        if(savedInstanceState != null) {
            partText = savedInstanceState.getString(PART_TEXT);
        }

        // Inflate the fragment view
        View rootView = inflater.inflate(R.layout.fragment_part_detail, container, false);

        mTextView = rootView.findViewById(R.id.tv_part_text);
        mTextView.setText(partText);

        // Return root view
        return rootView;
    }

    public void setPartText(String partText) {

        this.partText = partText;
        mTextView.setText(partText);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(PART_TEXT, partText);
        super.onSaveInstanceState(outState);
    }

}
