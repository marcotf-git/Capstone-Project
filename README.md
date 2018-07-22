# Capstone Project: Learning App

This project is the capstone project as part of the **Android Developer Nanodegree**, by **Udacity**. It allows users to create small lessons, with text, images or videos, store locally and on the cloud, and share the content with a group.

The program uses:

* **Firebase Authentication** for the login/logout process (https://firebase.google.com).

* **Firebase Realtime Database** for storing the text data on the cloud.

* **Firebase Storage** for storing the images or video files on the cloud.

* **Firebase Analytics** and **Crashlytics** for monitoring the app.

* **ExoPlayer** for playing the videos (https://github.com/google/ExoPlayer).

* **Picasso** library for handling image loading (<http://square.github.io/picasso/).

* **SQLite**  for the database engine (<https://www.sqlite.org/index.html>).

* **Java** programming language (http://www.oracle.com/technetwork/java/basicjava1-135508.html).

* **Android** operating system (https://developer.android.com/).


# Installation

* Download the all the files and subdirectories and import the project in the **Android Studio**.

* Create an account in the **Firebase** and a `project`, according to the instructions provided.

* Set the **Firebase Realtime Database** for the `project`, with the rules:

    {
      "rules": {
        	// Only authenticated users can read
    		".read": "auth != null",
        	"$uid":{
    	 		"$lesson": {
                    // For writing, make sure the uid of the requesting user
                    // matches path title of root
                    ".write": "auth.uid === $uid &&
                              // Consistency check of the 'user_uid' field
                              (!newData.child('user_uid').exists() ||
                               auth.uid === newData.child('user_uid').val())"
            	}
        	}
      }
    }


* Set the **Firebase Storage**, with the rules:

    service firebase.storage {
          match /b/{bucket}/o {
                // Forwriting, make sure the uid of the requesting user matches
                // path title of root
                match /{userId}/{allPaths=**} {
                  allow read: if request.auth != null;
                  allow write: if request.auth.uid == userId;
                }
          }
    }

* Download the `google-service.json` and install it in the `app` folder.

* In **Android Studio**, import the project and select to run your app. It will install the app in your connected device and starts it. See instructions on <https://developer.android.com/training/basics/firstapp/running-app.html>.


# Common usage

Well, do you like teaching something? And share with your friends?

Just create the content with your device resources. Use its camera, photos or videos, and make texts. The menu has the option to `create`, where you will see icons appear on the app bar, for editing and deleting, and a button on the bottom, for adding content.

After creating, you can upload to the cloud. There is an icon in the app bar for it. But before, you need to login (or create an account).

Then, go to the menu drawer in the top left, and you will see the options to login/logout, and the options to navigate to `My Lessons` (where you create) and `Group Lessons`.

If you select `My Lessons`, you will see only your lessons. Selecting `Group Lessons`, you will see only the group lessons, with your own lesson (if uploaded) added to the group (you are in the group).

Navigating to `Group Lessons`, you can download the lessons from the group content stored in the cloud. Just select the `refresh` icon (the circle).

So, if you create a lesson in the `My Lessons`, you can change to the `Group Lessons` and select the option to `refresh`, and you will download the lessons from the group, and see your own lesson there, together with all of your friends lessons! 😃

The app also has the `Smart Refresh` option, that will download on un-metered networks only, and in a more battery friendly way. It has the `Smart Upload` also! And all the process of uploading and downloading is registered in a `log`, that can be viewed while the tasks are being executed!

It is possible to delete the lesson locally, or from the cloud. If you delete locally, the lesson still exists on the cloud. Just go to the `My Lessons` folder, download the lesson from cloud, and choose the option to `Delete from Cloud`. You can choose to delete from cloud only. Just select the lesson (in your `My Lessons`), and select `Delete from Cloud`.

When you delete the lesson on `My Lessons` locally, will not delete the images, only the links to them (this was a design choice to protect the files). You can reuse these files in other lessons. However, the app doesn't download the images from the cloud, when the lesson in `My Lessons` is deleted, only restores the links to the images already saved on the device. If you have deleted the images manually on the device, they will not be recovered (by the app itself). 🚩

The option to insert data is for testing purposes. You can use it to test the app!  

That's it! 🎥


# Technical details

This app helps the user to create small lessons, with text and videos or images, using their own devices (phones or tablets), and sharing this content with a group of users.

Each user can create and save their content in the device, and upload it to the cloud. The group can download that content, and save it locally on the device, including the images or videos, for attending the classes.

The process requires login to the remote server, in case, the Firebase
(https://firebase.google.com), which handles all the login and logout, and stores the data.

The app has two modes: 'view' mode and 'create' mode.

If the user selects 'create' mode, it will show only the options to create, edit and delete, in the action items of the app bar.

If the user shows 'view' mode, it will show only option to read and sync the content with the remote server (download).

According to the modes of the app, the local database has two types of tables:
 - the tables "group_ ... " is a copy of the remote database.
 - the tables "my_ ... " is the content created by the user.

The database and its tables are handled by a content provider LessonsContentProvider.
The provider is queried by a cursor loader, which returns a cursor object.

In the 'view' mode, the view layout will be the activity_main.xml, which has containers for fragments. The fragment_main will have a RecyclerView, populated with a LinearLayoutManager and a custom adapter LessonsListAdapter. The cursor provided by the loader is passed to the adapter with the data that will be shown.

In the 'view' mode, if the user shows the option to sync the database, it will call a job
scheduled service (Intent Service), with the help of Firebase JobDispatcher, or call the service immediately, with an intent call to the Intent Service.

The result is informed via intent sent by a local broadcast, with messages that will be received by a Receiver in the MainActivity, and trigger, according to the content, messages informing the user about if the task has finished OK or with errors.

In case of the Scheduled Jobs, the user also have a notification, that will be received even if the app is closed, and has a pending intent incorporated, to open the app.

In the 'create' mode, if the user selects the option to add a lesson title, it will open another activity AddLessonActivity to add the title. If the user selects to delete and then click on an item, the item will be deleted after a confirmation. The confirmation is handled by a Dialog fragment. If the user selects to edit and then click, it will open an activity EditLessonActivity to edit that item.

There are specific rules in the app and in Firebase protecting the data of one user from another. The user only can upload or download data from the cloud when logged. The login process is handled by the Firebase Authentication.

The cloud data is divided between the Firebase Realtime Database (text), and the Firebase Storage (images and videos). The local data is divided between:
 - a table with the lesson data (text)
 - a table with the lesson parts data (text)
 - the local folders for file (blob) storage

The table with the lesson parts will store the URIs of the images or videos of that part.

The upload of data has the following algorithm:
  1) first, upload the images or videos to Storage
  2) get the URIs info and save in the local database, in the table of the lesson parts
  3) then, upload the two tables (lesson and parts) to the Firebase Database

The user can choose to download the group data (in the group table) or its own data (in the user table).

To download data from the cloud, the app has the algorithm:
  1) The clearing process:
  1.1) before the download, the local group data is cleared: the tables are deleted from a local database, and the files are deleted from local folder (/data/user/0/appUri/files/...)
  1.2) the clearing process does not delete all the data when downloading only the user lessons; it deletes only what is equal to that downloaded
  2) The download:
  2.1) download the tables from Firebase Database
  2.2) in case of group data:
  2.2.1) get the URIs of those tables, and with them, download the files from Storage
  2.2.2) write the file in the local folder, in the internal storage
  2.2.3) save the local URI in the table of the parts

