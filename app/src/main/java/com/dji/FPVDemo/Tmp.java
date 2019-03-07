package com.dji.FPVDemo;

import android.app.Activity;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.dji.FPVDemo.media.DJIVideoStreamDecoder;
//import com.dji.FPVDemo.media.NativeHelper;
import com.dji.videostreamdecodingsample.media.NativeHelper;

import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.sdkmanager.LiveStreamManager;

public class Tmp extends RosActivity {

    private static final java.lang.String TAG = Tmp.class.getName();
    protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;
    Camera mCamera = null;
    DJIVideoStreamDecoder djiDecoder = null;

    public VideoStream videoStream = null;

    public Tmp() {
        this("RosDrone", "RosDrone", URI.create("http://192.168.1.21:11311/"));//phantom wifi
        //this("RosDrone", "RosDrone", URI.create("http://192.168.1.104:11311/"));//dinf3
    }

    protected Tmp(String notificationTicker, String notificationTitle, URI uri) {
        super(notificationTicker, notificationTitle, uri);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mix);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname(), getMasterUri());

//        videoStream = new VideoStream();

        videoStream = VideoStream.getInstance();
        videoStream.setContext(this);
        nodeConfiguration.setNodeName("videoStream");
        nodeMainExecutor.execute(videoStream, nodeConfiguration);

//        initMy();

    }


//    void initMy() {
//
//        djiDecoder = new DJIVideoStreamDecoder(getApplicationContext(), videoStream);
//
//        final BaseProduct product = FPVDemoApplication.getProductInstance();
//
//        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {
//            @Override
//            public void onReceive(byte[] videoBuffer, int size) {
//                //handle h264
//                djiDecoder.parse(videoBuffer, size);
//            }
//        };
//
//        if (null == product || !product.isConnected()) {
//            mCamera = null;
//        } else {
//            if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
//                mCamera = product.getCamera();
//                mCamera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
//                    @Override
//                    public void onResult(DJIError djiError) {
//                        if (djiError != null) {
//                            Log.d(TAG, "can't change mode of camera, error:" + djiError.getDescription());
//                        } else {
//                            if (VideoFeeder.getInstance().getPrimaryVideoFeed() != null) {
//                                VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
//                            }
//                        }
//                    }
//                });
//
//
//            }
//        }
//
//    }
//
//
//    @Override
//    protected void onDestroy() {
//        if (djiDecoder != null) {
//            djiDecoder.destroy();
//        }
//        NativeHelper.getInstance().release();
//        super.onDestroy();
//    }

}
