package com.dji.FPVDemo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;


import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dji.common.product.Model;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;

public class Mix extends RosActivity implements DJICodecManager.YuvDataCallback {

    private static final java.lang.String TAG = Mix.class.getName();

    /*
        START ROS CONFIG
     */

    VideoStream videoStream = null;

    public Mix() {
        this("RosDrone", "RosDrone", URI.create("http://192.168.1.21:11311/"));//phantom wifi
        //this("RosDrone", "RosDrone", URI.create("http://192.168.1.104:11311/"));//dinf3
    }

    protected Mix(String notificationTicker, String notificationTitle, URI uri) {
        super(notificationTicker, notificationTitle, uri);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mix);
        doSomething();
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname(), getMasterUri());

//        TalkerListener talkerListener = new TalkerListener();
//        nodeConfiguration.setNodeName("talkerListener");
//        nodeMainExecutor.execute(talkerListener, nodeConfiguration);
        videoStream = new VideoStream();
        nodeConfiguration.setNodeName("videoStream");
        nodeMainExecutor.execute(videoStream, nodeConfiguration);
    }
    /*
        END ROS CONFIG
     */

    private DJICodecManager mCodecManager;
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    private Camera mCamera;

    @Override
    protected void onResume() {
        super.onResume();
        doSomething();
    }

    void doSomething() {

        final BaseProduct product = FPVDemoApplication.getProductInstance();

        final MySurfaceView dummy = addPreView();
//        MySurfaceView dummy = new MySurfaceView(this);
//        mCodecManager = dummy.mCodecManager;
//        if (mCodecManager == null) {
//            Log.d(TAG,"mCodecManager == null");
//        } else {
//            Log.d(TAG,"mCodecManager != null");
//        }

//        int w = 960, h = 720;
//
//        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
//        Bitmap bmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap
//        Canvas canvas = new Canvas(bmp);
//
//        dummy.draw(canvas);

        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
//                Log.d(TAG, "ENTER mCodecManager.sendDataToDecoder!!!!!!!!");
                if (dummy.mCodecManager != null) {
//                    Log.d(TAG, "mCodecManager.sendDataToDecoder!!!!!!!!");
                    dummy.mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
            }
        };

        if (null == product || !product.isConnected()) {
            mCamera = null;
        } else {
            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
                mCamera = product.getCamera();
                mCamera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            Log.d(TAG, "can't change mode of camera, error:" + djiError.getDescription());
                        }
                    }
                });

                if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
                    VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
                }
            }
        }

//        if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
//            VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
//        }

//        if (mCodecManager == null) {
//            mCodecManager = new DJICodecManager(getApplicationContext(), dummy.mHolder, dummy.videoViewWidth,
//                    dummy.videoViewHeight);
//            mCodecManager.enabledYuvData(true);
//            mCodecManager.setYuvDataCallback(Mix.this);
//        }

    }

    @Override
    public void onYuvDataReceived(final ByteBuffer yuvFrame, final int dataSize, final int width, final int height) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "onYuvDataReceived RUN!!!!!!!!!!!");
                final byte[] bytes = new byte[dataSize];
                yuvFrame.get(bytes);
                videoStream.publishScreenShotv2(bytes, width, height);
            }
        });
    }


    /**
     * Add camera preview to the root of the activity layout.
     *
     * @return {@link CameraPreview} that was added to the view.
     */
    private MySurfaceView addPreView() {
        //create fake camera view
        MySurfaceView cameraSourceCameraPreview = new MySurfaceView(this, Mix.this);
        cameraSourceCameraPreview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View view = ((ViewGroup) getWindow().getDecorView().getRootView()).getChildAt(0);

        if (view instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) view;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(1, 1);
            linearLayout.addView(cameraSourceCameraPreview, params);
        } else if (view instanceof RelativeLayout) {
            RelativeLayout relativeLayout = (RelativeLayout) view;

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(1, 1);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            relativeLayout.addView(cameraSourceCameraPreview, params);
        } else if (view instanceof FrameLayout) {
            FrameLayout frameLayout = (FrameLayout) view;

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1, 1);
            frameLayout.addView(cameraSourceCameraPreview, params);
        } else {
            throw new RuntimeException("Root view of the activity/fragment cannot be other than Linear/Relative/Frame layout");
        }

        return cameraSourceCameraPreview;
    }

}
