package com.example.androidstudio.capstoneproject.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.test.espresso.IdlingResource;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
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
import com.example.androidstudio.capstoneproject.data.TestUtil;
import com.example.androidstudio.capstoneproject.utilities.MyFirebaseUtilities;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.Arrays;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;


/**
 * The app has two modes: 'view' mode and 'create' mode.
 * If the user select 'create' mode, it will show only the content created by the user,
 * plus the options to create, edit and delete, in the action items of the app bar.
 * If the user shows 'view' mode, it will show the content that is synced with an remote server.
 * This class will start showing the lessons' titles queried from a local database, which is a copy
 * of the remote database. This corresponds to the 'view' mode, which can be selected in the
 * overflow menu.
 * According to the modes of the app, the local database has two tables.
 * The table "group_content" is a copy of the remote database.
 * The table "my_content" is the content created by the user.
 * The database and its tables are handled by a content provider LessonsContentProvider.
 * The provider is queried by a cursor loader, which returns a cursor object.
 * In the 'view' mode, the view layout will be the activity_main.xml, which has a RecyclerView,
 * populated with a GridLayoutManager and a custom adapter LessonsListAdapter.
 * The cursor provided by the loader is passed to the adapter with the data that will be shown.
 * In the 'view' mode, if the user shows the option to sync the database, it will call a task ....
 * In the 'create' mode, if the user selects the option to add a lesson title, it will open another
 * activity AddLessonActivity to add the title. If the user select to delete and then click on an item,
 * the item will be deleted after a confirmation. The confirmation is handled by a fragment.
 * If the user select to edit and then click, it will open an activity EditLessonActivity to edit that item.
 *
 *
 *
 *
 */
