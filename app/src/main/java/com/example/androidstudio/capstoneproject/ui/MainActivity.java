package com.example.androidstudio.capstoneproject.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
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
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidstudio.capstoneproject.IdlingResource.SimpleIdlingResource;
import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.sync.MyDownloadService;
import com.example.androidstudio.capstoneproject.sync.SyncUtilities;
import com.example.androidstudio.capstoneproject.utilities.MyFirebaseFragment;
import com.example.androidstudio.capstoneproject.utilities.InsertTestDataUtil;
import com.example.androidstudio.capstoneproject.utilities.MyReceiver;
import com.example.androidstudio.capstoneproject.utilities.MyRefreshUserDatabase;
import com.example.androidstudio.capstoneproject.utilities.NotificationUtils;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.Arrays;
import java.util.List;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.View.inflate;


/**
 * The app has two modes: 'view' mode and 'create' mode.
 * If the user select 'create' mode, it will show only the content created by the user,
 * plus the options to create, edit and delete, in the action items of the app bar.
 * If the user shows 'view' mode, it will show the content that is synced with an remote server.
 * This class will start showing the lessons' titles queried from a local database, which is a copy
 * of the remote database. This corresponds to the 'view' mode, which can be selected in the
 * overflow menu.
 *
 * According to the modes of the app, the local database has two types of tables.
 * The table "group_ ... content" is a copy of the remote database.
 * The table "my_ ... content" is the content created by the user.
 *
 * The database and its tables are handled by a content provider LessonsContentProvider.
 * The provider is queried by a cursor loader, which returns a cursor object.
 *
 * In the 'view' mode, the view layout will be the activity_main.xml, which has fragments containers.
 * The fragment_main will have a RecyclerView,
 * populated with a GridLayoutManager and a custom adapter LessonsListAdapter.
 * The cursor provided by the loader is passed to the adapter with the data that will be shown.
 *
 * In the 'view' mode, if the user shows the option to sync the database, it will call a job task,
 * or an async task. There will have two options. The async task will be the immediate sync.
 *
 * In the 'create' mode, if the user selects the option to add a lesson title, it will open another
 * activity AddLessonActivity to add the title. If the user select to delete and then click on an
 * item, the item will be deleted after a confirmation. The confirmation is handled by a fragment.
 * If the user select to edit and then click, it will open an activity EditLessonActivity to edit
 * that item.
 *
 * The app has two main kinds of local tables: the USER tables and the GROUP tables. The user tables
 * are for user data, and creation of new content. It is possible to use all of the resources from
 * the device, like camera and video, selecting images from the device storage, using the device
 * touch screen keyboard and edition resources to write text, etc.
 *
 * Then, the user creation can be uploaded to the cloud, and downloaded (in the GROUP table) by any
 * person that have the same key (same root/project) in the Firebase. There are specific rules in
 * the app and in Firebase protecting the data of one user from another.
 *
 * The user only can upload or download data from the cloud when logged. The login process is
 * handled by the Firebase Authentication.
 *
 * The cloud data is divided between the Firebase Database Cloud Firestore (text), and the Firebase
 * Storage (blob). The local data is divided between a table with the lesson data (text), and a
 * table with the lesson parts data (text), and the local folders for file (blob) storage.
 * The table with the lesson parts will store the uri's of the images or videos of that part.
 *
 * The upload of data has the following algorithm:
 * 1) first, upload the images or videos to Storage
 * 2) get the uri's info and save in the local database, in the table of the lesson parts
 * 3) then, upload the two tables (lesson and parts) to the Firebase Database
 *
 * To download data from cloud, the app has the algorithm:
 * 1) first, download the tables from Firebase Database
 * 2) get the uri's and with them, download the files from Storage
 * 3) write the file in the local folder, in the internal storage
 * 4) save the path and filename (the local uri) in the table of the parts
 * 5) when the user clicks on the part, the ExoPlayer or the Picasso will get that file uri,
 * and load the file form local folder.
 * 6) before the downloading, the table is deleted from local database, and the files are deleted
 * from local folder (/data/user/0/appUri/files/...)
 *
 * To delete a user lesson (a complex task!), the app has the algorithm:
 * a) it is possible to delete or locally, or in the cloud
 * 1) only delete if there are no parts
 * 2) when deleting the part, store the reference of the file, that will be used to delete the file
 * in the cloud, when the user choose to do it
 * 3) when the user choose to delete the lesson from the cloud, it won't be deleted locally
 * 4) when the user choose to delete in the cloud, it will also delete the files in the Storage:
 * 4.1) delete from cloud database and save the uri's of the files in a specific local table, that
 * will store the files for future deletion
 * 4.2) query that table, and with the information, delete from the Storage
 *
 * To edit the local data, the user can edit only their table my_...
 * 1) choose what to edit and edit (select choose the option in the menu)
 * 2) choose to pick another image
 * 2.1) the old image, if has cloud uri, will be deleted but its reference will be saved in that
 * specific table, for future deletion from Storage
 * 2.2) the new one (or the new video) take the place
 *
 * So, the app implements CRUD on local user data, and in user cloud data, managing the deletion
 * from Storage for when deleting the whole lesson from Database Firestore.
 *
 * And the app implements only QUERY from the cloud, in case of group data (inside the group will be
 * also the user same lessons, but saved in the group table.
 *
 * The app menu is contextual: the options change according to the user actions.
 *
 * Finally, the cloud communication is saved in a log file, that can be viewed by an option in the
 * drawer menu.
 *
 * So, we have Content Provider, Async Tasks, Firebase, Firebase JobDispatcher, Widget, thousands of
 * listeners... :-) and a complex task of handling with two databases in the cloud!
 * All trying to make an app that handle the creation of a small lesson by the user, with some parts,
 * with images and videos, storing and sharing in the cloud.
 *
 */


