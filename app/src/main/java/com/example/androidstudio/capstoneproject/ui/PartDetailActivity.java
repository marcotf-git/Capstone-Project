package com.example.androidstudio.capstoneproject.ui;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.net.URI;

import static android.app.PendingIntent.getActivity;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static java.security.AccessController.getContext;


public class PartDetailActivity extends AppCompatActivity implements
            LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = PartDetailActivity.class.getSimpleName();

    private static final String CLICKED_LESSON_PART_ID = "clickedLessonPartId";
    private static final String DATABASE_VISIBILITY = "databaseVisibility";

    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";

    // Loader id
    private static final int ID_LESSON_PARTS_LOADER = 2;
    private static final int ID_GROUP_LESSON_PARTS_LOADER = 20;

    private static final int RC_PHOTO_PICKER =  2;
    private static final int RC_VIDEO_PICKER =  3;

    // state vars
    private static long clickedLessonPart_id;
    private static String databaseVisibility;
    private static int mPlayerViewVisibility;
    private static int imageViewVisibility;
    private static int  errorMessageViewVisibility;

    private String partText;
    private String localVideoUri;
    private String cloudVideoUri;
    private String localImageUri;
    private String cloudImageUri;

    // Menus and buttons
    private Menu mMenu;
    private Toolbar mToolbar;
    private ActionBar actionBar;
    private FloatingActionButton mButton;

    // The views variables
    private View mPlayerView;
    private ImageView imageView;
    private ProgressBar mLoadingIndicator;
    private TextView errorMessageView;

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    private FirebaseStorage mFirebaseStorage;
    private StorageReference mImagesStorageReference;
    private StorageReference mVideosStorageReference;

    private Context mContext;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_part_detail);

        mContext = this;

        mFirebaseStorage = FirebaseStorage.getInstance();
        mImagesStorageReference = mFirebaseStorage.getReference().child("images");
        mVideosStorageReference = mFirebaseStorage.getReference().child("videos");


        // Add the toolbar as the default app bar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        // Get a support ActionBar corresponding to this toolbar
        actionBar = getSupportActionBar();
        if (null != actionBar) {
            // Enable the Up button (icon will be set in onPrepareMenu
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // The views variables
        mPlayerView =  (View) findViewById(R.id.player_container);
        imageView = (ImageView) findViewById(R.id.iv_image);
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);
        errorMessageView = (TextView) findViewById(R.id.tv_illustration_not_available_label);

        // Recover the views state in case of device rotating
        if (savedInstanceState != null) {
            mPlayerView.setVisibility(mPlayerViewVisibility);
            imageView.setVisibility(imageViewVisibility);
            errorMessageView.setVisibility(errorMessageViewVisibility);
        }

        /* Initialize the data vars for this class */
        // Recover information from caller activity
        Intent intentThatStartedThisActivity = getIntent();
        if (intentThatStartedThisActivity.hasExtra(CLICKED_LESSON_PART_ID)) {
            clickedLessonPart_id = intentThatStartedThisActivity.getLongExtra(CLICKED_LESSON_PART_ID, -1);
        }

        if (intentThatStartedThisActivity.hasExtra(DATABASE_VISIBILITY)) {
            databaseVisibility = intentThatStartedThisActivity.getStringExtra(DATABASE_VISIBILITY);
        }

        mLoadingIndicator.setVisibility(View.VISIBLE);

        // Query the database and set the view with the cursor data
        if (databaseVisibility.equals(USER_DATABASE)) {
            this.getSupportLoaderManager().initLoader(ID_LESSON_PARTS_LOADER, null, this);
        } else if (databaseVisibility.equals(GROUP_DATABASE)) {
            this.getSupportLoaderManager().initLoader(ID_GROUP_LESSON_PARTS_LOADER, null, this);
        }

        /* Render the views with the data vars */

        // Create a new StepDetailFragment instance and display it using the FragmentManager
        // only create new fragment when there is no previously saved state
