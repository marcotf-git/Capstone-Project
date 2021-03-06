package com.example.androidstudio.capstoneproject.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;


public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.LogItemViewHolder>{


    /* The context we use to utility methods, app resources and layout inflaters */
    private final Context mContext;

    // Store the data to be displayed
    private Cursor mCursor;


    LogListAdapter(Context context) {
        mContext = context;
    }


    public void swapCursor(Cursor newCursor){
        mCursor = newCursor;
        notifyDataSetChanged();
    }


    /**
     *
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param viewGroup The ViewGroup that these ViewHolders are contained within.
     * @param viewType  If your RecyclerView has more than one type of item (which ours doesn't) you
     *                  can use this viewType integer to provide a different layout. See
     *                  {@link RecyclerView.Adapter#getItemViewType(int)}
     *                  for more details.
     * @return A new NumberViewHolder that holds the View for each list item
     */
    @NonNull
    @Override
    public LogItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {


        View view = LayoutInflater
                .from(mContext)
                .inflate(R.layout.log_list_item, viewGroup, false);

        view.setFocusable(true);

        return new LogItemViewHolder(view);
    }


    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the correct
     * indices in the list for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull final LogItemViewHolder holder, int position) {

        if(!mCursor.moveToPosition(position))
            return;

        String logText = mCursor.getString(mCursor.
                getColumnIndex(LessonsContract.MyLogEntry.COLUMN_LOG_ITEM_TEXT));

        if (logText != null && !logText.equals("")) {
            holder.logTextView.setText(logText);
            holder.logTextView.setVisibility(View.VISIBLE);
        }

        //Log.v(TAG, "onBindViewHolder logText:" + logText);

    }


    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our recipes list
     */
    @Override
    public int getItemCount() {
        if (null == mCursor) return 0;
        return mCursor.getCount();
    }


    /**
     * Cache of the children views for a list item.
     */
    class LogItemViewHolder extends RecyclerView.ViewHolder {

        final TextView logTextView;

        /**
         * Constructor for our ViewHolder. Within this constructor, we get a reference to our
         * TextViews and set an onClickListener to listen for clicks. Those will be handled in the
         * onClick method below.
         * @param itemView The View that you inflated in
         *                 {@link PartsListAdapter#onCreateViewHolder(ViewGroup, int)}
         */
        private LogItemViewHolder(View itemView) {

            super(itemView);

            logTextView = itemView.findViewById(R.id.tv_log);

        }

    }

}
