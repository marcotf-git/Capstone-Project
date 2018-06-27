package com.example.androidstudio.capstoneproject.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;



public class LessonsListAdapter extends RecyclerView.Adapter<LessonsListAdapter.RecipeViewHolder>{


    private static final String TAG = LessonsListAdapter.class.getSimpleName();

    // Store the count of items to be displayed in the recycler view
    private static int viewHolderCount;

    // Store the data to be displayed
    private Cursor lessonsCursor;

    /**
     * An on-click handler that we've defined to make it easy for an Activity to interface with
     * our RecyclerView
     */
    final private ListItemClickListener mOnClickListener;

    /**
     * The interface that receives onClick messages and is implemented in MainActivity (communicates
     * with the MainActivity).
     */
    public interface ListItemClickListener {
        void onListItemClick(int clickedItemIndex,
                             int lesson_id,
                             String lessonName);
    }

    /**
     * Constructor for StepsListAdapter that accepts a number of items to display and the specification
     * for the ListItemClickListener.
     *
     * @param listener Listener for list item clicks
     */
    LessonsListAdapter(ListItemClickListener listener) {
        mOnClickListener = listener;
        viewHolderCount = 0;
    }


    void setLessonsCursorData(Cursor cursor){
        Log.v(TAG, "setLessonsCursorData cursor:" + cursor.toString());
        this.lessonsCursor = cursor;
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
     *                  {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
     *                  for more details.
     * @return A new NumberViewHolder that holds the View for each list item
     */
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {

        Context context = viewGroup.getContext();

        int layoutIdForListItem = R.layout.lessons_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutIdForListItem, viewGroup, shouldAttachToParentImmediately);
        RecipeViewHolder viewHolder = new RecipeViewHolder(view);

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
    public void onBindViewHolder(@NonNull final RecipeViewHolder holder, int position) {

        Log.d(TAG, "#" + position);

        if(!lessonsCursor.moveToPosition(position))
            return;

        String lessonName = lessonsCursor.getString(lessonsCursor.
                getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME));

        if (lessonName != null && !lessonName.equals("")) {
            holder.lessonTextView.setText(lessonName);
            holder.lessonTextView.setVisibility(View.VISIBLE);
        }

        Log.v(TAG, "onBindViewHolder lessonName:" + lessonName);

//        Log.v(TAG, "onBindViewHolder imageURL:" + imageURL);
//
//
//        if (imageURL != null && !imageURL.equals("")) {
//            /*
//             * Use the call back of picasso to manage the error in loading poster.
//             * On error, write the message in the text view that is together with the
//             * image view, and make it visible.
//             */
//            Picasso.with(holder.context)
//                    .load(imageURL)
//                    .into(holder.lessonImageView, new Callback() {
//                        @Override
//                        public void onSuccess() {
//                            Log.v(TAG, "Recipe image loaded.");
//                            holder.errorTextView.setVisibility(View.INVISIBLE);
//                        }
//
//                        @Override
//                        public void onError() {
//                            Log.e(TAG, "Error in loading recipe image.");
//                            holder.errorTextView.setVisibility(View.VISIBLE);
//                        }
//                    });
//        } else {
//            holder.errorTextView.setVisibility(View.VISIBLE);
//        }

    }


    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our recipes list
     */
    @Override
    public int getItemCount() {

        if (null != lessonsCursor) {
            return lessonsCursor.getCount();
        } else {
            return 0;
        }
    }


    /**
     * Cache of the children views for a list item.
     */
    class RecipeViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private ImageView lessonImageView;
        private TextView lessonTextView;
        private TextView errorTextView;


        final Context context;

        /**
         * Constructor for our ViewHolder. Within this constructor, we get a reference to our
         * TextViews and set an onClickListener to listen for clicks. Those will be handled in the
         * onClick method below.
         * @param itemView The View that you inflated in
         *                 {@link LessonsListAdapter#onCreateViewHolder(ViewGroup, int)}
         */
        private RecipeViewHolder(View itemView) {

            super(itemView);

            context = itemView.getContext();

            lessonImageView =  itemView.findViewById(R.id.iv_main_lesson_image);
            lessonTextView = itemView.findViewById(R.id.tv_main_lesson_name);
            errorTextView = itemView.findViewById(R.id.tv_lesson_image_error_message_label);


            // Call setOnClickListener on the View passed into the constructor (use 'this' as the OnClickListener)
            itemView.setOnClickListener(this);
        }

        /**
         * Called whenever a user clicks on an item in the list.
         * @param v The View that was clicked
         */
        @Override
        public void onClick(View v) {

            int clickedItemIndex = getAdapterPosition();

            if(!lessonsCursor.moveToPosition(clickedItemIndex))
                return;

            int lesson_id = lessonsCursor.getInt(lessonsCursor.
                    getColumnIndex(LessonsContract.MyLessonsEntry._ID));
            String lessonName = lessonsCursor.getString(lessonsCursor.
                    getColumnIndex(LessonsContract.MyLessonsEntry.COLUMN_LESSON_NAME));

            Log.v(TAG, "onClick recipeName:" + lessonName);

            // Calls the method implemented in the main activity
            mOnClickListener.onListItemClick(
                    clickedItemIndex,
                    lesson_id,
                    lessonName);

        }

    }

}