//        if (savedInstanceState == null) {
//            loadViews();
//        }

        builder = new AlertDialog.Builder(PartDetailActivity.this);
        // Add the buttons
        builder.setTitle(R.string.pick_image_video)
                .setItems(R.array.illustrations_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        Log.v(TAG, "onClick which:" + which);
                        switch (which) {
                                case 0:
                                    addImage();
                                    break;
                                case 1:
                                    addVideo();
                                    break;
                                default:
                                    break;
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        dialog = builder.create();

        /*
         Set the Floating Action Button (FAB) to its corresponding View.
         Attach an OnClickListener to it, so that when it's clicked, a new intent will be created
         to pick photo or video.
         */
        mButton = findViewById(R.id.fab_add);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "fab onClick clickedLessonPart_id:" + clickedLessonPart_id);
                dialog.show();
            }
        });

    }


    private void updateView(Cursor cursor) {

        Log.v(TAG, "updateView");

        // Set initial state of the player and thumbnail views (this method is only called in two pane)
        errorMessageView.setVisibility(View.GONE);
        mPlayerView.setVisibility(View.GONE);
        imageView.setVisibility(View.GONE);

        // Remove previously loaded fragments
        FragmentManager myFragmentManager = getSupportFragmentManager();
        Fragment fragment = myFragmentManager.findFragmentById(R.id.part_detail_container);
        if (null != fragment) {
            myFragmentManager.beginTransaction().remove(fragment).commit();
        }
        fragment = myFragmentManager.findFragmentById(R.id.player_container);
        if (null != fragment) {
            myFragmentManager.beginTransaction().remove(fragment).commit();
        }


        // Create a new PartDetailFragment instance
        PartDetailFragment partDetailFragment = new PartDetailFragment();

        partText = null;
        localImageUri = null;

        if (null != cursor) {

            Log.v(TAG, "updateView cursor.getCount():" + cursor.getCount());

            cursor.moveToLast();

            // load the vars data to display
            if (databaseVisibility.equals(USER_DATABASE)) {

                if (!cursor.isNull(cursor.getColumnIndex(
                        LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT))) {
                    partText = cursor.getString(cursor.getColumnIndex(
                            LessonsContract.MyLessonPartsEntry.COLUMN_PART_TEXT));
                }
                if (!cursor.isNull(cursor.getColumnIndex(
                        LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI))) {
                    localImageUri = cursor.getString(cursor.getColumnIndex(
                            LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
                }
                if (!cursor.isNull(cursor.getColumnIndex(
                        LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI))) {
                    localVideoUri = cursor.getString(cursor.getColumnIndex(
                            LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));
                }

            } else if (databaseVisibility.equals(GROUP_DATABASE)) {

                if (!cursor.isNull(cursor.getColumnIndex(
                        LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TEXT))) {
                    partText = cursor.getString(cursor.getColumnIndex(
                            LessonsContract.GroupLessonPartsEntry.COLUMN_PART_TEXT));
                }
                if (!cursor.isNull(cursor.getColumnIndex(
                        LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI))) {
                    localImageUri = cursor.getString(cursor.getColumnIndex(
                            LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
                }
                if (!cursor.isNull(cursor.getColumnIndex(
                        LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI))) {
                    localVideoUri = cursor.getString(cursor.getColumnIndex(
                            LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));
                }
            }

        }

        // Send the data to the fragment data
        if (partText != null) {
            partDetailFragment.setPartText(partText);
        } else {
            partDetailFragment.setPartText("No text available.");
        }

        // Use a FragmentManager and transaction to add the fragment to the screen
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(R.id.part_detail_container, partDetailFragment)
                .commit();

        // Create a new ExoPlayerFragment instance and display it using FragmentManager
        // or try to load and show the Thumbnail
        if (null != localVideoUri && !localVideoUri.equals("")) {

            Uri uri = Uri.parse(localVideoUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    final int takeFlags = intent.getFlags()
                            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                } catch (Exception e) {

                }
            }

            ExoPlayerFragment exoPlayerFragment = new ExoPlayerFragment();
            // Set the fragment data
            exoPlayerFragment.setMediaUrl(localVideoUri);
            // Use a FragmentManager and transaction to add the fragment to the screen
            FragmentManager playerFragmentManager = getSupportFragmentManager();
            playerFragmentManager.beginTransaction()
                    .add(R.id.player_container, exoPlayerFragment)
                    .commit();
            mPlayerView.setVisibility(View.VISIBLE);

        } else {

            // Try to load the image
            if (null != localImageUri && !localImageUri.equals("")) {

                Log.v(TAG, "updateView loading image localImageUri:" + localImageUri);
                /*
                 * Use the call back of picasso to manage the error in loading thumbnail.
                 */

                Uri uri = Uri.parse(localImageUri);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        final int takeFlags = intent.getFlags()
                                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (Exception e) {

                    }
                }

                Picasso.get()
                        .load(localImageUri)
                        .into(imageView, new Callback() {
                            @Override
                            public void onSuccess() {
                                Log.v(TAG, "Image loaded");
                                imageView.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error in loading image:" + e.getMessage());
                                Toast.makeText(mContext, "Error in loading image:" + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                                imageView.setVisibility(View.GONE);
                                if(mPlayerView.getVisibility() == View.GONE) {
                                    errorMessageView.setVisibility(View.VISIBLE);
                                }
                            }

                        });


            } else {
                errorMessageView.setVisibility(View.VISIBLE);
            }

        }

    }


    /**
     * Called by the {@link android.support.v4.app.LoaderManagerImpl} when a new Loader needs to be
     * created. This Activity only uses one loader, so we don't necessarily NEED to check the
     * loaderId, but this is certainly best practice.
     *
     * @param loaderId The loader ID for which we need to create a loader
     * @param bundle   Any arguments supplied by the caller
     * @return A new Loader instance that is ready to start loading.
     */
    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {

        Log.d(TAG, "onCreateLoader loaderId:" + loaderId);

        Uri partUri;

        switch (loaderId) {

            case ID_LESSON_PARTS_LOADER:

                /* URI for one lesson part by its _id in "my_lesson_parts" table */
                partUri = ContentUris.withAppendedId(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                        clickedLessonPart_id);

                Log.v(TAG, "onCreateLoader partUri:" + partUri.toString());
                Log.v(TAG, "onCreateLoader clickedLessonPart_id:" + clickedLessonPart_id);

                return new CursorLoader(mContext,
                        partUri,
                        null,
                        null,
                        null,
                        null);

            case ID_GROUP_LESSON_PARTS_LOADER:

                /* URI for one lesson part by its _id in "my_lesson_parts" table */
                partUri = ContentUris.withAppendedId(LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                        clickedLessonPart_id);

                Log.v(TAG, "onCreateLoader partUri:" + partUri.toString());
                Log.v(TAG, "onCreateLoader clickedLessonPart_id:" + clickedLessonPart_id);

                return new CursorLoader(mContext,
                        partUri,
                        null,
                        null,
                        null,
                        null);

            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);
        }
    }

    /**
     * Called when a Loader has finished loading its data.
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        Log.v(TAG, "onLoadFinished cursor:" + data.toString());
        // Pass the data to the view
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        updateView(data);
    }

    /**
     * Called when a previously created loader is being reset, and thus making its data unavailable.
     * The application should at this point remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        /*
         * Since this Loader's data is now invalid, we need to clear the view that is
         * displaying the data.
         */
        updateView(null);
    }

    // Inflate the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Save a reference to the menu
        mMenu = menu;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String modeOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        if (modeOption.equals(this.getString(R.string.pref_mode_view))) {
            menu.findItem(R.id.select_view).setChecked(true);
        }

        if (modeOption.equals(this.getString(R.string.pref_mode_create))) {
            menu.findItem(R.id.select_create).setChecked(true);
        }

         // Prepare the visibility of the action items
        contextualizeMenu();

        return true;

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemThatWasClickedId = item.getItemId();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        switch (itemThatWasClickedId) {

            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected");
                break;

            case R.id.select_view:
                mMenu.findItem(R.id.select_view).setChecked(true);
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_view)).apply();
                // Set visibility of action icons
                contextualizeMenu();
                Log.d(TAG, "View mode selected");
                break;

            case R.id.select_create:
                mMenu.findItem(R.id.select_create).setChecked(true);
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_create)).apply();
                // Set visibility of action icons
                contextualizeMenu();
                Log.v(TAG, "Create mode selected");
                break;

            case R.id.action_edit:
                Log.d(TAG, "Deletion action selected");
                editLessonPartText(clickedLessonPart_id);
                 break;

            case R.id.action_cancel:
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    // Set the visibility of the options according to the app state
    private void contextualizeMenu() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String modeOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        Log.v(TAG,"contextualizeMenu modeOption:" + modeOption + " databaseVisibility:" +
                databaseVisibility);

        mMenu.findItem(R.id.action_delete).setVisible(false);

        if (databaseVisibility.equals(USER_DATABASE) &&
                modeOption.equals(this.getString(R.string.pref_mode_create))) {
            mMenu.findItem(R.id.action_edit).setVisible(true);
            mButton.setVisibility(VISIBLE);
        } else {
            mMenu.findItem(R.id.action_edit).setVisible(false);
            mButton.setVisibility(GONE);
        }

        mMenu.findItem(R.id.action_delete_from_cloud).setVisible(false);
        mMenu.findItem(R.id.action_upload).setVisible(false);
        mMenu.findItem(R.id.action_refresh).setVisible(false);
        mMenu.findItem(R.id.action_insert_fake_data).setVisible(false);

        if (databaseVisibility.equals(GROUP_DATABASE)) {
            mMenu.findItem(R.id.select_view).setVisible(false);
            mMenu.findItem(R.id.select_create).setVisible(false);
            mMenu.findItem(R.id.action_cancel).setVisible(false);
        } else if (databaseVisibility.equals(USER_DATABASE)) {
            mMenu.findItem(R.id.select_view).setVisible(true);
            mMenu.findItem(R.id.select_create).setVisible(true);
            mMenu.findItem(R.id.action_cancel).setVisible(true);
        }

    }


    // Helper function to edit lesson part
    private void editLessonPartText(long _id) {
        Log.d(TAG, "editLessonPart _id:" + _id);
        // Create a new intent to start an
        Class destinationActivity = EditPartTextActivity.class;
        Intent editPartTextIntent = new Intent(mContext, destinationActivity);
        editPartTextIntent.putExtra(CLICKED_LESSON_PART_ID, _id);
        startActivity(editPartTextIntent);
    }


    private void addImage() {
//        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
//        intent.setType("image/jpeg");
//        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
//        startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);

        Intent intent;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }else{
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, getResources()
                .getString(R.string.form_pick_image)), RC_PHOTO_PICKER);
    }


    private void addVideo() {
        Intent intent;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        }else{
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        }
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("video/*");
        startActivityForResult(Intent.createChooser(intent, getResources()
                .getString(R.string.form_pick_video)), RC_VIDEO_PICKER);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.v(TAG, "resultCode=" + resultCode);
        Log.v(TAG, "data=" + String.valueOf(data));

        final Uri uri = data != null ? data.getData() : null;
        if (uri != null) {
            Log.v(TAG, "isDocumentUri=" + DocumentsContract.isDocumentUri(this, uri));
        } else {
            Log.e(TAG, "missing URI?");
            return;
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
                ContentResolver resolver = mContext.getContentResolver();
                resolver.takePersistableUriPermission(uri, takeFlags);
            } catch (SecurityException e) {
                Log.e(TAG, "FAILED TO TAKE PERMISSION", e);
            }
        }

        if (resultCode == RESULT_OK) {
            if (requestCode == RC_PHOTO_PICKER) {
                Toast.makeText(this, "Picked an image!", Toast.LENGTH_LONG).show();
                insertBlobUriInDatabase(data, "image");
            } else if (requestCode == RC_VIDEO_PICKER) {
                Toast.makeText(this, "Picked a video!", Toast.LENGTH_LONG).show();
                insertBlobUriInDatabase(data, "video");
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Canceled!", Toast.LENGTH_LONG).show();
        }

        super.onActivityResult(requestCode, resultCode, data);

    }


    private void insertBlobUriInDatabase(Intent data, String type) {

        Uri selectedBlobUri = data.getData();

        Log.v(TAG, "insertImageUriInDatabase selectedBlobUri:" + selectedBlobUri.toString());

        // Create new empty ContentValues object
        ContentValues contentValues = new ContentValues();

        if (type.equals("image")) {
            contentValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI,
                    selectedBlobUri.toString());
        } else if (type.equals("video")) {
            contentValues.put(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI,
                    selectedBlobUri.toString());
        }

        Uri updateUri = ContentUris.withAppendedId(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                clickedLessonPart_id);

        int numberOfImagesUpdated = getContentResolver().update(updateUri,
                contentValues, null, null);

        if (numberOfImagesUpdated > 0) {
            Log.d(TAG, "Local Uri of added " + type + ":" + selectedBlobUri.toString());
        } else {
            Log.e(TAG, "Error on saving uri to local database");
            Toast.makeText(this,
                    "Error on saving uri to local database", Toast.LENGTH_LONG).show();
        }

    }


    // This is for saving the step that is being viewed when the device is rotated
    @Override
    public void onSaveInstanceState(Bundle outState) {

        mPlayerViewVisibility =  mPlayerView.getVisibility();
        imageViewVisibility = imageView.getVisibility();
        errorMessageViewVisibility = errorMessageView.getVisibility();

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