When the app reads the data, when the user clicks on the lesson part, the ExoPlayer
(https://developer.android.com/guide/topics/media/exoplayer) or the Picasso
(http://square.github.io/picasso/) will get that file URI, and load the file from a local folder into the specific view.

To delete a **user** lesson, the app has the algorithm:
  a) it is possible to delete or locally, or in the cloud

  a.1) when deleting user lesson locally:
  1) only delete if there are no parts
  2) when deleting the lesson part row, store the reference of the cloud file URI Storage (reading it from the lesson parts table) in a specific table, which will be used to delete the file in the cloud, when the user chooses to do it in future
  3) the file is not deleted (the user is able to delete manually because the file is an image/video taken on the device, stays in the user folders)

In the **group** lessons folder, the text and the files are deleted from the device, but not from the cloud.  

When the user chooses to delete the lesson from the cloud, the app will also delete the files in the Storage, but not in the device.

  a.2) when deleting user lesson only in the cloud:
  1) delete from cloud Database and save the URIs of the files in a specific local table, that will store the files for deletion from Storage
  2) query that table, and with the information, delete from the Storage

To edit the local data, the user can edit only their tables:
  1) choose what to edit and edit (click on the item and on the 'edit' icon)
  2) choose to pick another image (the 'Fab' button)
  2.1) the old image, if it has cloud URI, will be deleted but its reference will be saved in that specific table, for future deletion from Storage
  2.2) the new one (or the new video) take the place

