package com.dji.FPVDemo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import dji.sdk.codec.DJICodecManager;

class MySurfaceView extends SurfaceView {
    private static final java.lang.String TAG = MySurfaceView.class.getName();


    public SurfaceHolder mHolder;
    private SurfaceHolder.Callback surfaceCallback;
    public DJICodecManager mCodecManager = null;
    public int videoViewWidth = 960;
    public int videoViewHeight = 720;
    private Context mycontext;
    DJICodecManager.YuvDataCallback myyuv;

    MySurfaceView(@NonNull Context context, DJICodecManager.YuvDataCallback yuv) {
        super(context);
        mycontext = context;
        myyuv = yuv;
        //Set surface holder
        initSurfaceView();
    }

    private void initSurfaceView() {
        Log.d(TAG, "initSurfaceView!!!!!!!!!!!");
        mHolder = getHolder();

        surfaceCallback = new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated!!!!!!!!!!!");
//                Log.d(TAG, "real onSurfaceTextureAvailable");
//                videoViewWidth = videostreamPreviewSf.getWidth();
//                videoViewHeight = videostreamPreviewSf.getHeight();
                videoViewWidth = 960;
                videoViewHeight = 720;
//                Log.d(TAG, "real onSurfaceTextureAvailable3: width " + videoViewWidth + " height " + videoViewHeight);

                if (mCodecManager == null) {
                    mCodecManager = new DJICodecManager(mycontext, holder, videoViewWidth,
                            videoViewHeight);

                    mCodecManager.enabledYuvData(true);
                    mCodecManager.setYuvDataCallback(myyuv);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                videoViewWidth = width;
                videoViewHeight = height;
//                Log.d(TAG, "real onSurfaceTextureAvailable4: width " + videoViewWidth + " height " + videoViewHeight);
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

        mHolder.addCallback(surfaceCallback);
//        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
}
