package com.camera;

import android.os.Environment;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.exception.DropboxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Костя on 26.06.2015.
 */
public class DropBoxApi {
    private static final String LOG_TAG = "DropBoxApi";

    public static void postToDropBox(MainActivity mainActivity, final ArrayList<String> labels, final int position) {
        String directoryPictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/" + labels.get(position);
        String directoryMovies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + labels.get(position);
        post(mainActivity, labels, position, directoryPictures);
        post(mainActivity, labels, position, directoryMovies);
    }

    private static void post(final MainActivity mainActivity, final ArrayList<String> labels, final int position, String s) {
        final List<File> allPictures = SaveFileUtils.getAllFiles(s);
        for (final File picture : allPictures) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileInputStream inputStream = null;
                        inputStream = new FileInputStream(picture);
                        DropboxAPI.Entry response = mainActivity.mDBApi.putFile(labels.get(position) + "/" + picture.getName(), inputStream,
                                picture.length(), null, true, null);
                        Log.i(LOG_TAG, "The uploaded file's rev is: " + response.rev);
                    } catch (DropboxException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
