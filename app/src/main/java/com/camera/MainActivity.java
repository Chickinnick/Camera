package com.camera;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.LogLevel;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public static final String ACCEES_TOKEN_KEY = "aseesToken";
    public static final String POSITION = "position";
    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 300;

    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    public static final int COUNT_OF_STANDART_FOLDERS = 7;
    private String newName;
    private static final String LOG_TAG = "MainActivity";
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
    private Uri fileUri;
    private String SCAN_PATH;
    private static String directoryName;
    CallbackManager mCallbackManager;
    public DropboxAPI<AndroidAuthSession> mDBApi;
    final static private String APP_KEY = "z4s4zns2oxclk4m";
    final static private String APP_SECRET = "wbsx4ngkmdbxwdj";
    private static ArrayList<String> labels;
    GoogleApiClient mGoogleApiClient;
    GridAdapter adapter;


    private Locale myLocale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GridView gridview = (GridView) findViewById(R.id.gridview);
        Hawk.initWithoutEncryption(this, LogLevel.NONE);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
        final AndroidAuthSession session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        ImageButton langBtn = (ImageButton) findViewById(R.id.imbtn_lang);


        langBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.change_lang);
                builder.setItems(R.array.langs, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String lang = "en";
                        switch (which) {
                            case 0:
                                lang = "en";
                                break;
                            case 1:
                                lang = "ru";
                                break;
                            case 2:
                                lang = "uk";
                                break;
                            default:
                                break;
                        }
                        changeLang(lang);

                        Intent intent = getIntent();
                        finish();
                        startActivity(intent);
                    }
                });
                builder.show();



            }
        });



        labels = new ArrayList<>();

        labels.add(getString(R.string.camera_title_standart));
        labels.add(getString(R.string.camera_title_notebook));
        labels.add(getString(R.string.camera_title_selfi));
        labels.add(getString(R.string.camera_title_scanner));
        labels.add(getString(R.string.camera_title_family));
        labels.add(getString(R.string.camera_title_job));
        labels.add(getString(R.string.camera_title_hobbi));
        labels.add(getString(R.string.camera_title_add_new));

        labels = Hawk.get("labels", labels);

        adapter = new GridAdapter(this, labels);
        gridview.setAdapter(adapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                Log.i("ON ITEM CLICK", "position:" + position + "id: " + id);


                if (position == adapter.getCount() - 1) {
                    createNewFolder(position, adapter);
                } else {
                    if (SaveFileUtils.isExternalStorageWritable()) {
                        carpute(position);
                    } else {
                        noSdCardDialog();
                    }

                }

            }
        });

        gridview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, final View view, final int position, long id) {
                CharSequence[] shareModes = {"Facebook", "DropBox", "OneDrive", "GoogleDrive"};

                Log.i("ON ITEM LONG CLICK", "position:" + position + "id: " + id);


                final View dialogView = View.inflate(MainActivity.this, R.layout.options_dialog, null);


                Button renameBtn = (Button) dialogView.findViewById(R.id.btn_rename);
                Button removeBtn = (Button) dialogView.findViewById(R.id.btn_remove);
                Button openBtn1 = (Button) dialogView.findViewById(R.id.btn_open);


                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(getString(R.string.title_options));

                Button openBtn = new Button(MainActivity.this);
                openBtn.setText(R.string.open);
                builder.setView(openBtn);

                if(position >= COUNT_OF_STANDART_FOLDERS){
                builder.setView(dialogView);}
                builder.setItems(shareModes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                mCallbackManager = CallbackManager.Factory.create();
                                if (AccessToken.getCurrentAccessToken() == null) {
                                    FacebookApi.LoginToFacebook(MainActivity.this);
                                } else {
                                    Log.v(LOG_TAG, "Залогирован");
                                    String s = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + labels.get(position);
                                    List<File> allPictures = SaveFileUtils.getAllFiles(s);
                                    Log.v(LOG_TAG, " allPictures " + allPictures);
                                    FacebookApi.postToFacebook(allPictures);
                                }
                                break;
                            case 1:
                                String accessToken = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(ACCEES_TOKEN_KEY, "");
                                if (accessToken.equals("")) {
                                    PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit()
                                            .putInt(POSITION, position).commit();
                                    mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
                                } else {
                                    if (!session.isLinked()) {
                                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit()
                                                .putInt(POSITION, position).commit();
                                        mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
                                    } else {
                                        Log.v(LOG_TAG, "Drop Box залогирован");
                                        DropBoxApi.postToDropBox(MainActivity.this, labels, position);
                                    }
                                }
                                break;
                            case 2:
                                mGoogleApiClient.connect();
                                break;
                            case 3:

                                break;

                        }

                    }
                });

                View.OnClickListener onButtonClickOpen =  new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String nameOfFolder = labels.get(position);

                        openFolder(Uri.parse(String.valueOf(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES) + "/" + nameOfFolder)));
                    }
                };

                renameBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        renameFolder(position, adapter);
                    }
                });

                removeBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeFolder(position, adapter);

                    }
                });
                openBtn.setOnClickListener(onButtonClickOpen);
                openBtn1.setOnClickListener(onButtonClickOpen);


                builder.show();
                return true;
            }
        });
    }

    private void noSdCardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(MainActivity.this.getString(R.string.no_sd_card_error_massage))
                .setTitle(MainActivity.this.getString(R.string.no_sd_card_error_massage_title))
                .setPositiveButton(MainActivity.this.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id1) {
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void carpute(final int position) {
        CharSequence[] cameraModes = {MainActivity.this.getString(R.string.camera_mode_photo), MainActivity.this.getString(R.string.camera_mode_videa)};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(MainActivity.this.getString(R.string.Camera_mode));
        builder.setItems(cameraModes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                directoryName = labels.get(position);
                switch (which) {
                    case 0:

                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE );
                        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
                        if (position != 0) {

                            fileUri = SaveFileUtils.getOutputMediaFileUri(SaveFileUtils.MEDIA_TYPE_IMAGE, directoryName); // create a file to save the image
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
                            // start the image capture Intent
                        }
                        MainActivity.this.startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                        break;
                    case 1:
                        Intent intent2 = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        if (position != 0) {
                            fileUri = SaveFileUtils.getOutputMediaFileUri(SaveFileUtils.MEDIA_TYPE_VIDEO, directoryName);  // create a file to save the video
                            intent2.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);  // set the image file name
                        }
                        intent2.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
                        // start the Video Capture Intent
                        MainActivity.this.startActivityForResult(intent2, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
                        break;
                }

            }
        });
        builder.show();
    }

    private void createNewFolder(final int position, final GridAdapter adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(MainActivity.this.getString(R.string.add_new_folder_title));
        final EditText input = new EditText(MainActivity.this);
        input.setHint(MainActivity.this.getString(R.string.enter_name_of_new_folder));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                newName = input.getText().toString();
                if (!"".equals(newName)) {
                    labels.set(position, newName);
                    labels.add(getString(R.string.add_new_camera));
                    adapter.notifyDataSetChanged();
                    Hawk.put("labels", labels);
                }
            }
        });
        builder.show();
    }


    @Override
    protected void onStart() {
        super.onStart();

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mCallbackManager != null)
            mCallbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Image captured and saved to fileUri specified in the Intent

                Log.v(LOG_TAG, "directoryName " + directoryName);
                SCAN_PATH = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES) + "/" + directoryName;
                if (data != null) {
                    openFolder(Uri.parse(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM) + "/Camera"));
                } else {
                    openFolder(Uri.parse(SCAN_PATH));
                }

            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture
            } else {
                // Image capture failed, advise user
            }
        }

        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Video captured and saved to fileUri specified in the Intent
                Log.v(LOG_TAG, "directoryName " + directoryName);
                SCAN_PATH = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_MOVIES) + "/" + directoryName;
                if (data != null) {
                    openFolder(Uri.parse(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DCIM) + "/Camera"));
                } else {
                    openFolder(Uri.parse(SCAN_PATH));
                }
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the video capture
            } else {
                // Video capture failed, advise user
            }
        }

        if (requestCode == RESOLVE_CONNECTION_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            }
        }

        if (requestCode == REQUEST_CODE_CREATOR) {
            // Called after a file is saved to Drive.
            if (resultCode == RESULT_OK) {
                Log.i(LOG_TAG, "Image successfully saved.");
            }
        }

    }

    protected void onResume() {
        super.onResume();
        AndroidAuthSession session = mDBApi.getSession();
        if (session.authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                session.finishAuthentication();
                String accessToken = mDBApi.getSession().getOAuth2AccessToken();
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString(ACCEES_TOKEN_KEY, accessToken).commit();
                int pos = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getInt(POSITION, 0);
                Log.v(LOG_TAG, "pos = " + pos);
                DropBoxApi.postToDropBox(this, labels, pos);
            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
            }
        }
    }

    public void openFolder(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "resource/folder");
        startActivity(Intent.createChooser(intent, "Open folder"));
    }


    private void renameFolder(final int position, final GridAdapter adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(MainActivity.this.getString(R.string.rename_folder));
        final EditText input = new EditText(MainActivity.this);
        input.setHint(MainActivity.this.getString(R.string.enter_name));
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                newName = input.getText().toString();
                if (!"".equals(newName)) {
                    String oldDirName = labels.get(position);

                    labels.set(position, newName);
                    adapter.notifyDataSetChanged();
                    Hawk.put("labels", labels);


                    File mediaStorageDir = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES)));
                    //List<File> currentDirs = SaveFileUtils.getAllDirectiries(mediaStorageDir);

                    for (File dir : mediaStorageDir.listFiles()) {
                        if (dir.getName().equals(oldDirName)) {
                            dir.renameTo(new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES), newName));
                        }
                    }

                    mediaStorageDir = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_MOVIES)));


                    for (File dir : mediaStorageDir.listFiles()) {
                        if (dir.getName().equals(oldDirName)) {
                            dir.renameTo(new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_MOVIES), newName));
                        }
                    }


                }
            }
        });
        builder.show();
    }


    private void removeFolder(final int position, final GridAdapter adapter) {


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(MainActivity.this.getString(R.string.delete_folder));
        builder.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String oldDirName = labels.get(position);
                        labels.remove(position);
                        adapter.notifyDataSetChanged();
                        Hawk.put("labels", labels);

                        File mediaStorageDir = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES)));

                        for (File dir : mediaStorageDir.listFiles()) {
                            if (dir.getName().equals(oldDirName)) {

                                try {
                                    FileUtils.deleteDirectory(dir);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            }
                        }

                        mediaStorageDir = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_MOVIES)));


                        for (File dir : mediaStorageDir.listFiles()) {
                            if (dir.getName().equals(oldDirName)) {
                                try {
                                    FileUtils.deleteDirectory(dir);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }


                        dialog.dismiss();


                    }
                }

        );

        builder.setNegativeButton(R.string.CANCEL, new DialogInterface.OnClickListener()

                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }

        );


        builder.show();
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }


