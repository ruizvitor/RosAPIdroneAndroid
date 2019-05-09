package com.dji.RosAPI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


import org.ros.address.InetAddressFactory;
import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.math.BigInteger;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import dji.common.product.Model;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;

import android.net.wifi.WifiManager;
import android.content.Context;
import java.net.InetAddress;
import java.nio.ByteOrder;

public class SimpleActivity extends MyRosActivity implements DJICodecManager.YuvDataCallback {

    private static final java.lang.String TAG = SimpleActivity.class.getName();
    String ip = "http://192.168.1.20:11311/";
    private TextView mText;

    /*
        START ROS CONFIG
     */

    PublisherSubscriber publisherSubscriber = null;


    public SimpleActivity() {
        this(URI.create("http://192.168.1.20:11311/"));//pre configured ip phantom wifi
    }

    protected SimpleActivity(URI uri) {
        super(uri);
    }



    private String getLocalWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            ipAddressString = null;
        }

        return ipAddressString;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        ip = intent.getStringExtra("ipParam");

        android.util.Log.d(TAG, ip);

        configMyROSActivity(URI.create(ip));

        setContentView(R.layout.activity_mix);
        mText = (TextView) findViewById(R.id.rosOnline);

        if (publisherSubscriber != null) {
            mText.setText("ROS_MASTER_URI: " + getMasterUri());
        } else {
            mText.setText("ROS_MASTER_URI: " + ip);
        }

        initUi();
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getLocalWifiIpAddress(), URI.create(ip));
//      NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname(), getMasterUri());

        publisherSubscriber = new PublisherSubscriber(this);
        nodeConfiguration.setNodeName("publisherSubscriber");
        nodeMainExecutor.execute(publisherSubscriber, nodeConfiguration);
    }
    /*
        END ROS CONFIG
     */

    //BEGIN LIVE STREAMING VARS
    private SurfaceView videostreamPreviewSf;
    private SurfaceHolder videostreamPreviewSh;
    private SurfaceHolder.Callback surfaceCallback;
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

        if (publisherSubscriber != null) {
            mText.setText("ROS_MASTER_URI: " + getMasterUri());
        } else {
            mText.setText("ROS_MASTER_URI: " + ip);
        }
        if (mCodecManager != null) {
            mCodecManager.resetKeyFrame();
        }
//        notifyStatusChange();
//        initPreviewerSurfaceView();
    }

    @Override
    protected void onDestroy() {
        nodeMainExecutorService.forceShutdown();
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager.destroyCodec();
            mCodecManager = null;
        }
        super.onDestroy();
    }

    public void refreshStreaming() {
            if (mCodecManager != null) {
                mCodecManager.resetKeyFrame();
            }
    }

//    public void startStreaming() {
////        setContentView(R.layout.activity_mix);
////        initUi();
////        notifyStatusChange();
////        initPreviewerSurfaceView();
//
//        initPreviewerSurfaceView();
//    }
//
//    public void stopStreaming() {
//        if (mCodecManager != null) {
//            mCodecManager.cleanSurface();
//            mCodecManager.destroyCodec();
//        }
////        if(mReceivedVideoDataListener!=null){
////            mReceivedVideoDataListener=null;
////        }
////        if(videostreamPreviewSh!=null){
////            videostreamPreviewSh=null;
////        }
//    }

    private void initUi() {
        videostreamPreviewSf = (SurfaceView) findViewById(R.id.livestream_preview_sf);
//        VideoFeeder.getInstance().setTranscodingDataRate(10.0f);
//        VideoFeeder.getInstance().setTranscodingDataRate(20.0f);
//        VideoFeeder.getInstance().setTranscodingDataRate(3.0f);
        VideoFeeder.getInstance().setTranscodingDataRate(2.0f);
        updateUIVisibility();

        notifyStatusChange();
        initPreviewerSurfaceView();
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
                    mCodecManager.setYuvDataCallback(SimpleActivity.this);
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

        final BaseProduct product = RosAPIApplication.getProductInstance();
        Log.d(TAG, "notifyStatusChange: " + (product == null ? "Disconnect" : (product.getModel() == null ? "null model" : product.getModel().name())));


        // The callback for receiving the raw H264 video data for camera live view
        mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if (mCodecManager != null && size>0) {
                    mCodecManager.sendDataToDecoder(videoBuffer, size);
                }
//                mCodecManager.sendDataToDecoder(videoBuffer, size);
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

//        Log.d(TAG, "GOT FRAME!!!" );
        if (publisherSubscriber != null) {
            final byte[] bytes = new byte[(width + (width / 2)) * height];
            yuvFrame.get(bytes, 0, width * height);
            if (index++ % publisherSubscriber.skipFrameFlag == 0) {//skip frame logic
                publisherSubscriber.publishImage(bytes, width, height);
            }
            if (index > 10000) {//avoid index overflow
                index = 0;
            }
        }

    }

}