public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        MainFragment.OnLessonListener,
        MainFragment.OnIdlingResourceListener,
        PartsFragment.OnLessonPartListener,
        PartsFragment.OnIdlingResourceListener,
        DeleteLessonDialogFragment.DeleteLessonDialogListener,
        DeletePartDialogFragment.DeletePartDialogListener,
        MyFirebaseUtilities.OnCloudListener,
        DeleteLessonCloudDialogFragment.DeleteLessonCloudDialogListener {


    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String ANONYMOUS = "anonymous";

    // Firebase auth: choose an arbitrary request code value
    private static final int RC_SIGN_IN = 1;

    // Final string to store state information
    private static final String CLICKED_LESSON_ID = "clickedLessonId";
    private static final String SELECTED_LESSON_ID = "selectedLessonId";
    private static final String CLICKED_LESSON_PART_ID = "clickedLessonPartId";
    private static final String SELECTED_LESSON_PART_ID = "selectedLessonPartId";

    // App state information variables
    private static long clickedLesson_id;
    private static long selectedLesson_id;
    private static long clickedLessonPart_id;
    private static long selectedLessonPart_id;
    private static int mainVisibility;
    private static int partsVisibility;
    //private static boolean flag_preferences_updates = false;

    // User data variables
    private String mUsername;
    private String mUserEmail;
    // The user's ID, unique to the Firebase project.
    private String mUserUid;

    // Firebase instance variables
    private FirebaseFirestore mFirestoreDatabase;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    // Menus and buttons
    private Menu mMenu;
    private Toolbar mToolbar;
    private ActionBar actionBar;
    private FloatingActionButton mButton;

    // Drawer menu variables
    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;

    private Context mContext;

    // Views
    private FrameLayout lessonsContainer;
    private FrameLayout partsContainer;
    private TextView mUsernameTextView;
    private TextView mUserEmailTextView;

    // Fragments
    private MainFragment mainFragment;
    private PartsFragment partsFragment;

    // The Idling Resource which will be null in production.
    @Nullable
    private SimpleIdlingResource mIdlingResource;


    /**
     * Only called from test, creates and returns a new {@link SimpleIdlingResource}.
     */
    @VisibleForTesting
    @NonNull
    public IdlingResource getIdlingResource() {
        if (mIdlingResource == null) {
            mIdlingResource = new SimpleIdlingResource();
        }
        return mIdlingResource;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        mUsername = ANONYMOUS;
        mUserEmail = "";

        // Add the toolbar as the default app bar
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        // Get a support ActionBar corresponding to this toolbar
        actionBar = getSupportActionBar();
        if (null != actionBar) {
            // Enable the Up button (icon will be set in onPrepareMenu
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

        // Initialize the state vars
        if (null == savedInstanceState) {
            clickedLesson_id = -1;
            selectedLesson_id = -1;
            clickedLessonPart_id = -1;
            selectedLessonPart_id = -1;
            // Phone visibility
            mainVisibility = VISIBLE;
            partsVisibility = GONE;
        }

        // Initialize the fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        // Only create fragment when needed
        if (null == savedInstanceState) {

            Log.v(TAG, "creating MainFragment");
            mainFragment = new MainFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.lessons_container, mainFragment, "MainFragment")
                    .commit();

            Log.v(TAG, "creating PartsFragment");
            partsFragment = new PartsFragment();
            fragmentManager.beginTransaction()
                    .replace(R.id.parts_container, partsFragment, "PartsFragment")
                    .commit();

        } else {
            mainFragment = (MainFragment) fragmentManager.findFragmentByTag("MainFragment");
            partsFragment = (PartsFragment) fragmentManager.findFragmentByTag("PartsFragment");
        }

        // Set the fragment views visibility
        lessonsContainer.setVisibility(mainVisibility);
        partsContainer.setVisibility(partsVisibility);

        Log.d(TAG, "lessonsContainer visibility:" + lessonsContainer.getVisibility());
        Log.d(TAG, "partsContainer visibility:" + partsContainer.getVisibility());

        // Get the IdlingResource instance for testing
        getIdlingResource();

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
        mFirestoreDatabase = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mFirestoreDatabase.setFirestoreSettings(settings);

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
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(MenuItem menuItem) {
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
                        default:
                            break;
                    }
                    return true;
                }
            });

        // Set the username int the drawer
        View headerView = navigationView.getHeaderView(0);
        mUsernameTextView = (TextView) headerView.findViewById(R.id.tv_user_name);
        mUserEmailTextView = (TextView) headerView.findViewById(R.id.tv_user_email);
        mUsernameTextView.setText(mUsername);
        mUserEmailTextView.setText(mUserEmail);
    }


    // Helper method for Firebase login
    private void login() {
        Log.v(TAG, "login");
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

        // Set the drawer
        mUsernameTextView.setText(mUsername);
        mUserEmailTextView.setText(mUserEmail);

        // Set the visibility of the upload action icon
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String queryOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));
        if (null != mMenu) {
            if (queryOption.equals(this.getString(R.string.pref_mode_create))) {
                mMenu.findItem(R.id.action_upload).setVisible(true);
            } else {
                mMenu.findItem(R.id.action_upload).setVisible(false);
            }
        }

        Log.v(TAG, "onSignedInInitialize mUsername:" + mUsername);
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
        Log.v(TAG, "logout");
        if (mUsername.equals(ANONYMOUS)) {
            Snackbar.make(lessonsContainer, "You are not signed!",
                    Snackbar.LENGTH_LONG).show();
        }
        AuthUI.getInstance().signOut(this);
    }

    // Method for sign out for the listener
    private void onSignedOutCleanup(){
        Log.v(TAG, "onSignedOutCleanup");
        mUsername = ANONYMOUS;
        mUserEmail = "";
        mUserUid = "";
        mUsernameTextView.setText(mUsername);
        mUserEmailTextView.setText(mUserEmail);
        // Set the visibility of the upload action icon
        if (null != mMenu) {
            mMenu.findItem(R.id.action_upload).setVisible(false);
        }
    }

    // This method is saving the visibility of the fragments in static vars
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        mainVisibility = lessonsContainer.getVisibility();
        partsVisibility = partsContainer.getVisibility();
        super.onSaveInstanceState(savedInstanceState);
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
        //mUserNameText.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove listener from PreferenceManager
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    // Inflate the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * This method sets the option menu that choose which kind of movie search will be executed,
     * if popular or top rated
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Save a reference to the menu
        mMenu = menu;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String queryOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));

        if (queryOption.equals(this.getString(R.string.pref_mode_view))) {
            menu.findItem(R.id.select_view).setChecked(true);
            // Prepare the visibility of the creation action items
            mMenu.findItem(R.id.action_delete_from_cloud).setVisible(false);
            mMenu.findItem(R.id.action_delete).setVisible(false);
            mMenu.findItem(R.id.action_edit).setVisible(false);
            mMenu.findItem(R.id.action_upload).setVisible(false);
            mMenu.findItem(R.id.action_refresh).setVisible(true);
            mButton.setVisibility(GONE);
        }

        if (queryOption.equals(this.getString(R.string.pref_mode_create))) {
            menu.findItem(R.id.select_create).setChecked(true);
            // Prepare the visibility of the creation action items
            mMenu.findItem(R.id.action_delete).setVisible(true);
            if (!mUsername.equals(ANONYMOUS)) {
                mMenu.findItem(R.id.action_delete_from_cloud).setVisible(true);
                mMenu.findItem(R.id.action_upload).setVisible(true);
            } else {
                mMenu.findItem(R.id.action_delete_from_cloud).setVisible(false);
                mMenu.findItem(R.id.action_upload).setVisible(false);
            }
            mMenu.findItem(R.id.action_refresh).setVisible(true);
            mButton.setVisibility(VISIBLE);
        }

        // Set the drawer menu icon according to views
        if (mainVisibility == VISIBLE) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemThatWasClickedId = item.getItemId();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        MyFirebaseUtilities myFirebase;

        switch (itemThatWasClickedId) {

            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected mainVisibility:" + mainVisibility);
                // Set the action according to the views visibility
                if (mainVisibility == VISIBLE) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                    return true;
                } else {
                    closePartsFragment();
                }
                break;

            case R.id.select_view:
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_view)).apply();
                // Set visibility of action icons
                mMenu.findItem(R.id.action_delete).setVisible(false);
                mMenu.findItem(R.id.action_delete_from_cloud).setVisible(false);
                mMenu.findItem(R.id.action_edit).setVisible(false);
                mMenu.findItem(R.id.action_upload).setVisible(false);
                mMenu.findItem(R.id.action_refresh).setVisible(true);
                mButton.setVisibility(GONE);
                // Deselect the last view selected
                mainFragment.deselectViews();
                partsFragment.deselectViews();
                selectedLesson_id = -1;
                selectedLessonPart_id = -1;
                Log.d(TAG, "View mode selected");
                break;

            case R.id.select_create:
                sharedPreferences.edit()
                        .putString(this.getString(R.string.pref_mode_key),
                                this.getString(R.string.pref_mode_create)).apply();
                // Set visibility of action icons
                mMenu.findItem(R.id.action_delete).setVisible(true);
                mMenu.findItem(R.id.action_edit).setVisible(true);
                if (!mUsername.equals(ANONYMOUS)) {
                    mMenu.findItem(R.id.action_delete_from_cloud).setVisible(true);
                    mMenu.findItem(R.id.action_upload).setVisible(true);
                } else {
                    mMenu.findItem(R.id.action_delete_from_cloud).setVisible(false);
                    mMenu.findItem(R.id.action_upload).setVisible(false);
                }
                mMenu.findItem(R.id.action_refresh).setVisible(true);
                mButton.setVisibility(VISIBLE);
                Log.v(TAG, "Create mode selected");
                break;

            case R.id.action_refresh:
                myFirebase = new MyFirebaseUtilities(this, mFirestoreDatabase, mUserUid);
                myFirebase.refreshDatabase();
                deselectViews();
                break;

            case R.id.action_edit:
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
                break;

            case R.id.action_upload:
                if (selectedLesson_id != -1) {
                    myFirebase = new MyFirebaseUtilities(this, mFirestoreDatabase, mUserUid);
                    myFirebase.uploadDatabase(selectedLesson_id);
                } else {
                    Toast.makeText(this,
                            "Please, select a lesson to upload!", Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.action_delete:
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
                break;

            case R.id.action_delete_from_cloud:
                Log.d(TAG, "Delete from Cloud action selected");
                // Try to action first on the more specific item
                if  (selectedLesson_id != -1) {
                    deleteLessonFromCloud(selectedLesson_id);
                } else {
                    Toast.makeText(this,
                            "Please, select a lesson to delete from Cloud!", Toast.LENGTH_LONG).show();
                }
                break;

            case R.id.action_insert_fake_data:
                Log.d(TAG, "Insert fake data action selected");
                TestUtil.insertFakeData(this);
                Toast.makeText(this,
                        "Fake data inserted!", Toast.LENGTH_LONG).show();
                break;

            case R.id.action_cancel:
               break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        String lessonsQueryOption = sharedPreferences.getString(this.getString(R.string.pref_mode_key),
                this.getString(R.string.pref_mode_view));
        Log.d(TAG, "onSharedPreferenceChanged lessonsQueryOption:" + lessonsQueryOption);
    }
    
    private void deselectViews() {
        // Deselect the last view selected
        mainFragment.deselectViews();
        selectedLesson_id = -1;
        partsFragment.deselectViews();
        selectedLessonPart_id = -1;
    }

    // Helper function to delete lesson data and update the view
    private void deleteLesson(long _id) {
        Log.v(TAG, "deleteLesson _id:" + _id);
        // Call the fragment for showing the delete dialog
        DialogFragment deleteLessonFragment = new DeleteLessonDialogFragment();
        // Pass the _id of the lesson
        Bundle bundle = new Bundle();
        bundle.putLong("_id", _id);
        deleteLessonFragment.setArguments(bundle);
         // Show the dialog box
        deleteLessonFragment.show(getSupportFragmentManager(), "DeleteLessonDialogFragment");
    }

    // Helper function to delete lesson data from cloud (delete the lesson document)
    private void deleteLessonFromCloud(long _id) {
        Log.v(TAG, "deleteLessonFromCloud _id:" + _id);
        // Call the fragment for showing the delete dialog
        DialogFragment deleteLessonCloudFragment = new DeleteLessonCloudDialogFragment();
        // Pass the _id of the lesson
        Bundle bundle = new Bundle();
        bundle.putLong("_id", _id);
        deleteLessonCloudFragment.setArguments(bundle);
        // Show the dialog box
        deleteLessonCloudFragment.show(getSupportFragmentManager(), "DeleteLessonCloudDialogFragment");
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
        Log.v(TAG, "deleteLessonPart _id:" + _id);
        // Call the fragment for showing the delete dialog
        DialogFragment deleteLessonPartFragment = new DeletePartDialogFragment();
        // Pass the _id of the lesson
        Bundle bundle = new Bundle();
        bundle.putLong("_id", _id);
        deleteLessonPartFragment.setArguments(bundle);
        // Show the dialog box
        deleteLessonPartFragment.show(getSupportFragmentManager(), "DeletePartDialogFragment");
    }

    // Helper function to edit lesson part
    private void editLessonPart(long _id) {
        Log.d(TAG, "editLessonPart _id:" + _id);
        // Create a new intent to start an EditTaskActivity
        Class destinationActivity = EditPartActivity.class;
        Intent editLessonPartIntent = new Intent(mContext, destinationActivity);
        editLessonPartIntent.putExtra(SELECTED_LESSON_PART_ID, _id);
        startActivity(editLessonPartIntent);
        // Deselect the last view selected
        partsFragment.deselectViews();
        selectedLessonPart_id = -1;
    }

    // Helper method for hiding the PartsFragment
    public void closePartsFragment() {
        Log.d(TAG, "closePartsFragment");
        // deselect the views on the fragment that will be closed
        partsFragment.deselectViews();
        // clear the reference var
        selectedLessonPart_id = -1;
        // Change the views
        partsContainer.setVisibility(GONE);
        partsVisibility = GONE;
        lessonsContainer.setVisibility(VISIBLE);
        mainVisibility = VISIBLE;
        // Set the drawer menu icon according to views
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
    }

    // Method for receiving communication from the MainFragment
    @Override
    public void onLessonSelected(long _id) {
        selectedLesson_id = _id;
    }

    // Method for receiving communication from the MainFragment
    @Override
    public void onLessonClicked(long _id) {
        Log.d(TAG, "onLessonClicked _id:" + _id);
        clickedLesson_id = _id;
        // Show the lesson parts fragment
        lessonsContainer.setVisibility(GONE);
        mainVisibility = GONE;
        partsContainer.setVisibility(VISIBLE);
        partsVisibility = VISIBLE;
        // Set the drawer menu icon according to views
        if (mainVisibility == VISIBLE) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        } else {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
        }
        // Send the data to the fragment
        partsFragment.setReferenceLesson(_id);
    }

    // Method for receiving communication from the DeleteLessonFragment
    @Override
    public void onDialogPositiveClick(DialogFragment dialog, long _id) {
        ContentResolver contentResolver = mContext.getContentResolver();
        /* The delete method deletes the row by its _id */
        Uri uriToDelete = LessonsContract.MyLessonsEntry.CONTENT_URI.buildUpon()
                .appendPath("" + _id + "").build();
        Log.d(TAG, "onDialogPositiveClick: Uri to delete:" + uriToDelete.toString());
        int numberOfLessonsDeleted = contentResolver.delete(uriToDelete, null, null);
        if (numberOfLessonsDeleted > 0) {
            Toast.makeText(this,
                    numberOfLessonsDeleted + " item(s) removed!", Toast.LENGTH_LONG).show();

//            Snackbar mySnackbar = Snackbar.make(lessonsContainer,
//                    numberOfLessonsDeleted + " item removed", Snackbar.LENGTH_LONG);
//            mySnackbar.show();

            // Deselect the last view selected
            mainFragment.deselectViews();
            selectedLesson_id = -1;
        }
    }

    // Method for receiving communication from the DeleteLessonFragment
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Toast.makeText(mContext,
                "Canceled!", Toast.LENGTH_LONG).show();
    }

    // Receive from the MainFragment and PartsFragment the order to setting the idling resource
    @Override
    public void onIdlingResource(Boolean value) {
        if (mIdlingResource != null) {
            mIdlingResource.setIdleState(value);
        }
    }

    // Receive communication from the PartsFragment
    @Override
    public void onPartSelected(long _id) {
        selectedLessonPart_id = _id;
    }

    // Receive communication from the PartsFragment
    @Override
    public void onPartClicked(long _id) {
        clickedLessonPart_id = _id;
    }

    // Receive communication form DeleteDialogPartFragment
    @Override
    public void onDialogPartPositiveClick(DialogFragment dialog, long _id) {
        ContentResolver contentResolver = mContext.getContentResolver();
        /* The delete method deletes the row by its _id */
        Uri uriToDelete = LessonsContract.MyLessonPartsEntry.CONTENT_URI.buildUpon()
                .appendPath("" + _id + "").build();
        Log.d(TAG, "onDialogPartPositiveClick: Uri to delete:" + uriToDelete.toString());
        int numberOfPartsDeleted = contentResolver.delete(uriToDelete, null, null);
        if (numberOfPartsDeleted > 0) {
            Toast.makeText(this,
                    numberOfPartsDeleted + " item(s) removed!", Toast.LENGTH_LONG).show();
            // Deselect the last view selected
            partsFragment.deselectViews();
            selectedLessonPart_id = -1;
        }
    }

    // Receive communication form DeleteDialogPartFragment
    @Override
    public void onDialogPartNegativeClick(DialogFragment dialog) {
        Toast.makeText(mContext,
                "Canceled!", Toast.LENGTH_LONG).show();
    }


    // Receive communication form MyFirebaseUtilities instance
    @Override
    public void onUploadSuccess() {
        Toast.makeText(mContext,
                "Lesson uploaded!", Toast.LENGTH_LONG).show();
        // Deselect the last view selected
        mainFragment.deselectViews();
        selectedLesson_id = -1;
    }

    @Override
    public void onUploadFailure(@NonNull Exception e) {
        Toast.makeText(mContext,
                "Error on uploading:" + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDownloadComplete() {
        Toast.makeText(mContext,
                "Download completed. Updating the local database...", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDownloadFailure(@NonNull Exception e) {
        Toast.makeText(mContext,
                "Error on downloading:" + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDeletedSuccess() {
        Toast.makeText(mContext,
                "Lesson deleted from Cloud!", Toast.LENGTH_LONG).show();
        // Deselect the last view selected
        mainFragment.deselectViews();
        selectedLesson_id = -1;
    }

    @Override
    public void onDeleteFailure(@NonNull Exception e) {
        Toast.makeText(mContext,
                "Error on deleting from Cloud:" + e.getMessage(), Toast.LENGTH_LONG).show();
    }

    // receive communication from DeleteLessonCloudDialogFragment instance
    @Override
    public void onDialogDeleteCloudPositiveClick(DialogFragment dialog, long lesson_id) {
        MyFirebaseUtilities myFirebase;
        myFirebase = new MyFirebaseUtilities(this, mFirestoreDatabase, mUserUid);
        myFirebase.deleteLessonFromCloud(selectedLesson_id);
    }

    @Override
    public void onDialogDeleteCloudNegativeClick(DialogFragment dialog) {
        Toast.makeText(mContext,
                "Canceled!", Toast.LENGTH_LONG).show();
    }

}
