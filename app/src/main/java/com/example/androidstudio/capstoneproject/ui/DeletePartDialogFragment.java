package com.example.androidstudio.capstoneproject.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import com.example.androidstudio.capstoneproject.R;

public class DeletePartDialogFragment extends DialogFragment {

    private long lesson_part_id;
    private Context mContext;

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DeletePartDialogListener {
        void onDialogPartPositiveClick(DialogFragment dialog, long lesson_part_id);
        void onDialogPartNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    DeletePartDialogListener mListener;

    @Override
    public void onAttach(Context context) {
        mContext = context;
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the listener so we can send events to the host
            mListener = (DeletePartDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(context.toString()
                    + " must implement DeletePartDialogListener");
        }
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        if (null != args) {
            this.lesson_part_id = args.getLong("_id");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        builder.setMessage(R.string.dialog_delete_lesson)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User confirm the dialog
                        mListener.onDialogPartPositiveClick(DeletePartDialogFragment.this, lesson_part_id);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                        mListener.onDialogPartNegativeClick(DeletePartDialogFragment.this);
                    }
                });
        // Create the AlertDialog object and return it
        return builder.create();
    }

}
