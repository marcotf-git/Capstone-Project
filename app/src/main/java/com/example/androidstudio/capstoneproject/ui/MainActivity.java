package com.example.androidstudio.capstoneproject.ui;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androidstudio.capstoneproject.R;
import com.example.androidstudio.capstoneproject.data.LessonsContract;
import com.example.androidstudio.capstoneproject.sync.MyDeleteService;
import com.example.androidstudio.capstoneproject.sync.MyDownloadService;
import com.example.androidstudio.capstoneproject.sync.MyLog;
import com.example.androidstudio.capstoneproject.sync.MyUploadService;
import com.example.androidstudio.capstoneproject.sync.ScheduledUtilities;
import com.example.androidstudio.capstoneproject.utilities.InsertTestDataUtil;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;


/**
 * This app helps the user to create small lessons, with text and videos or images, using their own
 * devices (phones or tablets), and sharing this content with a group of users.
 *
 * Each user can create and save their content in the device, and upload it to the cloud. The group
 * can download that content, and save it locally on the device, including the images or videos,
 * for attending the classes.
 *
 * The process requires login to the remote server, in case, the Firebase
 * (https://firebase.google.com), which handles all the login and logout, and stores the data.
 *
 * The app has two modes: 'view' mode and 'create' mode.
 *
 * If the user selects 'create' mode, it will show only the options to create, edit and delete,
 * in the action items of the app bar.
 *
 * If the user shows 'view' mode, it will show only option to read and sync the content with the
 * remote server (download).
 *
 * According to the modes of the app, the local database has two types of tables:
 * - the tables "group_ ... " is a copy of the remote database.
 * - the tables "my_ ... " is the content created by the user.
 *
 * The database and its tables are handled by a content provider LessonsContentProvider.
 * The provider is queried by a cursor loader, which returns a cursor object.
 *
 * In the 'view' mode, the view layout will be the activity_main.xml, which has containers for
 * fragments. The fragment_main will have a RecyclerView, populated with a LinearLayoutManager and
 * a custom adapter LessonsListAdapter. The cursor provided by the loader is passed to the adapter
 * with the data that will be shown.
 *
 * In the 'view' mode, if the user shows the option to sync the database, it will call a job
 * scheduled service (Intent Service), with the help of Firebase JobDispatcher, or call the service
 * immediately, with an intent call to the Intent Service.
 *
 * The result is informed via intent sent by a local broadcast, with messages that will be received
 * by a Receiver in the MainActivity, and trigger, according to the content, messages informing the
 * user about if the task has finished OK or with errors.
 *
 * In case of the Scheduled Jobs, the user also have a notification, that will be received even
 * if the app is closed, and has a pending intent incorporated, to open the app.
 *
 * In the 'create' mode, if the user selects the option to add a lesson title, it will open another
 * activity AddLessonActivity to add the title. If the user selects to delete and then click on an
 * item, the item will be deleted after a confirmation. The confirmation is handled by a Dialog
 * fragment. If the user selects to edit and then click, it will open an activity EditLessonActivity
 * to edit that item.
 *
 * There are specific rules in the app and in Firebase protecting the data of one user from another.
 *
 * The user only can upload or download data from the cloud when logged. The login process is
 * handled by the Firebase Authentication.
 *
 * The cloud data is divided between the Firebase Realtime Database (text), and the Firebase
 * Storage (images and videos). The local data is divided between:
 * - a table with the lesson data (text)
 * - a table with the lesson parts data (text)
 * - the local folders for file (blob) storage
 *
 * The table with the lesson parts will store the URIs of the images or videos of that part.
 *
 * The upload of data has the following algorithm:
 * 1) first, upload the images or videos to Storage
 * 2) get the URIs info and save in the local database, in the table of the lesson parts
 * 3) then, upload the two tables (lesson and parts) to the Firebase Database
 *
 * The user can choose to download the group data (in the group table) or its own data (in the user
 * table).
 *
 * To download data from the cloud, the app has the algorithm:
 * 1) The clearing process:
 * 1.1) before the download, the local group data is cleared: the tables are deleted from a local
 * database, and the files are deleted from local folder (/data/user/0/appUri/files/...)
 * 1.2) the clearing process does not delete all the data when downloading only the user lessons;
 * it deletes only what is equal to that downloaded
 * 2) The download:
 * 2.1) download the tables from Firebase Database
 * 2.2) in case of group data:
 * 2.2.1) get the URIs of those tables, and with them, download the files from Storage
 * 2.2.2) write the file in the local folder, in the internal storage
 * 2.2.3) save the local URI in the table of the parts
 *
 * When the app reads the data, when the user clicks on the lesson part, the ExoPlayer
 * (https://developer.android.com/guide/topics/media/exoplayer) or the Picasso
 * (http://square.github.io/picasso/) will get that file URI, and load the file from a local folder
 * into the specific view.
 *
 * To delete a user lesson, the app has the algorithm:
 * a) it is possible to delete or locally, or in the cloud
 *
 * a.1) when deleting user lesson locally:
 * 1) only delete if there are no parts
 * 2) when deleting the lesson part row, store the reference of the cloud file URI Storage (reading
 * it from the lesson parts table) in a specific table, which will be used to delete the file in
 * the cloud, when the user chooses to do it in future
 *
 * When the user chooses to delete the lesson from the cloud, it will also delete the files in the
 * Storage, but it won't be deleted locally.
 *
 * a.2) when deleting user lesson only in the cloud:
 * 1) delete from cloud Database and save the URIs of the files in a specific local table, that
 * will store the files for deletion from Storage
 * 2) query that table, and with the information, delete from the Storage
 *
 * To edit the local data, the user can edit only their tables:
 * 1) choose what to edit and edit (click on the item and on the 'edit' icon)
 * 2) choose to pick another image (the 'Fab' button)
 * 2.1) the old image, if it has cloud URI, will be deleted but its reference will be saved in that
 * specific table, for future deletion from Storage
 * 2.2) the new one (or the new video) take the place
 *
 * So, the app implements `CRUD` on local user data, and `read` (download), `write` (upload) and
 * `delete` in user cloud data, managing the deletion of the images or videos from Firebase Storage
 * for when deleting the whole lesson data from Firebase Database.
 *
 * And the app implements only `read` from the cloud, in case of group data (inside the group there
 * will be also the user lessons, but saved in the group tables).
 *
 * The app menu is contextual: the options change according to the user actions.
 *
 * Finally, the cloud communication is saved in a log table, updated at the same time as the
 * services are being executed. The log can be viewed by an option in the drawer menu.
 *
 * This app is for studying purposes.
 * Thanks to my family, to their support.
 * Thanks to Udacity, and Google, and all others that make this (learning) app possible!
 *
 * Marcos Tewfiq
 *
 */


