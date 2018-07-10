package com.example.androidstudio.capstoneproject.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;


public class LogListAdapter extends RecyclerView.Adapter<LogListAdapter.LogItemViewHolder>{


    private static final String TAG = LogListAdapter.class.getSimpleName();

    // Store the count of items to be displayed in the recycler view
    private static int viewHolderCount;

    // Store the data to be displayed
    private Cursor logCursor;


    /**
     * An on-click handler that we've defined to make it easy for an Activity to interface with
     * our RecyclerView
     */
    final private ListItemClickListener mOnClickListener;

    /**
     * The interface that receives onClick messages and is implemented in MyFirebaseFragment
     * (communicates with the MyFirebaseFragment).
     */
    public interface ListItemClickListener {
        void onListItemClick(View view,
                             int clickedItemIndex,
                             long log_id);

        void onListItemLongClick(View view,
                                 int clickedItemIndex,
                                 long log_id);
    }

    /**
     * Constructor for ListAdapter that accepts a number of items to display and the specification
     * for the ListItemClickListener.
     *
     * @param listener Listener for list item clicks
     */
    public LogListAdapter(ListItemClickListener listener) {
        mOnClickListener = listener;
        viewHolderCount = 0;
    }


    public void setLogCursorData(Cursor cursor){
        this.logCursor = cursor;
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

        Context context = viewGroup.getContext();

        int layoutIdForListItem = R.layout.log_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        final boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        LogItemViewHolder viewHolder = new LogItemViewHolder(view);

        viewHolderCount++;
        Log.d(TAG, "onCreateViewHolder: number of ViewHolders created: " + viewHolderCount);

        return viewHolder;
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

        Log.d(TAG, "#" + position);

        if(!logCursor.moveToPosition(position))
            return;

        String logText = logCursor.getString(logCursor.
                getColumnIndex(LessonsContract.MyLogEntry.COLUMN_LOG_ITEM_TEXT));

        if (logText != null && !logText.equals("")) {
            holder.logTextView.setText(logText);
            holder.logTextView.setVisibility(View.VISIBLE);
        }

        Log.v(TAG, "onBindViewHolder logText:" + logText);

    }


    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our recipes list
     */
    @Override
    public int getItemCount() {

        if (null != logCursor) {
            return logCursor.getCount();
        } else {
            return 0;
        }
    }


    /**
     * Cache of the children views for a list item.
     */
    class LogItemViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        private TextView logTextView;

        final Context context;

        /**
         * Constructor for our ViewHolder. Within this constructor, we get a reference to our
         * TextViews and set an onClickListener to listen for clicks. Those will be handled in the
         * onClick method below.
         * @param itemView The View that you inflated in
         *                 {@link PartsListAdapter#onCreateViewHolder(ViewGroup, int)}
         */
        private LogItemViewHolder(View itemView) {

            super(itemView);

            context = itemView.getContext();

            logTextView = itemView.findViewById(R.id.tv_log);

            // Call setOnClickListener on the View passed into the constructor
            // (use 'this' as the OnClickListener)
            itemView.setOnClickListener(this);

            // Call setOnLongClickListener on the View passed into the constructor
            // (use 'this' as the OnLongClickListener)
            itemView.setOnLongClickListener(this);
        }

        /**
         * Called whenever a user clicks on an item in the list.
         * @param view The View that was clicked
         */
        @Override
        public void onClick(View view) {

            int clickedItemIndex = getAdapterPosition();
            if(!logCursor.moveToPosition(clickedItemIndex))
                return;
            long log_id = logCursor.getLong(logCursor.
                    getColumnIndex(LessonsContract.MyLessonPartsEntry._ID));
            // Calls the method implemented in the main activity
            mOnClickListener.onListItemClick(view, clickedItemIndex, log_id);

        }

        /**
         * Called whenever a user long clicks on an item in the list.
         * @param view The View that was clicked
         */
        @Override
        public boolean onLongClick(View view) {

            int clickedItemIndex = getAdapterPosition();
            if(!logCursor.moveToPosition(clickedItemIndex))
                return true;
            long log_id = logCursor.getLong(logCursor.
                    getColumnIndex(LessonsContract.MyLogEntry._ID));
            // Calls the method implemented in the main activity
            mOnClickListener.onListItemLongClick(view, clickedItemIndex, log_id);

            return true;
        }

    }

}
