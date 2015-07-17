package com.camera;

import android.os.Bundle;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestBatch;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Костя on 26.06.2015.
 */
public class FacebookApi {
    private static final String LOG_TAG = "FacebookApi";

    public static void postToFacebook(List<File> allPictures) {
        GraphRequestBatch batch = new GraphRequestBatch();
        for (File picture : allPictures) {
            GraphRequest graphRequest = GraphRequest.newPostRequest(AccessToken.getCurrentAccessToken(),
                    "/photos", null, new GraphRequest.Callback() {
                        @Override
                        public void onCompleted(GraphResponse graphResponse) {
                            Log.d(LOG_TAG, "------ graphResponse = " + graphResponse);
                        }
                    });

            Bundle parameters = new Bundle();
            try {
                parameters.putByteArray("Camera Photos", IOUtils.toByteArray(new FileInputStream(picture)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            graphRequest.setParameters(parameters);
            batch.add(graphRequest);
        }
        batch.addCallback(new GraphRequestBatch.Callback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch graphRequests) {
                Log.v(LOG_TAG, "onBatchCompleted " + graphRequests);
            }
        });
        batch.executeAsync();
    }

    public static void LoginToFacebook(MainActivity mainActivity) {
        List<String> permissionNeeds = Arrays.asList("publish_actions");
        LoginManager.getInstance().logInWithPublishPermissions(
                mainActivity,
                permissionNeeds);
        LoginManager.getInstance().registerCallback(mainActivity.mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResults) {
                        Log.v(LOG_TAG, "fasbook AccessToken " + loginResults.getAccessToken());

                    }

                    @Override
                    public void onCancel() {
                        Log.e(LOG_TAG, "facebook login canceled");
                    }

                    @Override
                    public void onError(FacebookException e) {
                        Log.e(LOG_TAG, "facebook login failed error");
                    }
                });
    }
}