public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        MainFragment.OnLessonListener,
        MainFragment.OnIdlingResourceListener,
        PartsFragment.OnLessonPartListener,
        PartsFragment.OnIdlingResourceListener,
        DeleteLessonLocallyDialogFragment.DeleteLessonDialogListener,
        DeletePartDialogFragment.DeletePartDialogListener,
        MyFirebaseFragment.OnCloudListener,
        DeleteLessonOnCloudDialogFragment.DeleteLessonCloudDialogListener,
        MyRefreshUserDatabase.OnRefreshUserListener{


    private static final String TAG = MainActivity.class.getSimpleName();

    // Firebase auth: choose an arbitrary request code value
    private static final int RC_SIGN_IN = 1;

    // Final string to store state information
    private static final String CLICKED_LESSON_ID = "clickedLessonId";
    private static final String SELECTED_LESSON_ID = "selectedLessonId";
    private static final String CLICKED_LESSON_PART_ID = "clickedLessonPartId";
    private static final String SELECTED_LESSON_PART_ID = "selectedLessonPartId";
    private static final String MAIN_VISIBILITY = "mainVisibility";
    private static final String PARTS_VISIBILITY = "partsVisibility";
    private static final String LOG_VISIBILITY = "logVisibility";
    private static final String DATABASE_VISIBILITY = "databaseVisibility";
    private static final String LOCAL_USER_UID = "localUserUid";
    private static final String LOADING_INDICATOR = "loadingIndicator";
    private static final String UPLOAD_COUNT = "uploadCount";
    private static final String UPLOAD_COUNT_FINAL = "uploadCountFinal";
    private static final String DOWNLOAD_COUNT = "downloadCount";
    private static final String DOWNLOAD_COUNT_FINAL = "downloadCountFinal";
    private static final String RELEASE = "release";

    // Final strings
    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";
    public static final String ANONYMOUS = "anonymous";

    // App state information variables
    private long clickedLesson_id;
    private long selectedLesson_id;
    private long clickedLessonPart_id;
    private long selectedLessonPart_id;
    private int mainVisibility;
    private int partsVisibility;
    private int logVisibility;
    private static String databaseVisibility;
    private String mUserUid; // The user's ID, unique to the Firebase project.
    private boolean loadingIndicator;
    private int uploadCount;
    private int uploadCountFinal;
    private int downloadCount;
    private int downloadCountFinal;
    private int totalImagesToDelete;
    private int imagesToDeleteCount;

    // User data variables
    private String mUsername;
    private String mUserEmail;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    // Menus and buttons
    private Menu mMenu;
    private ActionBar actionBar;
    private FloatingActionButton mButton;

    // Drawer menu variables
    private DrawerLayout mDrawerLayout;

    // Views
    private FrameLayout lessonsContainer;
    private FrameLayout partsContainer;
    private FrameLayout logContainer;
    private TextView mUsernameTextView;
    private TextView mUserEmailTextView;

    // Dialogs
    private AlertDialog uploadDialog;
    private AlertDialog refreshJobDialog;
    private AlertDialog uploadJobDialog;

    // Fragments
    private MainFragment mainFragment;
    private PartsFragment partsFragment;
    static private MyFirebaseFragment firebaseFragment;
    private LogFragment logFragment;

    // Context
    private Context mContext;

    // Receiver
    public MyReceiver myReceiver;



    // The Idling Resource which will be null in production.
    @Nullable
    private SimpleIdlingResource mIdlingResource;


    /**
     * Only called from test, creates and returns a new {@link SimpleIdlingResource}.
     */
//    @VisibleForTesting
//    public void getIdlingResource() {
//        if (mIdlingResource == null) {
//            mIdlingResource = new SimpleIdlingResource();
//        }
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // recovering the instance state
        if (savedInstanceState != null) {
            clickedLesson_id = savedInstanceState.getLong(CLICKED_LESSON_ID);
            selectedLesson_id = savedInstanceState.getLong(SELECTED_LESSON_ID);
            clickedLessonPart_id = savedInstanceState.getLong(CLICKED_LESSON_PART_ID);
            selectedLessonPart_id = savedInstanceState.getLong(SELECTED_LESSON_PART_ID);
            mainVisibility = savedInstanceState.getInt(MAIN_VISIBILITY);
            partsVisibility = savedInstanceState.getInt(PARTS_VISIBILITY);
            logVisibility =  savedInstanceState.getInt(LOG_VISIBILITY);
            databaseVisibility = savedInstanceState.getString(DATABASE_VISIBILITY);
            mUserUid = savedInstanceState.getString(LOCAL_USER_UID);
            loadingIndicator = savedInstanceState.getBoolean(LOADING_INDICATOR);
            uploadCount = savedInstanceState.getInt(UPLOAD_COUNT);
            uploadCountFinal = savedInstanceState.getInt(UPLOAD_COUNT_FINAL);
            downloadCount = savedInstanceState.getInt(DOWNLOAD_COUNT);
            downloadCountFinal = savedInstanceState.getInt(DOWNLOAD_COUNT_FINAL);

        } else {
            // Initialize the state vars
            clickedLesson_id = -1;
            selectedLesson_id = -1;
            clickedLessonPart_id = -1;
            selectedLessonPart_id = -1;
            // Phone visibility
            mainVisibility = VISIBLE;
            partsVisibility = GONE;
            logVisibility = GONE;
            // Recover the local user uid for handling the database global consistency
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            mUserUid = sharedPreferences.getString(LOCAL_USER_UID,"");
            databaseVisibility = sharedPreferences.getString(DATABASE_VISIBILITY, USER_DATABASE);
            loadingIndicator = false;
        }

        // Init the main view
        setContentView(R.layout.activity_main);

        // Init another member variables
        mContext = this;
        mUsername = ANONYMOUS;
        mUserEmail = "";

        // Add the toolbar as the default app bar
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        // Get a support ActionBar corresponding to this toolbar
        actionBar = getSupportActionBar();
        if (null != actionBar) {
            // Enable the Up button (icon will be set in onPrepareMenu)
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        /*
         Set the Floating Action Button (FAB) to its corresponding View.
         Attach an OnClickListener to it, so that when it's clicked, a new intent will be created
         to launch the AddLessonActivity.
         */
        mButton = findViewById(R.id.fab_add);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick clickedLesson_id:" + clickedLesson_id);
                // Try to action first on the more specific item
                if (partsContainer.getVisibility() == GONE) {
                    // Create a new intent to start an AddLessonActivity
                    Intent addLessonIntent = new Intent(MainActivity.this, AddLessonActivity.class);
                    startActivity(addLessonIntent);
                } else if (clickedLesson_id >= 0) {
                    // Create a new intent to start an AddLessonPartActivity
                    Intent addLessonPartIntent = new Intent(MainActivity.this, AddLessonPartActivity.class);
                    addLessonPartIntent.putExtra(CLICKED_LESSON_ID, clickedLesson_id);
                    startActivity(addLessonPartIntent);
                } else {
                    // Create a new intent to start an AddLessonActivity
                    Intent addLessonIntent = new Intent(MainActivity.this, AddLessonActivity.class);
                    startActivity(addLessonIntent);
                }
                // Clear selections on fragments
                mainFragment.deselectViews();
                partsFragment.deselectViews();
            }
        });

        // Initialize the containers for fragments
        lessonsContainer = findViewById(R.id.lessons_container);
        partsContainer = findViewById(R.id.parts_container);
        logContainer = findViewById(R.id.log_container);

        // Initialize the fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        // Only create fragment when needed
        if (null == savedInstanceState) {

            Log.d(TAG, "creating MainFragment");
            mainFragment = new MainFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.lessons_container, mainFragment, "MainFragment")
                    .commit();

            Log.d(TAG, "creating PartsFragment");
            partsFragment = new PartsFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.parts_container, partsFragment, "PartsFragment")
                    .commit();

            Log.d(TAG, "creating firebaseFragment");
            firebaseFragment = new MyFirebaseFragment();
            fragmentManager.beginTransaction()
                    .add(firebaseFragment, "MyFirebaseFragment")
                    .commit();

        } else {
            mainFragment = (MainFragment) fragmentManager.findFragmentByTag("MainFragment");
            partsFragment = (PartsFragment) fragmentManager.findFragmentByTag("PartsFragment");
            firebaseFragment = (MyFirebaseFragment) fragmentManager.findFragmentByTag("MyFirebaseFragment");

            if (logVisibility == VISIBLE) {
                logFragment = (LogFragment) fragmentManager.findFragmentByTag("LogFragment");
            }
        }

        // Set fragments database visibility
        mainFragment.setDatabaseVisibility(databaseVisibility);
        partsFragment.setDatabaseVisibility(databaseVisibility);

        // Set the fragment views visibility
        lessonsContainer.setVisibility(mainVisibility);
        partsContainer.setVisibility(partsVisibility);
        logContainer.setVisibility(logVisibility);

        mainFragment.setLoadingIndicator(loadingIndicator);

        Log.d(TAG, "lessonsContainer visibility:" + lessonsContainer.getVisibility());
        Log.d(TAG, "partsContainer visibility:" + partsContainer.getVisibility());
        Log.d(TAG, "logContainer visibility:" + logContainer.getVisibility());

        // Get the IdlingResource instance for testing
        //getIdlingResource();

        /*
         * The IdlingResource is null in production as set by the @Nullable annotation which means
         * the value is allowed to be null.
         *
         * If the idle state is true, Espresso can perform the next action.
         * If the idle state is false, Espresso will wait until it is true before
         * performing the next action.
         */
        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(false);
        }

        // Initialize Firebase components
        FirebaseFirestore mFirebaseDatabase = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mFirebaseDatabase.setFirestoreSettings(settings);

        // Initialize the FirebaseAuth instance and the AuthStateListener method so
        // we can track whenever the user signs in or out.
        mFirebaseAuth = FirebaseAuth.getInstance();
        // Initialize Firebase Auth listener
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null){
                    // user is signed in
                    onSignedInInitialize(user.getDisplayName(), user.getEmail(), user.getUid());
                } else {
                    // user is signed out
                    if (!mUsername.equals(ANONYMOUS)) {
                        Toast.makeText(MainActivity.this, "Signed out!",
                                Toast.LENGTH_SHORT).show();
                    }
                    onSignedOutCleanup();
                }
            }
        };

        // Set the drawer menu
        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                    SharedPreferences sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences(mContext);

                    // close drawer when item is tapped
                    mDrawerLayout.closeDrawers();
                    int itemThatWasClickedId = menuItem.getItemId();
                    // Update the UI based on the item selected
                    switch (itemThatWasClickedId) {

                        case R.id.action_login:
                            // Firebase UI login
                            login();
                            break;

                        case R.id.action_logout:
                            logout();
                            break;

                        case R.id.nav_my_lessons:
                            databaseVisibility = USER_DATABASE;
                            sharedPreferences.edit()
                                    .putString(DATABASE_VISIBILITY,databaseVisibility).apply();
                            // Set fragments database visibility
                            mainFragment.setDatabaseVisibility(databaseVisibility);
                            partsFragment.setDatabaseVisibility(databaseVisibility);
                            break;

                        case R.id.nav_group_lessons:
                            databaseVisibility = GROUP_DATABASE;
                            sharedPreferences.edit()
                                    .putString(DATABASE_VISIBILITY,databaseVisibility).apply();
                            // Set fragments database visibility
                            mainFragment.setDatabaseVisibility(databaseVisibility);
                            partsFragment.setDatabaseVisibility(databaseVisibility);
                            break;

                        case R.id.nav_log:
                            viewLog();
                            break;

                        default:
                            break;
                    }
                    contextualizeMenu();
                    return true;
                }
            });

        // Set the username int the drawer
        View headerView = navigationView.getHeaderView(0);
        mUsernameTextView = headerView.findViewById(R.id.tv_user_name);
        mUserEmailTextView = headerView.findViewById(R.id.tv_user_email);
        mUsernameTextView.setText(mUsername);
        mUserEmailTextView.setText(mUserEmail);

        // Set the drawer database view state
        Menu menuDrawer = navigationView.getMenu();
        menuDrawer.findItem(R.id.nav_my_lessons).setChecked(databaseVisibility.equals(USER_DATABASE));
        menuDrawer.findItem(R.id.nav_group_lessons).setChecked(databaseVisibility.equals(GROUP_DATABASE));

        // Init a uploadDialog menu for user to choose the type of data to upload
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Add the buttons
        builder.setTitle(R.string.upload_type)
                .setItems(R.array.uploading_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        Log.v(TAG, "onClick which:" + which);
                        switch (which) {
                            case 0:
                                uploadImagesAndDatabase();
                                break;
                            case 1:
                                uploadLesson();
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

        uploadDialog = builder.create();

        // Dialog for the refresh job service (that will run in background)
        builder.setTitle(R.string.refresh_job_type)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                                "The app will start syncing in background!\n" +
                                        "Please, wait a few minutes for the job...\n" +
                                        "You will receive a notification when it finishes!",
                                Snackbar.LENGTH_INDEFINITE);
                        snackBar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                snackBar.dismiss();
                            }
                        });
                        snackBar.show();
                        // call the job for sync the group table
                        SyncUtilities.scheduleSyncDatabase(mContext, databaseVisibility);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        refreshJobDialog = builder.create();


        // Dialog for the upload job service (that will run in background)
        builder.setTitle(R.string.upload_job_type)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (selectedLesson_id == -1) {
                            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                                    "Please, select a lesson to upload the images or videos!",
                                    Snackbar.LENGTH_INDEFINITE);
                            snackBar.setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    snackBar.dismiss();
                                }
                            });
                            snackBar.show();
                        } else {
                            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                                    "The app will start uploading in background!\n" +
                                    "Please, wait a few minutes for the job...\n" +
                                    "You will receive a notification when it finishes!",
                                    Snackbar.LENGTH_INDEFINITE);
                            snackBar.setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    snackBar.dismiss();
                                }
                            });
                            snackBar.show();
                            // call the job for upload the selected user lesson (images and text)
                            SyncUtilities.scheduleUploadTable(mContext, selectedLesson_id);
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        uploadJobDialog = builder.create();


        //setupServiceReceiver();

    }


    // Helper method to show the Log view (Log fragment)
    private void viewLog() {
        mainVisibility = GONE;
        lessonsContainer.setVisibility(mainVisibility);
        partsVisibility = GONE;
        partsContainer.setVisibility(partsVisibility);
        logVisibility = VISIBLE;
        logContainer.setVisibility(logVisibility);

        Log.d(TAG, "creating LogFragment");
        logFragment = new LogFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.log_container, logFragment, "LogFragment")
                .commit();

        actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);

    }


    // Helper method for Firebase login
    private void login() {
        Log.d(TAG, "login");
        FirebaseUser user = mFirebaseAuth.getCurrentUser();
        if (user != null){
            // user is signed in
            Toast.makeText(MainActivity.this, "You're already signed in!",
                    Toast.LENGTH_SHORT).show();
        } else {
            // user is signed out
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            // Do not automatically save user credentials
                            .setIsSmartLockEnabled(false)
                            .setAvailableProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.EmailBuilder().build(),
                                    new AuthUI.IdpConfig.GoogleBuilder().build()))
                            .build(),
                    RC_SIGN_IN);
        }
    }

    // Method for sign in for the listener
    private void onSignedInInitialize(String username, String userEmail, String userUid) {

        mUsername = username;
        mUserEmail = userEmail;
        mUserUid = userUid;

        Log.d(TAG, "userUid:" + userUid);

        // save the user uid locally
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString(LOCAL_USER_UID, mUserUid).apply();

        // Set the drawer
        mUsernameTextView.setText(mUsername);
        mUserEmailTextView.setText(mUserEmail);

        // Set the visibility of the upload action icon
        if (null != mMenu) {
            contextualizeMenu();
        }

        // Set the firebaseFragment
        //firebaseFragment.setFirebase(mFirebaseDatabase, mFirebaseStorage, mUserUid);
        firebaseFragment.setFirebase(mUserUid);

        Log.d(TAG, "onSignedInInitialize mUsername:" + mUsername);
    }

    // Handle cancelled sign in
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled!", Toast.LENGTH_SHORT).show();
             }
        }
    }

    // Helper method for Firebase logout
    private void logout () {
        Log.d(TAG, "logout");
        if (mUsername.equals(ANONYMOUS)) {
            Snackbar.make(lessonsContainer, "You are not signed!",
                    Snackbar.LENGTH_LONG).show();
        }
        AuthUI.getInstance().signOut(this);
    }

    // Method for sign out for the listener
    private void onSignedOutCleanup(){
        Log.d(TAG, "onSignedOutCleanup");
        mUsername = ANONYMOUS;
        mUserEmail = "";
        mUsernameTextView.setText(mUsername);
        mUserEmailTextView.setText(mUserEmail);
        // Set the visibility of the upload action icon
        if (null != mMenu) {
            contextualizeMenu();
        }
    }


    /**
     * The following methods handle menu creation and setting the menu according to the app state
     */
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

        // Set the drawer menu icon according to views
        if (mainVisibility == VISIBLE) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }

        // Prepare the visibility of the action items
        contextualizeMenu();

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemThatWasClickedId = item.getItemId();

        switch (itemThatWasClickedId) {

            case android.R.id.home:
                optionHome();
                return true;

            case R.id.select_view:
                optionSelectView();
                break;

            case R.id.select_create:
                optionSelectCreate();
                break;

            case R.id.action_edit:
                optionEdit();
                break;

            case R.id.action_job_sync:
                refreshJobDialog.show();
                break;

            case R.id.action_job_upload:
                uploadJobDialog.show();
                break;

            case R.id.action_delete:
                optionDelete();
                break;

            case R.id.action_delete_from_cloud:
                optionDeleteFromCloud();
                break;

            case R.id.action_refresh:
                optionRefresh();
                break;

            case R.id.action_upload:
                uploadDialog.show();
                break;

            case R.id.action_notification:
                //NotificationUtils.notifyUserBecauseSyncGroupFinished(this);

                testUtil();

                break;

            case R.id.action_insert_fake_data:
                optionInsertFakeData();
                break;

            case R.id.action_cancel:
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
    * Helper functions for the menu
    */
    private void optionHome() {
        Log.d(TAG, "onOptionsItemSelected mainVisibility:" + mainVisibility);
        // Set the action according to the views visibility
        if (mainVisibility == VISIBLE) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        } else if (mainVisibility == GONE && partsVisibility == VISIBLE){
            closePartsFragment();
        } else if (logVisibility == VISIBLE) {
            logVisibility = GONE;
            logContainer.setVisibility(logVisibility);

            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .remove(logFragment)
                    .commit();

            mainVisibility = VISIBLE;
            lessonsContainer.setVisibility(mainVisibility);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
            contextualizeMenu();
        }
    }

    private void optionSelectView() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mMenu.findItem(R.id.select_view).setChecked(true);
        sharedPreferences.edit()
                .putString(this.getString(R.string.pref_mode_key),
                        this.getString(R.string.pref_mode_view)).apply();
        // Set visibility of action icons
        contextualizeMenu();
        // Deselect the last view selected
        mainFragment.deselectViews();
        partsFragment.deselectViews();
        selectedLesson_id = -1;
        selectedLessonPart_id = -1;
        Log.d(TAG, "View mode selected");

    }

    private void optionSelectCreate() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mMenu.findItem(R.id.select_create).setChecked(true);
        sharedPreferences.edit()
                .putString(this.getString(R.string.pref_mode_key),
                        this.getString(R.string.pref_mode_create)).apply();
        // Set visibility of action icons
        contextualizeMenu();
        Log.v(TAG, "Create mode selected");
    }

    private void optionEdit() {
        Log.d(TAG, "Deletion action selected");
        // Try to action first on the more specific item
        if (selectedLessonPart_id != -1) {
            editLessonPart(selectedLessonPart_id);
        } else if (selectedLesson_id != -1) {
            editLesson(selectedLesson_id);
        } else {
            Toast.makeText(this,
                    "Please, select an item to delete!", Toast.LENGTH_LONG).show();
        }
    }

    private void optionDelete() {
        Log.d(TAG, "Deletion action selected");
        // Try to action first on the more specific item
        if (selectedLessonPart_id != -1) {
            deleteLessonPart(selectedLessonPart_id);
        } else if (selectedLesson_id != -1) {
            deleteLesson(selectedLesson_id);
        } else {
            Toast.makeText(this,
                    "Please, select an item to delete!", Toast.LENGTH_LONG).show();
        }
    }

    private void optionDeleteFromCloud() {
        Log.d(TAG, "Delete from Cloud action selected");
        // Try to action first on the more specific item
        if  (selectedLesson_id != -1) {
            deleteLessonFromCloud(selectedLesson_id);
        } else {
            Toast.makeText(this,
                    "Please, select a lesson to delete from Cloud!", Toast.LENGTH_LONG).show();
        }
    }

    private void optionInsertFakeData() {
        Log.d(TAG, "Insert fake data action selected");
        InsertTestDataUtil.insertFakeData(this);
        Toast.makeText(this,
                "Fake data inserted!", Toast.LENGTH_LONG).show();
    }

    /**
     * Contextualize the menu.
     * Set the visibility of the options according to the app state.
     */
    private void contextualizeMenu() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String modeOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        Log.d(TAG,"contextualizeMenu modeOption:" + modeOption + " databaseVisibility:" +
                databaseVisibility + " mUsername:" + mUsername);

        if (modeOption.equals(this.getString(R.string.pref_mode_create))) {
            mMenu.findItem(R.id.action_delete).setVisible(true);
        } else {
            mMenu.findItem(R.id.action_delete).setVisible(false);
        }

        if (databaseVisibility.equals(USER_DATABASE) &&
                modeOption.equals(this.getString(R.string.pref_mode_create))) {
            mMenu.findItem(R.id.action_insert_fake_data).setVisible(true);
            mMenu.findItem(R.id.action_edit).setVisible(true);
            mButton.setVisibility(VISIBLE);
        } else {
            mMenu.findItem(R.id.action_insert_fake_data).setVisible(false);
            mMenu.findItem(R.id.action_edit).setVisible(false);
            mButton.setVisibility(GONE);
        }

        if (databaseVisibility.equals(USER_DATABASE) &&
                modeOption.equals(this.getString(R.string.pref_mode_create)) &&
                !mUsername.equals(ANONYMOUS)) {
            mMenu.findItem(R.id.action_delete_from_cloud).setVisible(true);
            mMenu.findItem(R.id.action_upload).setVisible(true);
            mMenu.findItem(R.id.action_job_upload).setVisible(true);
        } else {
            mMenu.findItem(R.id.action_delete_from_cloud).setVisible(false);
            mMenu.findItem(R.id.action_upload).setVisible(false);
            mMenu.findItem(R.id.action_job_upload).setVisible(false);
        }

        if (!mUsername.equals(ANONYMOUS)) {
            mMenu.findItem(R.id.action_refresh).setVisible(true);
            mMenu.findItem(R.id.action_job_sync).setVisible(true);
        } else {
            mMenu.findItem(R.id.action_refresh).setVisible(false);
            mMenu.findItem(R.id.action_job_sync).setVisible(false);
        }

        if (databaseVisibility.equals(GROUP_DATABASE)) {
            mMenu.findItem(R.id.select_view).setVisible(false);
            mMenu.findItem(R.id.select_create).setVisible(false);
            mMenu.findItem(R.id.action_cancel).setVisible(true);
        } else if (databaseVisibility.equals(USER_DATABASE)) {
            mMenu.findItem(R.id.select_view).setVisible(true);
            mMenu.findItem(R.id.select_create).setVisible(true);
            mMenu.findItem(R.id.action_cancel).setVisible(true);
        }

        // Set the drawer menu icon according to views
        if (mainVisibility == VISIBLE) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }

        if(logVisibility == VISIBLE) {
            mMenu.findItem(R.id.select_view).setVisible(false);
            mMenu.findItem(R.id.select_create).setVisible(false);
            mMenu.findItem(R.id.action_refresh).setVisible(false);
            mMenu.findItem(R.id.action_job_sync).setVisible(false);
            mMenu.findItem(R.id.action_edit).setVisible(false);
            mMenu.findItem(R.id.action_upload).setVisible(false);
            mMenu.findItem(R.id.action_job_upload).setVisible(false);
            mMenu.findItem(R.id.action_delete).setVisible(false);
            mMenu.findItem(R.id.action_delete_from_cloud).setVisible(false);
            mMenu.findItem(R.id.action_insert_fake_data).setVisible(false);
            mMenu.findItem(R.id.action_cancel).setVisible(false);
            mButton.setVisibility(GONE);
        }

    }


    /**
     * Helper methods for closing fragments.
     */
    // Helper method for hiding the PartsFragment
    public void closePartsFragment() {
        Log.d(TAG, "closePartsFragment");
        // deselect the views on the fragment that will be closed
        partsFragment.deselectViews();
        // clear the reference var
        selectedLessonPart_id = -1;
        // Change the views
        partsVisibility = GONE;
        partsContainer.setVisibility(partsVisibility);
        mainVisibility = VISIBLE;
        lessonsContainer.setVisibility(mainVisibility);
        // Set the drawer menu icon according to views
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
    }

    private void deselectViews() {
        // Deselect the last view selected
        mainFragment.deselectViews();
        selectedLesson_id = -1;
        partsFragment.deselectViews();
        selectedLessonPart_id = -1;
    }


    /**
     * Methods for refreshing/uploading the database.
     */

    // This function will refresh all the database, of the user and the group.
    // It will download the photos from Firebase Storage, but only for the group table (the user table
    // already has the images in the folder).
    // In case of the group, it will also delete the old files, and the old rows in the table.
    private void optionRefresh() {

        downloadCountFinal = 1;

        // This will count the actual downloads
        downloadCount = 0;

        loadingIndicator = true;
        mainFragment.setLoadingIndicator(true);

        // Calls the refresh method in another thread
        AsyncTask mRefreshBackgroundTask = new RefreshTask(databaseVisibility);
        mRefreshBackgroundTask.execute();

        deselectViews();

    }




    // AsyncTask to process the refresh of the database in background
    private static class RefreshTask extends AsyncTask<Object, Void, Void> {

        private String databaseVisibility;

        RefreshTask(String databaseVisibility) {
            super();
            this.databaseVisibility = databaseVisibility;
        }

        @Override
        protected Void doInBackground(Object... object) {
            firebaseFragment.refreshDatabase(databaseVisibility);
            return null;
        }

    }


    // It will upload the images and videos, and after, upload the database
    private void uploadImagesAndDatabase() {

        // Verify if there is a lesson selected
        if (selectedLesson_id == -1) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Please, select a lesson to upload the images or videos!\n" +
                    "Sorry,there isn't an option to upload just one part!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();
            return;
        }

        // query the parts table to count for the number of images to upload
        /* Perform the ContentProvider query for the lesson parts */
        String selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + " =? ";
        String[] selectionArgs = {Long.toString(selectedLesson_id)};
        Cursor cursor = this.getContentResolver().query(
                LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        uploadCountFinal = 0;

        // Count the images
        if (cursor != null) {
            cursor.moveToFirst();

            do {
                long part_id = cursor.getLong(cursor.getColumnIndex(LessonsContract.MyLessonPartsEntry._ID));
                String local_video_uri = cursor.
                        getString(cursor.getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));
                String local_image_uri = cursor.
                        getString(cursor.getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
                Log.d(TAG, "part_id: " + part_id);
                Log.d(TAG, "local_video_uri: " + local_video_uri);
                Log.d(TAG, "local_image_uri: " + local_image_uri);
                if (local_image_uri != null || local_video_uri != null ) {
                    // Total number of images/videos to upload

                    // SET THE INITIAL STATE
                    uploadCountFinal++;
                }

            } while (cursor.moveToNext());

            Log.d(TAG, "cursor: getCount:" + cursor.getCount());
        }

        if (cursor != null) {
            cursor.close();
        }

        // SET THE INITIAL STATE
        uploadCount = 0;

        Log.d(TAG, "uploadCountFinal:" + uploadCountFinal + " images to upload");
        firebaseFragment.addToLog("STARTING UPLOAD IMAGES/VIDEOS: " +
                uploadCountFinal + " files to upload.");

        loadingIndicator = true;
        mainFragment.setLoadingIndicator(true);

        // After uploading images, this method (in the call back) will trigger the upload of the
        // database
        firebaseFragment.uploadImagesAndDatabase(selectedLesson_id);

    }


    private void uploadLesson() {

        if (selectedLesson_id == -1) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Please, select a lesson to upload the text content!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();
            return;
        }


        Log.d(TAG, "uploadLesson: uploadCountFinal:" + uploadCountFinal);

        firebaseFragment.addToLog("STARTING UPLOAD TEXT: " + uploadCountFinal + " files to upload.");

        loadingIndicator = true;
        mainFragment.setLoadingIndicator(true);

        AsyncTask mUploadBackgroundTask = new UploadTask(selectedLesson_id);
        mUploadBackgroundTask.execute();

    }

    // AsyncTask to process the upload of the lesson in background
    private static class UploadTask extends AsyncTask<Object, Void, Void> {

        private long selectedLesson_id;

        UploadTask(long selectedLesson_id) {
            super();
            this.selectedLesson_id = selectedLesson_id;
        }

        @Override
        protected Void doInBackground(Object... object) {
            firebaseFragment.uploadLesson(selectedLesson_id);
            return null;
        }
    }


    /**
     * Other helper methods. Handle for editing/deleting.
     */
    // Helper function to delete lesson data and update the view
    private void deleteLesson(long _id) {
        Log.d(TAG, "deleteLesson _id:" + _id);
        // Call the fragment for showing the delete uploadDialog
        DialogFragment deleteLessonFragment = new DeleteLessonLocallyDialogFragment();
        // Pass the _id of the lesson
        Bundle bundle = new Bundle();
        bundle.putLong("_id", _id);
        deleteLessonFragment.setArguments(bundle);
         // Show the uploadDialog box
        deleteLessonFragment.show(getSupportFragmentManager(), "DeleteLessonLocallyDialogFragment");
    }

    // Helper function to delete lesson data from cloud (delete the lesson document)
    private void deleteLessonFromCloud(long lesson_id) {

        // query the images to delete
        String selection = LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_LESSON_ID + "=?";
        String selectionArgs[] = {Long.toString(lesson_id)};
        Cursor cursor = this.getContentResolver().query(
                LessonsContract.MyCloudFilesToDeleteEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        // save the total images to delete
        if (cursor != null) {
            totalImagesToDelete = cursor.getCount();
            cursor.close();
        } else {
            totalImagesToDelete = 0;
        }

        imagesToDeleteCount = 0;

        // call the Dialog Fragment
        Log.d(TAG, "deleteLessonFromCloudDatabase lesson_id:" + lesson_id);
        // Call the fragment for showing the delete uploadDialog
        DialogFragment deleteLessonCloudFragment = new DeleteLessonOnCloudDialogFragment();
        // Pass the _id of the lesson
        Bundle bundle = new Bundle();
        bundle.putLong("lesson_id", lesson_id);
        deleteLessonCloudFragment.setArguments(bundle);
        // Show the Dialog Fragment box
        deleteLessonCloudFragment.show(getSupportFragmentManager(), "DeleteLessonOnCloudDialogFragment");
    }

    // Helper function to edit lesson
    private void editLesson(long _id) {
        Log.d(TAG, "editLesson _id:" + _id);
        // Create a new intent to start an EditTaskActivity
        Class destinationActivity = EditLessonActivity.class;
        Intent editLessonIntent = new Intent(mContext, destinationActivity);
        editLessonIntent.putExtra(SELECTED_LESSON_ID, _id);
        startActivity(editLessonIntent);
        // Deselect the last view selected
        mainFragment.deselectViews();
        selectedLesson_id = -1;
    }

    // Helper function to delete lesson part
    private void deleteLessonPart(long _id) {
        Log.d(TAG, "deleteLessonPart _id:" + _id);
        // Call the fragment for showing the delete uploadDialog
        DialogFragment deleteLessonPartFragment = new DeletePartDialogFragment();
        // Pass the _id of the lesson
        Bundle bundle = new Bundle();
        bundle.putLong("_id", _id);
        deleteLessonPartFragment.setArguments(bundle);
        // Show the uploadDialog box
        deleteLessonPartFragment.show(getSupportFragmentManager(), "DeletePartDialogFragment");
    }

    // Helper function to edit lesson part
    private void editLessonPart(long _id) {
        Log.d(TAG, "editLessonPart _id:" + _id);
        // Create a new intent to start an EditPartActivity
        Class destinationActivity = EditPartActivity.class;
        Intent editLessonPartIntent = new Intent(mContext, destinationActivity);
        editLessonPartIntent.putExtra(SELECTED_LESSON_PART_ID, _id);
        startActivity(editLessonPartIntent);
        // Deselect the last view selected
        partsFragment.deselectViews();
        selectedLessonPart_id = -1;
    }


    /**
     * The following methods are for receiving communication from other fragments or instances
     */

    // Method for receiving communication from the MainFragment
    @Override
    public void onLessonSelected(long _id) {

        // change the state to selected
        selectedLesson_id = _id;

        // clear the clicked state (this also happens in the main fragment, in deselectViews())
        clickedLesson_id = -1;

    }

    // Method for receiving communication from the MainFragment
    @Override
    public void onLessonClicked(long _id) {

        Log.d(TAG, "onLessonClicked _id:" + _id);

        // change the state to clicked
        clickedLesson_id = _id;

        // change the state to deselected (this also happens in the main fragment, in deselectViews())
        selectedLesson_id = -1;

        // Show the lesson parts fragment
        mainVisibility = GONE;
        lessonsContainer.setVisibility(mainVisibility);
        partsVisibility = VISIBLE;
        partsContainer.setVisibility(partsVisibility);
        // Set the drawer menu icon according to views
        if (mainVisibility == VISIBLE) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }
        // Inform the parts fragment
        partsFragment.setReferenceLesson(_id);
    }


    // Method for receiving communication from the DeleteLessonFragment
    @Override
    public void onDialogDeleteLessonLocallyPositiveClick(DialogFragment dialog, long _id) {
        ContentResolver contentResolver = mContext.getContentResolver();
        /* The delete method deletes the row by its _id */

        Uri uriToDelete = null;
        if (databaseVisibility.equals(USER_DATABASE)) {
            uriToDelete = LessonsContract.MyLessonsEntry.CONTENT_URI.buildUpon()
                    .appendPath("" + _id + "").build();
        } else if (databaseVisibility.equals(GROUP_DATABASE)) {
            uriToDelete = LessonsContract.GroupLessonsEntry.CONTENT_URI.buildUpon()
                    .appendPath("" + _id + "").build();
        }

        // Verify if the lesson with this _id has parts
        // Only delete the lesson if has no parts

        // Query the parts table with the same lesson_id
        String selection = null;
        Uri partsUri = null;
        if (databaseVisibility.equals(USER_DATABASE)) {
            selection = LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID + "=?";
            partsUri = LessonsContract.MyLessonPartsEntry.CONTENT_URI;
        } else if (databaseVisibility.equals(GROUP_DATABASE)) {
            selection = LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID + "=?";
            partsUri = LessonsContract.GroupLessonPartsEntry.CONTENT_URI;

        }

        String[] selectionArgs = {Long.toString(_id)};
        Cursor cursor = null;
        if (partsUri != null) {
            cursor = contentResolver.query(
                    partsUri,
                    null,
                    selection,
                    selectionArgs,
                    null);
        }
        if (cursor == null) {
            Log.e(TAG, "Failed to get cursor",
                    new Exception("onDialogDeleteLessonLocallyPositiveClick: Failed to get cursor."));
            Toast.makeText(this, "The application has found an error!\n" +
                    "Action canceled!", Toast.LENGTH_LONG).show();
            return;
        }

        int nRows = cursor.getCount();

        // If in the local database, check if lesson has parts and ask to delete parts first
        if (nRows > 1 && databaseVisibility.equals(USER_DATABASE)) {
            Log.d(TAG, "onDialogDeleteLessonLocallyPositiveClick number of lesson parts nRows:" + nRows);

            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "This lesson has " + nRows + " parts.\nPlease, delete the parts first!\n" +
                            "Action canceled!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();

            return;

        } else if (nRows > 1 && databaseVisibility.equals(GROUP_DATABASE)) {

            // In case of group, delete the images or videos directly
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Deleting the file images/videos from this group lesson...",
                    Snackbar.LENGTH_LONG);
            snackBar.show();
            Log.d(TAG, "onDialogDeleteLessonLocallyPositiveClick: " +
                    "Deleting the file images/videos from this group lesson");
            firebaseFragment.deleteImageLocalFilesOfGroupLesson(_id);
        }


        // Delete the lesson from local database
        int numberOfLessonsDeleted = 0;
        if (uriToDelete != null) {
            Log.d(TAG, "onDialogDeleteLessonLocallyPositiveClick: Uri to delete:" + uriToDelete.toString());
            numberOfLessonsDeleted = contentResolver.delete(uriToDelete, null, null);
        }

        if (numberOfLessonsDeleted > 0) {
            Toast.makeText(this,
                    numberOfLessonsDeleted + " item(s) removed!", Toast.LENGTH_LONG).show();
            // Deselect the last view selected
            mainFragment.deselectViews();
            selectedLesson_id = -1;
        }

        cursor.close();
    }


    // Method for receiving communication from the DeleteLessonFragment
    @Override
    public void onDialogDeleteLessonLocallyNegativeClick(DialogFragment dialog) {
        Toast.makeText(mContext,
                "Canceled!", Toast.LENGTH_LONG).show();
    }


    // receive communication from DeleteLessonOnCloudDialogFragment instance
    @Override
    public void onDialogDeleteLessonOnCloudPositiveClick(DialogFragment dialog, long lesson_id) {

        firebaseFragment.deleteLessonFromCloudDatabase(selectedLesson_id);

    }

    @Override
    public void onDialogDeleteLessonOnCloudNegativeClick(DialogFragment dialog) {
        Toast.makeText(mContext,
                "Canceled!", Toast.LENGTH_LONG).show();
    }

    // Receive from the MainFragment and PartsFragment the order to setting the idling resource


    // Receive communication from the PartsFragment
    @Override
    public void onPartSelected(long _id) {
        selectedLessonPart_id = _id;
    }

    // Receive communication from the PartsFragment
    @Override
    public void onPartClicked(long _id) {
        Log.d(TAG, "onPartClicked _id:" + _id);
        clickedLessonPart_id = _id;
        Log.d(TAG, "onPartClicked _id:" + _id);
        // Create a new intent to start an PartDetailActivity
        Class destinationActivity = PartDetailActivity.class;
        Intent partDetailIntent = new Intent(mContext, destinationActivity);
        partDetailIntent.putExtra(CLICKED_LESSON_PART_ID, _id);
        partDetailIntent.putExtra(DATABASE_VISIBILITY, databaseVisibility);
        startActivity(partDetailIntent);
    }

    /**
     * Receive communication form DeleteDialogPartFragment
     * Delete the part lesson row by its _id.
     */
    @Override
    public void onDialogDeletePartPositiveClick(DialogFragment dialog, long part_id) {

        ContentResolver contentResolver = mContext.getContentResolver();

        // First, save the cloud file reference in the form "images/001/file_name" or
        // "videos/001/file_name" where 001 is the lesson_id (not the part_id) in the
        // var fileReference
        // This is necessary to be able to delete from Firebase Storage

        String selection = LessonsContract.MyLessonPartsEntry._ID + "=?";
        String[] selectionArgs = {Long.toString(part_id)};
        Cursor cursor = null;
        if (contentResolver != null) {
            cursor = contentResolver.query(LessonsContract.MyLessonPartsEntry.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    null);
        }

        String fileReference = null;

        if (cursor == null) { return; }  // no files in the table

        if (cursor.moveToLast()) {

            // get info to build the fileRef
            String cloud_image_uri = cursor.getString(cursor.
                    getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_IMAGE_URI));
            String cloud_video_uri = cursor.getString(cursor.
                    getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_CLOUD_VIDEO_URI));

            // get the lesson_id
            long lesson_id = cursor.getLong(cursor.
                    getColumnIndex(LessonsContract.MyLessonPartsEntry.COLUMN_LESSON_ID));

            // build the fileRef
            if (cloud_image_uri != null) {
                String[] filePathParts = cloud_image_uri.split("/");
                fileReference = filePathParts[1] + "/" + filePathParts[2] + "/" + filePathParts[3];
            } else if (cloud_video_uri != null) {
                String[] filePathParts = cloud_video_uri.split("/");
                fileReference = filePathParts[1] + "/" + filePathParts[2] + "/" + filePathParts[3];
            }

            Log.d(TAG, "onDialogDeletePartPositiveClick fileReference:" + fileReference);

            cursor.close();

            // Now, delete the part from lesson parts table, and in case of success
            // store the fileRef (if it exists) in the my_cloud_files_to_delete
            Uri uriToDelete = null;

            if (databaseVisibility.equals(USER_DATABASE)) {
                uriToDelete = LessonsContract.MyLessonPartsEntry.CONTENT_URI.buildUpon()
                        .appendPath("" + part_id + "").build();
            } else if (databaseVisibility.equals(GROUP_DATABASE)) {
                uriToDelete = LessonsContract.GroupLessonPartsEntry.CONTENT_URI.buildUpon()
                        .appendPath("" + part_id + "").build();
            }

            int numberOfPartsDeleted = 0;
            if (uriToDelete != null) {
                Log.d(TAG, "onDialogDeletePartPositiveClick: Uri to delete:" + uriToDelete.toString());
                numberOfPartsDeleted = contentResolver.delete(uriToDelete, null, null);
            }

            // verify if there is a reference to the cloud (if the file has been uploaded before)
            // and then save that reference in the my_cloud_files_to_delete for future deletion
            if (numberOfPartsDeleted > 0 && fileReference != null) {
                Toast.makeText(this,
                        numberOfPartsDeleted + " item(s) removed!", Toast.LENGTH_LONG).show();

                // store the fileRef in the table my_cloud_files_to_delete
                ContentValues content = new ContentValues();
                content.put(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_FILE_REFERENCE, fileReference);
                content.put(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_LESSON_ID, lesson_id);
                Uri uri = contentResolver.insert(LessonsContract.MyCloudFilesToDeleteEntry.CONTENT_URI, content);
                Log.d(TAG, "onDialogDeletePartPositiveClick inserted uri:" + uri);
            }
        }

        // Deselect the last view selected
        partsFragment.deselectViews();
        selectedLessonPart_id = -1;
    }


    // Receive communication form DeleteDialogPartFragment
    @Override
    public void onDialogDeletePartNegativeClick(DialogFragment dialog) {
        Toast.makeText(mContext,
                "Canceled!", Toast.LENGTH_LONG).show();
    }

    /**
     * Manage the upload of images
     */
    // Receive communication form MyFirebaseFragment instance
    @Override
    public void onUploadImageSuccess() {

        uploadCount++;

        Log.d(TAG, "uploadCount:"  + uploadCount);
        firebaseFragment.addToLog("upload count " +  uploadCount + "/" + uploadCountFinal);

        if (uploadCount >= uploadCountFinal) {

            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Upload of images complete successfully!" +
                            "\nNow uploading the lesson text...",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();

            firebaseFragment.addToLog(RELEASE);

            // NOW UPLOAD THE DATABASE

            // --> CALL THE FUNCTION
            uploadLesson();

        }
    }


    @Override
    public void onUploadImageFailure(@NonNull Exception e) {

        Toast.makeText(mContext,
                "Error on uploading:" + e.getMessage(), Toast.LENGTH_LONG).show();
        Log.e(TAG, "onUploadImageFailure error:" + e.getMessage());

        uploadCount++;

        Log.d(TAG, "uploadCount:"  + uploadCount);
        firebaseFragment.addToLog("upload count " + uploadCount +
                "/" + uploadCountFinal);

        if (uploadCount >= uploadCountFinal) {

            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Upload of images complete, but " +
                            "with error!" + "\nPlease, see the log!\nNow uploading the text...",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();

            firebaseFragment.addToLog(RELEASE);

            // NOW UPLOAD THE DATABASE
            uploadLesson();

        }
    }


    @Override
    public void onUploadLessonSuccess() {

        uploadCount++;

        Log.d(TAG, "uploadCount:"  + uploadCount);
        firebaseFragment.addToLog("upload count " + uploadCount);

        if (uploadCount >= uploadCountFinal) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Upload of text complete successfully!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();

            loadingIndicator = false;
            mainFragment.setLoadingIndicator(false);
            mainFragment.deselectViews();
            selectedLesson_id = -1;

            firebaseFragment.addToLog(RELEASE);
        }
    }

    @Override
    public void onUploadLessonFailure(@NonNull Exception e) {
        Toast.makeText(mContext,
                "Error on uploading:" + e.getMessage(), Toast.LENGTH_LONG).show();
        Log.e(TAG, "onUploadDatabaseFailure error:" + e.getMessage());

        uploadCount++;

        Log.d(TAG, "uploadCount:"  + uploadCount);
        firebaseFragment.addToLog("upload count " + uploadCount);

        if (uploadCount >= uploadCountFinal) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Upload of text complete, but " +
                            "with error!" + "\nPlease, see the log!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();

            loadingIndicator = false;
            mainFragment.setLoadingIndicator(false);
            mainFragment.deselectViews();
            selectedLesson_id = -1;

            firebaseFragment.addToLog(RELEASE);
        }
    }


    /**
     * Manage the database download
     */
    @Override
    public void onDownloadDatabaseSuccess(int nImagesToDownload) {

        downloadCountFinal = nImagesToDownload;

        if (nImagesToDownload == 0){

            loadingIndicator = false;
            mainFragment.setLoadingIndicator(false);

            Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Download completed.",
                    Snackbar.LENGTH_SHORT);
            snackBar.show();

        } else {

            Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Download of text completed.\n" +
                    "Now, downloading the images...",
                    Snackbar.LENGTH_SHORT);
            snackBar.show();
        }

        firebaseFragment.addToLog(RELEASE);
    }


    @Override
    public void onDownloadDatabaseFailure(@NonNull Exception e) {

        loadingIndicator = false;
        mainFragment.setLoadingIndicator(false);

        final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                "Error in downloading the database:" + "\n" + e.getMessage() +
                "\nPlease, see the log!",
                Snackbar.LENGTH_INDEFINITE);
        snackBar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
            }
        });
        snackBar.show();

        Log.e(TAG, "onDownloadFailure error:" + e.getMessage());

        firebaseFragment.addToLog(RELEASE);
    }

    @Override
    public void onDownloadImageSuccess() {

        downloadCount++;

        Log.d(TAG, "downloadCount:"  + downloadCount + "/" + downloadCountFinal);
        firebaseFragment.addToLog("download count " + downloadCount + "/" + downloadCountFinal);

        if (downloadCount >= downloadCountFinal) {

            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Download of images complete successfully!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();

            // FINISHED DOWNLOADING
            loadingIndicator = false;
            mainFragment.setLoadingIndicator(false);

            firebaseFragment.addToLog(RELEASE);
        }

    }


    @Override
    public void onDownloadImageFailure(@NonNull Exception e) {

        downloadCount++;

        if (downloadCount >= downloadCountFinal) {

            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Download of images complete with error!" + "\n" + e.getMessage() +
                            "\nPlease, see the log!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();

            // FINISHED DOWNLOADING
            loadingIndicator = false;
            mainFragment.setLoadingIndicator(false);

            firebaseFragment.addToLog(RELEASE);
        }

    }


    @Override
    public void onDeleteCloudDatabaseSuccess() {

        firebaseFragment.addToLog(RELEASE);

        final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                "Lesson text deleted from cloud database!" +
                        "\nNow, deleting the images/videos...",
                Snackbar.LENGTH_INDEFINITE);
        snackBar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
            }
        });
        snackBar.show();

        long lesson_id = selectedLesson_id;

        firebaseFragment.deleteImageFilesFromStorage(lesson_id);

        // Deselect the last view selected
        mainFragment.deselectViews();
        selectedLesson_id = -1;

    }

    @Override
    public void onDeleteCloudDatabaseFailure(@NonNull Exception e) {

        firebaseFragment.addToLog(RELEASE);

        final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                "Error on deleting text from Cloud:" + e.getMessage() +
                "\nNow it will try to delete the images/videos...",
                Snackbar.LENGTH_INDEFINITE);
        snackBar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
            }
        });
        snackBar.show();

        long lesson_id = selectedLesson_id;

        firebaseFragment.deleteImageFilesFromStorage(lesson_id);
    }

    @Override
    public void onDeleteCloudImagesSuccess(int nRowsDeleted) {

        Log.d(TAG, "onDeleteCloudImagesSuccess");

        imagesToDeleteCount++;

        if (!(nRowsDeleted > 0)) {

            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "No images to delete!\n" +
                    "The action finished successfully!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();

        } else  if (imagesToDeleteCount >= totalImagesToDelete) {

            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Deletion of images completed successfully!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackBar.dismiss();
                }
            });
            snackBar.show();
        }

        firebaseFragment.addToLog(RELEASE);
    }

    @Override
    public void onDeleteCloudImagesFailure(@NonNull Exception e) {

        Log.e(TAG, "onDeleteCloudImagesFailure error:" + e.getMessage());

        final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                "Error in deleting image!",
                Snackbar.LENGTH_INDEFINITE);
        snackBar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackBar.dismiss();
            }
        });
        snackBar.show();

        firebaseFragment.addToLog(RELEASE);
    }


    /**
     * Methods for handling the activity state change
     */

    @Override
    public void onIdlingResource(Boolean value) {
        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(value);
        }
    }

    // This method is saving the visibility of the fragments in static vars
    @Override
    public void onSaveInstanceState(Bundle outState) {

        mainVisibility = lessonsContainer.getVisibility();
        partsVisibility = partsContainer.getVisibility();
        logVisibility = logContainer.getVisibility();

        outState.putLong(CLICKED_LESSON_ID, clickedLesson_id);
        outState.putLong(SELECTED_LESSON_ID, selectedLesson_id);
        outState.putLong(CLICKED_LESSON_PART_ID, clickedLessonPart_id);
        outState.putLong(SELECTED_LESSON_PART_ID, selectedLessonPart_id);

        outState.putInt(MAIN_VISIBILITY, mainVisibility);
        outState.putInt(PARTS_VISIBILITY, partsVisibility);
        outState.putInt(LOG_VISIBILITY, logVisibility);

        outState.putString(DATABASE_VISIBILITY, databaseVisibility);
        outState.putString(LOCAL_USER_UID, mUserUid);

        outState.putBoolean(LOADING_INDICATOR, loadingIndicator);
        outState.putInt(UPLOAD_COUNT, uploadCount);
        outState.putInt(UPLOAD_COUNT_FINAL, uploadCountFinal);
        outState.putInt(DOWNLOAD_COUNT, downloadCount);
        outState.putInt(DOWNLOAD_COUNT_FINAL, downloadCountFinal);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        String modeOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));
        Log.d(TAG, "onSharedPreferenceChanged modeOption:" + modeOption);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Attach the Firebase Auth listener
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove Firebase Auth listener
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener from PreferenceManager
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }





    private void testUtil() {
        Toast.makeText(this, "testUtil", Toast.LENGTH_LONG).show();

//        MyRefreshUserDatabase refreshUserDatabase = new MyRefreshUserDatabase(this);
//        String[] params = {databaseVisibility, mUserUid};
//        refreshUserDatabase.execute(params);

        Intent downloadIntent = new Intent(this, MyDownloadService.class);
        downloadIntent.putExtra(DATABASE_VISIBILITY, databaseVisibility);
        downloadIntent.putExtra(LOCAL_USER_UID, mUserUid);
        //downloadIntent.putExtra("receiver", myReceiver);
        startService(downloadIntent);

    }


    @Override
    public void onRefreshUserSuccess(List<String> messages) {
        Log.d(TAG, "onRefreshUserSuccess messages:" + messages.toString());
        Toast.makeText(this, "onRefreshUserSuccess:" + messages.toString(),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRefreshUserFailure(Exception e) {
        Log.d(TAG, "onRefreshUserFailure exception:" + e.toString());
        Toast.makeText(this, "onRefreshUserFailure " +
                e.getMessage(), Toast.LENGTH_LONG).show();
    }


    // Setup the callback for when data is received from the service
//    public void setupServiceReceiver() {
//
//        myReceiver = new MyReceiver(new Handler());
//
//        // Specify what happens when data is received from the service
//        myReceiver.setReceiver(new MyReceiver.Receiver() {
//            @Override
//            public void onReceiveResult(int resultCode, Bundle resultData) {
//                if (resultCode == RESULT_OK) {
//                    String resultValue = resultData.getString("resultValue");
//                    Log.d(TAG, "MyReceiver onReceiveResult:" + resultValue);
//                    Toast.makeText(MainActivity.this, resultValue, Toast.LENGTH_LONG).show();
//                }
//            }
//        });
//    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(MyDownloadService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(myUtilReceiver, filter);

    }

    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(myUtilReceiver);
    }


    private BroadcastReceiver myUtilReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);

            if (resultCode == RESULT_OK) {
                String resultValue = intent.getStringExtra("resultValue");
                Log.d(TAG, "BroadcastReceiver onReceive:" + resultValue);

                if (resultValue.equals("[REFRESH USER FINISHED OK]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Refreshing of the user database finished successfully!",
                            Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackBar.dismiss();
                        }
                    });
                    snackBar.show();
                }

                if (resultValue.equals("[REFRESH USER FINISHED WITH ERROR]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Refreshing of the user database finished, but with error." +
                                    "\nPlease, see the log!", Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackBar.dismiss();
                        }
                    });
                    snackBar.show();
                }


                if (resultValue.equals("[REFRESH GROUP FINISHED OK]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Refreshing of the group database finished successfully!",
                            Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackBar.dismiss();
                        }
                    });
                    snackBar.show();
                }

                if (resultValue.equals("[REFRESH GROUP FINISHED WITH ERROR]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Refreshing of the group database finished, but with error." +
                                    "\nPlease, see the log!", Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            snackBar.dismiss();
                        }
                    });
                    snackBar.show();
                }



            }
        }
    };


}
