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
        initUi();
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname(), getMasterUri());

        videoStream = new VideoStream();
        nodeConfiguration.setNodeName("videoStream");
        nodeMainExecutor.execute(videoStream, nodeConfiguration);
    }
    /*
        END ROS CONFIG
     */


    //BEGIN LIVE STREAMING VARS
    private SurfaceView videostreamPreviewSf;
    private SurfaceHolder videostreamPreviewSh;
    private SurfaceHolder.Callback surfaceCallback;
    private VideoFeeder.VideoFeed standardVideoFeeder;
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    private DJICodecManager mCodecManager;
    private Camera mCamera;
    private int videoViewWidth;
    private int videoViewHeight;
    private int index = 0;
    //END LIVE STREAMING VARS

    @Override
    protected void onResume() {
        super.onResume();
        initPreviewerSurfaceView();
        notifyStatusChange();
    }

//    @Override
//    protected void onPause() {
//        if (mCamera != null) {
//            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
//                VideoFeeder.getInstance().getPrimaryVideoFeed().removeVideoDataListener(mReceivedVideoDataListener);
//            }
//            if (standardVideoFeeder != null) {
//                standardVideoFeeder.removeVideoDataListener(mReceivedVideoDataListener);
//            }
//        }
//        super.onPause();
//    }

    @Override
    protected void onDestroy() {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager.destroyCodec();
        }
        super.onDestroy();
    }

    private void initUi() {
        videostreamPreviewSf = (SurfaceView) findViewById(R.id.livestream_preview_sf);
//        VideoFeeder.getInstance().setTranscodingDataRate(10.0f);
//        VideoFeeder.getInstance().setTranscodingDataRate(20.0f);
        VideoFeeder.getInstance().setTranscodingDataRate(2.0f);
//        VideoFeeder.getInstance().setTranscodingDataRate(3.0f);
        updateUIVisibility();
    }

    private void updateUIVisibility() {
        videostreamPreviewSf.setVisibility(View.VISIBLE);
    }

    /**
     * Init a surface view for the DJIVideoStreamDecoder
     */
    private void initPreviewerSurfaceView() {
        videostreamPreviewSh = videostreamPreviewSf.getHolder();
        surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "real onSurfaceTextureAvailable");
                videoViewWidth = videostreamPreviewSf.getWidth();
                videoViewHeight = videostreamPreviewSf.getHeight();
                Log.d(TAG, "real onSurfaceTextureAvailable3: width " + videoViewWidth + " height " + videoViewHeight);

                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(getApplicationContext(), holder, videoViewWidth,
                            videoViewHeight);
                    mCodecManager.enabledYuvData(true);
                    mCodecManager.setYuvDataCallback(Mix.this);
                }

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                videoViewWidth = width;
                videoViewHeight = height;
                Log.d(TAG, "real onSurfaceTextureAvailable4: width " + videoViewWidth + " height " + videoViewHeight);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (mCodecManager != null) {
                    mCodecManager.cleanSurface();
                    mCodecManager.destroyCodec();
                    mCodecManager = null;
                }
            }
        };

        videostreamPreviewSh.addCallback(surfaceCallback);
    }


    private void notifyStatusChange() {

        final BaseProduct product = FPVDemoApplication.getProductInstance();
        Log.d(TAG, "notifyStatusChange: " + (product == null ? "Disconnect" : (product.getModel() == null ? "null model" : product.getModel().name())));


        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
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
    }


    @Override
    public void onYuvDataReceived(ByteBuffer yuvFrame, int dataSize, final int width, final int height) {

        //skip some frames to reduce delay
//        if (index++ % 10 == 0 && yuvFrame != null) {
          if (index++ % 10 == 0 && yuvFrame != null) {
//        if (index++ % 5 == 0 && yuvFrame != null) {
            final byte[] bytes = new byte[dataSize];
            yuvFrame.get(bytes);

            if (videoStream != null && (bytes.length >= width * height)) {
                videoStream.publishImage(bytes, width, height);
            }
        }

        if (index > 1000) {
            index = 0;
        }
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