//    private void saveFileToDrive(File file) {
//        // Start by creating a new contents, and setting a callback.
//        Log.i(LOG_TAG, "Creating new contents.");

//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//
//        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
//        selected_photo.setImageBitmap(bitmap);
//
//        final Bitmap image = mBitmapToSave;
//        Drive.DriveApi.newDriveContents(mGoogleApiClient)
//                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
//
//                    @Override
//                    public void onResult(DriveApi.DriveContentsResult result) {
//                        // If the operation was not successful, we cannot do anything
//                        // and must
//                        // fail.
//                        if (!result.getStatus().isSuccess()) {
//                            Log.i(LOG_TAG, "Failed to create new contents.");
//                            return;
//                        }
//                        // Otherwise, we can write our data to the new contents.
//                        Log.i(LOG_TAG, "New contents created.");
//                        // Get an output stream for the contents.
//                        OutputStream outputStream = result.getDriveContents().getOutputStream();
//                        // Write the bitmap data from it.
//                        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
//                        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
//                        try {
//                            outputStream.write(bitmapStream.toByteArray());
//                        } catch (IOException e1) {
//                            Log.i(LOG_TAG, "Unable to write file contents.");
//                        }
//                        // Create the initial metadata - MIME type and title.
//                        // Note that the user will be able to change the title later.
//                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
//                                .setMimeType("image/jpeg").setTitle("Android Photo.png").build();
//                        // Create an intent for the file chooser, and start it.
//                        IntentSender intentSender = Drive.DriveApi
//                                .newCreateFileActivityBuilder()
//                                .setInitialMetadata(metadataChangeSet)
//                                .setInitialDriveContents(result.getDriveContents())
//                                .build(mGoogleApiClient);
//                        try {
//                            startIntentSenderForResult(
//                                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
//                        } catch (IntentSender.SendIntentException e) {
//                            Log.i(LOG_TAG, "Failed to launch file chooser.");
//                        }
//                    }
//                });
//    }



    public void changeLang(String lang)
    {
        if (lang.equalsIgnoreCase(""))
            return;
        myLocale = new Locale(lang);
        saveLocale(lang);
        Locale.setDefault(myLocale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = myLocale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        updateFolderNames();
    }


    public void saveLocale(String lang)
    {
        String langPref = "Language";
        SharedPreferences prefs = getSharedPreferences("CommonPrefs", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(langPref, lang);
        editor.commit();
    }


    public void loadLocale()
    {
        String langPref = "Language";
        SharedPreferences prefs = getSharedPreferences("CommonPrefs", Activity.MODE_PRIVATE);
        String language = prefs.getString(langPref, "");
        changeLang(language);
    }

    public void updateFolderNames(){

        int indexOfLast = labels.size()-1;
        labels.set(0, getString(R.string.camera_title_standart));
        labels.set(1, getString(R.string.camera_title_notebook));
        labels.set(2, getString(R.string.camera_title_selfi));
        labels.set(3, getString(R.string.camera_title_scanner));
        labels.set(4, getString(R.string.camera_title_family));
        labels.set(5, getString(R.string.camera_title_job));
        labels.set(6, getString(R.string.camera_title_hobbi));
        labels.set(indexOfLast, getString(R.string.camera_title_add_new));
        adapter.notifyDataSetChanged();
        Hawk.put("labels", labels);
    }

}