public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        MainFragment.OnLessonListener,
        PartsFragment.OnLessonPartListener,
        DeleteLessonLocallyDialogFragment.DeleteLessonDialogListener,
        DeletePartDialogFragment.DeletePartDialogListener,
        DeleteLessonOnCloudDialogFragment.DeleteLessonCloudDialogListener {


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
    private static final String USER_UID = "userUid";

    private static final String LOADING_INDICATOR = "loadingIndicator";

    private static final String UPLOAD_ERROR = "UploadError";
    private static final String DOWNLOAD_ERROR = "DownloadError";
    private static final String DELETION_ERROR = "DeletionError";

    // Final strings
    private static final String USER_DATABASE = "userDatabase";
    private static final String GROUP_DATABASE = "groupDatabase";
    private static final String ANONYMOUS = "anonymous";

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

    // User data variables
    private String mUsername;
    private String mUserEmail;

    // Declare the FirebaseAnalytics
    private FirebaseAnalytics mFirebaseAnalytics;

    // Declare the FirebaseAuthentication
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

    // A single-pane display refers to phone screens, and two-pane to tablet screens
    private boolean mTwoPane;

    // Dialogs
    private AlertDialog uploadDialog;
    private AlertDialog downloadJobDialog;
    private AlertDialog uploadJobDialog;

    // Fragments
    private MainFragment mainFragment;
    private PartsFragment partsFragment;
    private LogFragment logFragment;

    private MyLog myLog;

    // Context
    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the FirebaseAnalytics instance
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Obtain a myLog instance
        myLog = new MyLog(this);

        // Init the main view
        setContentView(R.layout.activity_main);

        // Determine if you are creating a two-pane or single-pane display
        // This LinearLayout will only initially exists in the two-pane tablet case
        mTwoPane = findViewById(R.id.drawer_tablet_land_layout) != null;

        Log.d(TAG, "mTwoPane:" + mTwoPane);

        LinearLayout listContainer = findViewById(R.id.linear_layout);
        listContainer.setVisibility(INVISIBLE);

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
            mUserUid = savedInstanceState.getString(USER_UID);

            loadingIndicator = savedInstanceState.getBoolean(LOADING_INDICATOR);

        } else {
            // Initialize the state vars
            clickedLesson_id = -1;
            selectedLesson_id = -1;
            clickedLessonPart_id = -1;
            selectedLessonPart_id = -1;

            if (mTwoPane) {
                // tablet visibility
                mainVisibility = VISIBLE; // the lesson list
                partsVisibility = VISIBLE; // the parts list
                logVisibility = GONE; // the log view
            } else {
                // Phone visibility
                mainVisibility = VISIBLE;
                partsVisibility = GONE;
                logVisibility = GONE;
            }

            // Recover the local user uid for handling the database global consistency
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            mUserUid = sharedPreferences.getString(USER_UID,"");
            databaseVisibility = sharedPreferences.getString(DATABASE_VISIBILITY, USER_DATABASE);
            loadingIndicator = false;
        }

        // Handle screen rotation cases
        if (mTwoPane && (mainVisibility == VISIBLE || partsVisibility == VISIBLE)) {
            mainVisibility = VISIBLE;
            partsVisibility = VISIBLE;
        } else if (clickedLesson_id != -1) {
            Log.d(TAG, "Handle screen rotation cases: clickedLesson_id:" + clickedLesson_id);
            mainVisibility = GONE;
            partsVisibility = VISIBLE;
        }

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

        } else {
            mainFragment = (MainFragment) fragmentManager.findFragmentByTag("MainFragment");
            partsFragment = (PartsFragment) fragmentManager.findFragmentByTag("PartsFragment");

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
        if (mTwoPane) {
            mDrawerLayout = findViewById(R.id.drawer_tablet_land_layout);
        } else {
            mDrawerLayout = findViewById(R.id.drawer_layout);
        }

        // Drawer options
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Dialog for immediate upload the lesson
        builder.setTitle(R.string.upload_confirm)
                .setPositiveButton(R.string.upload, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        uploadLesson();
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

                        if(noWifi()) {
                            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                                    "There isn't Wifi! Please, verify the connection! Action canceled!",
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

                        if (mUsername.equals(ANONYMOUS)) {
                            final Snackbar snackBarAnonymus = Snackbar.
                                    make(findViewById(R.id.drawer_layout), "Please, login to upload!",
                                            Snackbar.LENGTH_INDEFINITE);
                            snackBarAnonymus.setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    snackBarAnonymus.dismiss();
                                }
                            });
                            snackBarAnonymus.show();
                            return;
                        }

                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());
                        myLog.addToLog(time_stamp + "\nThe download are going to start in the " +
                                "next " + ScheduledUtilities.SYNC_INTERVAL_SECONDS + "s" +
                                " approximately, on non-metered networks, and by a scheduled" +
                                " and more battery friendly process.");

                        // call the job for sync the group table
                        ScheduledUtilities.scheduleDownloadDatabase(mContext, mUserUid, databaseVisibility);

                        final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                                "Download has started in background (if un-metered network)." +
                                        " Please, see the log...",
                                Snackbar.LENGTH_INDEFINITE);
                        snackBar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                snackBar.dismiss();
                            }
                        });
                        snackBar.show();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        downloadJobDialog = builder.create();


        // Dialog for the upload job service (that will run in background)
        builder.setTitle(R.string.upload_job_type)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Log.d(TAG, "uploadJobDialog selectedLesson_id:" + selectedLesson_id);

                        // test the pre-requisites to proceed
                        if(noWifi()) {
                            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                                    "There isn't Wifi! Please, verify the connection! Action canceled!",
                                    Snackbar.LENGTH_INDEFINITE);
                            snackBar.setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {  snackBar.dismiss();  }
                            });
                            snackBar.show();
                            return;
                        }

                        if (selectedLesson_id == -1) {
                            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                                    "Please, select a lesson to upload the images or videos!",
                                    Snackbar.LENGTH_INDEFINITE);
                            snackBar.setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) { snackBar.dismiss(); }
                            });
                            snackBar.show();
                            return;
                        }

                        if (mUsername.equals(ANONYMOUS)) {
                            final Snackbar snackBarAnonymous = Snackbar.
                                    make(findViewById(R.id.drawer_layout),"Please, login to upload!",
                                    Snackbar.LENGTH_INDEFINITE);
                            snackBarAnonymous.setAction("Dismiss", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {  snackBarAnonymous.dismiss(); }
                            });
                            snackBarAnonymous.show();
                            return;
                        }

                        String time_stamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US)
                                .format(new Date());
                        myLog.addToLog(time_stamp + "\nThe upload are going to start in the " +
                                        "next " + ScheduledUtilities.SYNC_INTERVAL_SECONDS + "s" +
                                        " approximately, on non-metered networks, and by a scheduled" +
                                        " and more battery friendly process.");

                        // call the job for upload the selected user lesson (images and text)
                        ScheduledUtilities.scheduleUploadLesson(mContext, mUserUid, selectedLesson_id);

                        final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                                "Uploading has started in background (if un-metered net)." +
                                        "Please, see the log...",
                                Snackbar.LENGTH_INDEFINITE);
                        snackBar.setAction("Dismiss", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) { snackBar.dismiss(); }
                        });
                        snackBar.show();


                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {  dialog.cancel(); }
                });

        uploadJobDialog = builder.create();

        listContainer.setVisibility(VISIBLE);
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


    /**
     * The following methods handle the Firebase login/logout
     */

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
        sharedPreferences.edit().putString(USER_UID, mUserUid).apply();

        // Set the drawer
        mUsernameTextView.setText(mUsername);
        mUserEmailTextView.setText(mUserEmail);

        // Set the visibility of the upload action icon
        if (null != mMenu) {
            contextualizeMenu();
        }

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
                actionHome();
                return true;

            case R.id.select_view:
                optionSelectView();
                break;

            case R.id.select_create:
                optionSelectCreate();
                break;

            case R.id.action_edit:
                actionEdit();
                break;

            case R.id.action_download:
                actionDownload();
                break;

            case R.id.action_upload:
                uploadDialog.show();
                break;

            case R.id.action_job_download:
                downloadJobDialog.show();
                break;

            case R.id.action_job_upload:
                uploadJobDialog.show();
                break;

            case R.id.action_delete:
                actionDelete();
                break;

            case R.id.action_delete_from_cloud:
                actionDeleteFromCloud();
                break;

            case R.id.action_insert_fake_data:
                actionInsertFakeData();
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
    private void actionHome() {
        Log.d(TAG, "onOptionsItemSelected mainVisibility:" + mainVisibility);
        // Set the action according to the views visibility

        // Show the lesson parts fragment
        if (mTwoPane) {

            // two pane visibility (tablet landscape)
            if (mainVisibility == VISIBLE) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            } else if (logVisibility == VISIBLE) {
                logVisibility = GONE;
                logContainer.setVisibility(logVisibility);

                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .remove(logFragment)
                        .commit();

                mainVisibility = VISIBLE;
                partsVisibility = VISIBLE;
                lessonsContainer.setVisibility(mainVisibility);
                partsContainer.setVisibility(partsVisibility);

                actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
                contextualizeMenu();
            }

        } else {

            // portrait visibility or phone visibility
            if (mainVisibility == VISIBLE) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            } else if (mainVisibility == GONE && partsVisibility == VISIBLE) {
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
    }

    /**
     *  Functions to execute the menu options
     */
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

    private void actionEdit() {
        Log.d(TAG, "Deletion action selected");
        // Try to action first on the more specific item
        if (selectedLessonPart_id != -1) {
            editLessonPart(selectedLessonPart_id);
        } else if (selectedLesson_id != -1) {
            editLesson(selectedLesson_id);
        } else {
            Toast.makeText(this,
                    "Please, select an item to edit!", Toast.LENGTH_LONG).show();
        }
    }

    // This function will refresh the database, of the user or the group.
    // It will download the photos from Firebase Storage, but only for the group table (the user table
    // already has the images in the folder).
    // In case of the group, it will also delete the old files, and the old rows in the table.
    private void actionDownload() {

        if (mUsername.equals(ANONYMOUS)) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Please, login to download!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {snackBar.dismiss();}
            });
            snackBar.show();
            return;
        }

        if(isOffline()) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "There isn't internet! Please, verify the connection! Action canceled!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {snackBar.dismiss();}
            });
            snackBar.show();
            return;
        }

        Intent downloadIntent = new Intent(this, MyDownloadService.class);
        downloadIntent.putExtra(DATABASE_VISIBILITY, databaseVisibility);
        downloadIntent.putExtra(USER_UID, mUserUid);
        startService(downloadIntent);

        if (databaseVisibility.equals(GROUP_DATABASE)) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Refreshing of the group database has started...", Snackbar.LENGTH_LONG);
            snackBar.show();
        } else if (databaseVisibility.equals(USER_DATABASE)) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Refreshing of the user database has started...", Snackbar.LENGTH_LONG);
            snackBar.show();
        }
    }


    private void uploadLesson() {

        if (selectedLesson_id == -1) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Please, select a lesson to upload!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {snackBar.dismiss();}
            });
            snackBar.show();
            return;
        }

        if (mUsername.equals(ANONYMOUS)) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Please, login to upload!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {snackBar.dismiss();}
            });
            snackBar.show();
            return;
        }

        if(isOffline()) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "There isn't internet! Please, verify the connection! Action canceled!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {snackBar.dismiss();}
            });
            snackBar.show();
            return;
        }

        Intent uploadIntent = new Intent(this, MyUploadService.class);
        uploadIntent.putExtra(SELECTED_LESSON_ID, selectedLesson_id);
        uploadIntent.putExtra(USER_UID, mUserUid);
        startService(uploadIntent);

        final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                "Uploading of the lesson has started...", Snackbar.LENGTH_LONG);
        snackBar.show();

    }


    private void actionDelete() {
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

    private void actionDeleteFromCloud() {
        Log.d(TAG, "Delete from Cloud action selected");
        // Try to action first on the more specific item


        if  (selectedLesson_id == -1) {
            Toast.makeText(this,
                    "Please, select a lesson to delete from Cloud!", Toast.LENGTH_LONG).show();
            return;
        }

        if (mUsername.equals(ANONYMOUS)) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "Please, login to delete from cloud!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {snackBar.dismiss();}
            });
            snackBar.show();
            return;
        }

        if(isOffline()) {
            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "There isn't internet! Please, verify the connection! Action canceled!",
                    Snackbar.LENGTH_INDEFINITE);
            snackBar.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {snackBar.dismiss();}
            });
            snackBar.show();
            return;
        }

        deleteLessonFromCloud(selectedLesson_id);
    }

    private void actionInsertFakeData() {
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
            mMenu.findItem(R.id.action_download).setVisible(true);
            mMenu.findItem(R.id.action_job_download).setVisible(true);
        } else {
            mMenu.findItem(R.id.action_download).setVisible(false);
            mMenu.findItem(R.id.action_job_download).setVisible(false);
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
            mMenu.findItem(R.id.action_download).setVisible(false);
            mMenu.findItem(R.id.action_job_download).setVisible(false);
            mMenu.findItem(R.id.action_edit).setVisible(false);
            mMenu.findItem(R.id.action_upload).setVisible(false);
            mMenu.findItem(R.id.action_job_upload).setVisible(false);
            mMenu.findItem(R.id.action_delete).setVisible(false);
            mMenu.findItem(R.id.action_delete_from_cloud).setVisible(false);
            mMenu.findItem(R.id.action_insert_fake_data).setVisible(false);
            mMenu.findItem(R.id.action_cancel).setVisible(false);
            //mMenu.findItem(R.id.action_cancel_task).setVisible(true);
            mButton.setVisibility(GONE);
        }

    }


    /**
     * Helper methods for closing fragments.
     */
    // Helper method for hiding the PartsFragment
    private void closePartsFragment() {
        Log.d(TAG, "closePartsFragment");
        // deselect the views on the fragment that will be closed
        partsFragment.deselectViews();
        // clear the reference var
        selectedLessonPart_id = -1;
        clickedLesson_id = -1;
        // Change the views
        partsVisibility = GONE;
        partsContainer.setVisibility(partsVisibility);
        mainVisibility = VISIBLE;
        lessonsContainer.setVisibility(mainVisibility);
        // Set the drawer menu icon according to views
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
    }

    /**
     * Other helper methods for editing/deleting.
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
    public void onLessonClicked(long _id, String lessonName) {

        Log.d(TAG, "onLessonClicked _id:" + _id + " lessonName:" + lessonName);

        // change the state to clicked
        clickedLesson_id = _id;

        // change the state to deselected (this also happens in the main fragment, in deselectViews())
        selectedLesson_id = -1;

        // Show the lesson parts fragment
        if (mTwoPane) {
            // tablet visibility
            mainVisibility = VISIBLE; // the lesson list
            partsVisibility = VISIBLE; // the parts list
        } else {
            // Phone visibility
            mainVisibility = GONE;
            partsVisibility = VISIBLE;
        }

        lessonsContainer.setVisibility(mainVisibility);
        partsContainer.setVisibility(partsVisibility);

        // Set the drawer menu icon according to views
        if (mainVisibility == VISIBLE) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }

        // Inform the parts fragment
        partsFragment.setReferenceLesson(_id, lessonName);
    }


    // Method for receiving communication from the DeleteLessonFragment
    // Delete the lesson on local database and local folder (image files)
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
            Toast.makeText(this, "The application has found an error! " +
                    "Action canceled!", Toast.LENGTH_LONG).show();
            return;
        }

        int nRows = cursor.getCount();

        // If in the local database, check if lesson has parts and ask to delete parts first
        if (nRows > 1 && databaseVisibility.equals(USER_DATABASE)) {
            Log.d(TAG, "onDialogDeleteLessonLocallyPositiveClick number of lesson parts nRows:" + nRows);

            final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                    "This lesson has " + nRows + " parts. Please, delete the parts first! " +
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
            deleteImageLocalFilesOfGroupLesson(_id);
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

    }

    // Helper method to delete local image files
    private void deleteImageLocalFilesOfGroupLesson(long lessonId) {

        // Query the parts table with the same lesson_id
        // find the uri's of the images to delete
        // delete the local files
        ContentResolver contentResolver = mContext.getContentResolver();
        String selection = LessonsContract.GroupLessonPartsEntry.COLUMN_LESSON_ID + "=?";
        String[] selectionArgs = {Long.toString(lessonId)};
        Cursor cursor = contentResolver.query(
                LessonsContract.GroupLessonPartsEntry.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                null);

        if (cursor == null) { return; }

        long nRows = cursor.getCount();

        if (nRows == 0) { return; }

        // Moves to the first part of that lesson
        cursor.moveToFirst();

        do {

            String localImageUri = cursor.getString(cursor.
                    getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_IMAGE_URI));
            String localVideoUri = cursor.getString(cursor.
                    getColumnIndex(LessonsContract.GroupLessonPartsEntry.COLUMN_LOCAL_VIDEO_URI));

            Log.d(TAG, "deleteImageLocalFilesOfGroupLesson localImageUri:" + localImageUri);
            Log.d(TAG, "deleteImageLocalFilesOfGroupLesson localVideoUri:" + localVideoUri);

            if (localImageUri != null) {
                Uri uri = Uri.parse(localImageUri);
                String fileName = uri.getLastPathSegment();
                Log.d(TAG, "localImageUri file name:" + fileName);
                File file = new File(mContext.getFilesDir(), fileName);
                Log.d(TAG, "localImageUri file exists:" + file.exists());
                try {
                    boolean fileDeleted = file.delete();
                    Log.d(TAG, "localImageUri fileDeleted:" + fileDeleted);
                } catch (Exception e) {
                    Log.e(TAG, "localImageUri:" + e.getMessage());
                }

            }

            if (localVideoUri != null) {
                Uri uri = Uri.parse(localVideoUri);
                String fileName = uri.getLastPathSegment();
                Log.d(TAG, "localVideoUri file name:" + fileName);
                File file = new File(mContext.getFilesDir(), fileName);
                Log.d(TAG, "localVideoUri file exists:" + file.exists());
                try {
                    boolean fileDeleted = file.delete();
                    Log.d(TAG, "localVideoUri fileDeleted:" + fileDeleted);
                } catch (Exception e) {
                    Log.e(TAG, "localVideoUri:" + e.getMessage());
                }
            }

            // get the next part
        } while (cursor.moveToNext());

    }


    // Method for receiving communication from the DialogDeleteLessonFragment (cancel option)
    @Override
    public void onDialogDeleteLessonLocallyNegativeClick(DialogFragment dialog) {
        Toast.makeText(mContext,
                "Canceled!", Toast.LENGTH_LONG).show();
    }


    // receive communication from DeleteLessonOnCloudDialogFragment instance
    @Override
    public void onDialogDeleteLessonOnCloudPositiveClick(DialogFragment dialog, long lesson_id) {

        // Start the intent service for this task
        Intent deleteIntent = new Intent(this, MyDeleteService.class);
        deleteIntent.putExtra(SELECTED_LESSON_ID, selectedLesson_id);
        deleteIntent.putExtra(USER_UID, mUserUid);
        startService(deleteIntent);

        final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                "Deletion of the lesson from cloud has started...", Snackbar.LENGTH_LONG);
        snackBar.show();

    }

    @Override
    public void onDialogDeleteLessonOnCloudNegativeClick(DialogFragment dialog) {
        Toast.makeText(mContext,
                "Canceled!", Toast.LENGTH_LONG).show();
    }


    // Receive communication from the PartsFragment (part selected)
    @Override
    public void onPartSelected(long _id) {
        selectedLessonPart_id = _id;
    }

    // Receive communication from the PartsFragment (part clicked)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Apply activity transition
            startActivity(partDetailIntent,
                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        } else {
            // Swap without transition
            startActivity(partDetailIntent);
        }

    }

    /**
     * Receive communication form DeleteDialogPartFragment
     * Delete the part lesson row from local database by its _id.
     */
    @Override
    public void onDialogDeletePartPositiveClick(DialogFragment dialog, long part_id) {

        ContentResolver contentResolver = mContext.getContentResolver();

        // First, save the cloud image file reference in the form "images/001/file_name" or
        // "videos/001/file_name" where 001 is the lesson_id (not the part_id) in the
        // var fileReference.
        // This reference is necessary to be able to delete from Firebase Storage.

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


            // Now,  store the fileRef (if it exists) in the my_cloud_files_to_delete
            // In case of success, delete the part from lesson parts table
            Uri uri = null;
            if (fileReference != null) {
                // store the fileRef in the table my_cloud_files_to_delete
                ContentValues content = new ContentValues();
                content.put(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_FILE_REFERENCE, fileReference);
                content.put(LessonsContract.MyCloudFilesToDeleteEntry.COLUMN_LESSON_ID, lesson_id);
                uri = contentResolver.insert(LessonsContract.MyCloudFilesToDeleteEntry.CONTENT_URI, content);
                Log.d(TAG, "onDialogDeletePartPositiveClick inserted uri:" + uri);
            }

            if ((uri != null) || (fileReference == null)) {

                // delete from local database
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

                if (numberOfPartsDeleted > 0) {
                    Toast.makeText(this,
                            numberOfPartsDeleted + " item(s) removed!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this,
                            "Error in deleting items! No item deleted!", Toast.LENGTH_LONG).show();
                }
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
     * Methods for handling the activity state change
     */
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
        outState.putString(USER_UID, mUserUid);
        outState.putBoolean(LOADING_INDICATOR, loadingIndicator);

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


    @Override
    protected void onStart() {
        super.onStart();

        // register the receiver to get communication from the service
        IntentFilter filterUpload = new IntentFilter(MyUploadService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(myUploadReceiver, filterUpload);

        IntentFilter filterDownload = new IntentFilter(MyDownloadService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(myDownloadReceiver, filterDownload);

        IntentFilter filterDelete = new IntentFilter(MyDeleteService.ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(myDeleteReceiver, filterDelete);
    }


    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(myUploadReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myDownloadReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myDeleteReceiver);
    }


    /**
     * Receivers that handles communication from services
     */
    // Define a receiver to listen for communication from the services (download service)
    private final BroadcastReceiver myDownloadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);

            if (resultCode == RESULT_OK) {
                String resultValue = intent.getStringExtra("resultValue");
                Log.d(TAG, "BroadcastReceiver onReceive:" + resultValue);

                if (resultValue.equals("[REFRESH USER FINISHED OK]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Refreshing of the user database has finished successfully!",
                            Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { snackBar.dismiss();}
                    });
                    snackBar.show();
                }

                if (resultValue.equals("[REFRESH USER FINISHED WITH ERROR]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Refreshing of the user database has finished, but with error." +
                                    " Please, see the log!", Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { snackBar.dismiss();}
                    });
                    snackBar.show();

                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                            "Download of the user data has finished, but with error.");
                    mFirebaseAnalytics.logEvent(DOWNLOAD_ERROR, bundle);
                }


                if (resultValue.equals("[REFRESH GROUP FINISHED OK]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Refreshing of the group database has finished successfully!",
                            Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {snackBar.dismiss();}
                    });
                    snackBar.show();
                }

                if (resultValue.equals("[REFRESH GROUP FINISHED WITH ERROR]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Refreshing of the group database has finished, but with error." +
                                    " Please, see the log!", Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {snackBar.dismiss();}

                    });
                    snackBar.show();

                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                            "Download of the group data has finished, but with error.");
                    mFirebaseAnalytics.logEvent(DOWNLOAD_ERROR, bundle);
                }
            }
        }
    };


    // Define a receiver to listen for communication from the services (upload service)
    private final BroadcastReceiver myUploadReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);

            if (resultCode == RESULT_OK) {
                String resultValue = intent.getStringExtra("resultValue");
                Log.d(TAG, "BroadcastReceiver onReceive:" + resultValue);

                if (resultValue.equals("[UPLOAD LESSON FINISHED OK]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Upload of the lesson has finished successfully!",
                            Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { snackBar.dismiss();}
                    });
                    snackBar.show();
                }

                if (resultValue.equals("[UPLOAD LESSON FINISHED WITH ERROR]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Upload of the lesson has finished, but with error." +
                                    " Please, see the log!", Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {snackBar.dismiss();}
                    });
                    snackBar.show();

                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                            "Upload of the lesson has finished, but with error.");
                    mFirebaseAnalytics.logEvent(UPLOAD_ERROR, bundle);

                }
            }
        }
    };

    // Define a receiver to listen for communication from the services (delete service)
    private final BroadcastReceiver myDeleteReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            int resultCode = intent.getIntExtra("resultCode", RESULT_CANCELED);

            if (resultCode == RESULT_OK) {
                String resultValue = intent.getStringExtra("resultValue");
                Log.d(TAG, "BroadcastReceiver onReceive:" + resultValue);

                if (resultValue.equals("[DELETE LESSON FINISHED OK]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Deletion of the lesson from cloud has finished successfully!",
                            Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) { snackBar.dismiss();}
                    });
                    snackBar.show();

                    // Deselect the last view selected
                    mainFragment.deselectViews();
                    selectedLesson_id = -1;
                }

                if (resultValue.equals("[DELETE LESSON FINISHED WITH ERROR]")) {
                    final Snackbar snackBar = Snackbar.make(findViewById(R.id.drawer_layout),
                            "Deletion of the lesson from cloud has finished, but with error." +
                                    " Please, see the log!", Snackbar.LENGTH_INDEFINITE);
                    snackBar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {snackBar.dismiss();}
                    });
                    snackBar.show();

                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                            "Deletion of the lesson from cloud has finished, but with error.");
                    mFirebaseAnalytics.logEvent(DELETION_ERROR, bundle);

                }
            }

        }
    };


    /**
     * Helper methods for checking internet state
     */
    // Verify if there is internet
    private boolean isOffline() {

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        }
        boolean isWifiConn = false;
        if (networkInfo != null) {
            isWifiConn = networkInfo.isConnected();
        }
        if (connMgr != null) {
            networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        }
        boolean isMobileConn = false;
        if (networkInfo != null) {
            isMobileConn = networkInfo.isConnected();
        }
        Log.d(TAG, "Wifi connected: " + isWifiConn);
        Log.d(TAG, "Mobile connected: " + isMobileConn);

        return !isWifiConn && !isMobileConn;

    }


    // Verify if there is wi-fi
    private boolean noWifi() {

        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        }
        boolean isWifiConn = false;
        if (networkInfo != null) {
            isWifiConn = networkInfo.isConnected();
        }
        Log.d(TAG, "Wifi connected: " + isWifiConn);

        return (!isWifiConn);
    }


}