So, the app implements `CRUD` on local user data, and `read` (download), `write` (upload) and `delete` in user cloud data, managing the deletion from Firebase Storage for when deleting the whole lesson from Firebase Database.

And the app implements only `read` from the cloud, in case of group data. In the group there will be also the user lessons, but saved in the group table.

The app menu is contextual: the options change according to the user actions.

Finally, the cloud communication is saved in a log table, updated at the same time as the
services are being executed. The log can be viewed by an option in the drawer menu.

*Marcos Tewfiq (marcotf.dev@gmail.com)*


**This app is for learning purposes.** 📚



# Credits

These are some useful links, in addition to **Udacity** itself, that were queried in this project:

https://developer.android.com/guide/components/activities

https://developer.android.com/guide/components/activities/process-lifecycle

https://developer.android.com/topic/libraries/architecture/lifecycle

https://developer.android.com/guide/components/fragments

https://developer.android.com/training/implementing-navigation/nav-drawer

https://developer.android.com/guide/topics/providers/document-provider

https://developer.android.com/training/basics/network-ops/managing

https://developer.android.com/studio/build/multidex

https://developer.android.com/studio/publish/app-signing

https://google.github.io/ExoPlayer/guide.html

https://material.io/develop/android/components/bottom-app-bar/

https://material.io/develop/android/components/collapsing-toolbar-layout/

https://firebase.google.com/docs/

https://firebase.google.com/docs/database/video-series/

https://github.com/VisualGhost/Firebase-Storage/wiki/Handle-Activity-Lifecycle-Changes

https://www.sqlite.org/foreignkeys.html

https://www.sqlite.org/lang_createtable.html

https://guides.codepath.com/android/local-databases-with-sqliteopenhelper

https://guides.codepath.com/android/Starting-Background-Services#overview

https://guides.codepath.com/android/Starting-Background-Services#communicating-with-a-broadcastreceiver

https://guides.codepath.com/android/Creating-and-Executing-Async-Tasks#executing-the-asynctask

https://guides.codepath.com/android/Displaying-the-Snackbar

https://guides.codepath.com/android/Working-with-the-EditText

https://guides.codepath.com/android/using-the-recyclerview

https://medium.com/google-developers/building-a-documentsprovider-f7f2fb38e86a

https://medium.com/@hitherejoe/exploring-the-android-design-support-library-bottom-navigation-drawer-548de699e8e0

https://medium.com/fungjai/playing-video-by-exoplayer-b97903be0b33

https://github.com/codepath/android_guides/wiki/Using-an-ArrayAdapter-with-ListView

https://stackoverflow.com/questions/2453989/how-to-get-string-array-from-arrays-xml-file

https://stackoverflow.com/questions/34973456/how-to-change-text-of-a-textview-in-navigation-drawer-header

https://stackoverflow.com/questions/49153215/failed-to-find-style-cardview-style-in-current-theme

https://stackoverflow.com/questions/41267446/how-to-get-loadermanager-initloader-working-within-a-fragment

https://stackoverflow.com/questions/6374170/how-to-set-a-fragment-tag-by-code

https://stackoverflow.com/questions/3014089/maintain-save-restore-scroll-position-when-returning-to-a-listview?noredirect=1&lq=1

https://stackoverflow.com/questions/49292487/failed-to-find-style-coordinatorlayoutstyle-in-current-theme

https://stackoverflow.com/questions/27390682/highlight-selected-item-inside-a-recyclerview

https://stackoverflow.com/questions/46916992/having-trouble-implementing-action-open-document-to-my-project

https://stackoverflow.com/questions/25414352/how-to-persist-permission-in-android-api-19-kitkat

https://stackoverflow.com/questions/11229219/android-get-application-name-not-package-name

https://stackoverflow.com/questions/33608746/in-android-using-exoplayer-how-to-fill-surfaceview-with-a-video-that-does-not

https://stackoverflow.com/questions/30729312/how-to-dismiss-a-snackbar-using-its-own-action-button

https://stackoverflow.com/questions/16237950/android-check-if-file-exists-without-creating-a-new-one

https://stackoverflow.com/questions/14002022/android-sqlite-closed-exception/25379071#25379071

https://blog.stylingandroid.com/recyclerview-animations-add-remove-items/

https://www.youtube.com/watch?v=GNGzmf_UcvY

https://www.youtube.com/watch?v=imsr8NrIAMs

https://github.com/OneSignal/OneSignal-Android-SDK/issues/281

https://github.com/evernote/android-job/issues/344
