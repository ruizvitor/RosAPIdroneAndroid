package com.dji.FPVDemo;

import android.os.Environment;

import org.apache.commons.logging.Log;
import org.ros.exception.RosRuntimeException;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.Node;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
//import java.lang.String;
import std_msgs.String;

import org.jboss.netty.buffer.ChannelBufferOutputStream;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.camera.Camera;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.ros.internal.message.MessageBuffers;

public class TalkerListener extends AbstractNodeMain {
    private static final java.lang.String TAG = TalkerListener.class.getName();

    ConnectedNode mynode;

    //BEGIN MediaManager
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager mMediaManager;
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/MediaManagerDemo/");
    private FetchMediaTaskScheduler scheduler;
    //END MediaManager

    Publisher<std_msgs.String> publisher;
    Publisher<sensor_msgs.CompressedImage> publisherImg;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava/talkerListener");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        mynode = connectedNode;

        final Log log = connectedNode.getLog();
        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber("chatter", std_msgs.String._TYPE);


        publisher = connectedNode.newPublisher("chatterResponse", std_msgs.String._TYPE);

        publisherImg = connectedNode.newPublisher("chatterImg", sensor_msgs.CompressedImage._TYPE);


        subscriber.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {


                log.info("I heard: \"" + message.getData() + "\"");

                if (message.getData().equals("photo")) {
                    switchCameraModeAndCapture(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                } else if (message.getData().equals("deleteFromSd")) {
                    initMediaManager(0);
                } else if (message.getData().equals("deleteFromAn")) {
                    clearAndroidFolder();
                } else if (message.getData().equals("downloadToAn")) {
                    initMediaManager(1);
                } else if (message.getData().equals("downloadToHost")) {

                    for (File f : destDir.listFiles()) {
                        if (f.isFile()) {
                            pubMessage(publisher, "Attempting to publish image " + destDir + "/" + f.getName() + "from android");
                            pubImg(publisherImg, destDir + "/" + f.getName());
                        }
                    }

                }
//                else if (message.getData().equals("all")) {
//                    allOp();
//                }

            }
        });
    }

    void clearAndroidFolder() {
        File[] dir = destDir.listFiles();
        pubMessage(publisher, dir.length + " files to delete from android");
        for (File f : dir) {
            if (f.isFile()) {
                if (f.delete()) {
                    pubMessage(publisher, "File deleted from android");
                } else {
                    pubMessage(publisher, "File not deleted from android");
                }
            }
        }
    }

    @Override
    public void onShutdown(Node node) {
        destroyMediaManager();
    }

    void pubMessage(Publisher<std_msgs.String> publisher, java.lang.String msg) {
        std_msgs.String str = publisher.newMessage();
        str.setData(msg);
        publisher.publish(str);
    }

//    public static int calculateInSampleSize(
//            BitmapFactory.Options options, int reqWidth, int reqHeight) {
//        // Raw height and width of image
//        final int height = options.outHeight;
//        final int width = options.outWidth;
//        int inSampleSize = 1;
//
//        if (height > reqHeight || width > reqWidth) {
//
//            final int halfHeight = height / 2;
//            final int halfWidth = width / 2;
//
//            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
//            // height and width larger than the requested height and width.
//            while ((halfHeight / inSampleSize) >= reqHeight
//                    && (halfWidth / inSampleSize) >= reqWidth) {
//                inSampleSize *= 2;
//            }
//        }
//
//        return inSampleSize;
//    }
//
//
//    public static Bitmap decodeSampledBitmapFromResource(java.lang.String path, int reqWidth, int reqHeight) {
//
//        // First decode with inJustDecodeBounds=true to check dimensions
//        final BitmapFactory.Options options = new BitmapFactory.Options();
//        //options.inJustDecodeBounds = true;
//        //BitmapFactory.decodeResource(res, resId, options);
//        //BitmapFactory.decodeFile(path, options);
//
//        // Calculate inSampleSize
//        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//
//        // Decode bitmap with inSampleSize set
//        options.inJustDecodeBounds = false;
//        //return BitmapFactory.decodeResource(res, resId, options);
//        return BitmapFactory.decodeFile(path, options);
//    }
//

    private Bitmap decodeMyFile(java.lang.String imgPath)
    {
        Bitmap b = null;
        int max_size = 1000;
        File f = new File(imgPath);
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream fis = new FileInputStream(f);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();
            int scale = 1;
            if (o.outHeight > max_size || o.outWidth > max_size)
            {
                scale = (int) Math.pow(2, (int) Math.ceil(Math.log(max_size / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            fis = new FileInputStream(f);
            b = BitmapFactory.decodeStream(fis, null, o2);
            fis.close();
        }
        catch (Exception e)
        {
        }
        return b;
    }

    void pubImg(Publisher<sensor_msgs.CompressedImage> publisherImg, java.lang.String path) {


        //1280h,720w good size!

        //Bitmap bmp = BitmapFactory.decodeFile(path);
//        Bitmap bmp = decodeSampledBitmapFromResource(path,720, 1280);
        Bitmap bmp = decodeMyFile(path);

//        if (bmp == null) {
//            android.util.Log.d(TAG, "FILE FAILED NULL!!!!!!!!!!!!!!!!!!!!!!!!!");
//        }

        //Compressed image

        sensor_msgs.CompressedImage image = publisherImg.newMessage();

        image.setFormat("jpeg");
        image.getHeader().setStamp(mynode.getCurrentTime());
        image.getHeader().setFrameId("camera");


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//        bmp.compress(Bitmap.CompressFormat.JPEG, 10, baos);
        bmp.recycle();//to destroy it


        ChannelBufferOutputStream stream = new ChannelBufferOutputStream(MessageBuffers.dynamicBuffer());
        stream.buffer().writeBytes(baos.toByteArray());


        image.setData(stream.buffer().copy());


        stream.buffer().clear();
//        android.util.Log.d(TAG, "pre publish");
        publisherImg.publish(image);
//        android.util.Log.d(TAG, "after publish");
    }


    private void switchCameraModeAndCapture(SettingsDefinitions.CameraMode cameraMode) {

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        android.util.Log.d(TAG, "Switch Camera Mode Succeeded");
                        captureAction();

                    } else {
                        android.util.Log.d(TAG, error.getDescription());
                    }

                }
            });
        }
    }

    // Method for taking photo
    private void captureAction() {

        final Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {

            SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
            camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (null == djiError) {
                        camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError == null) {
                                    //showToast("take photo: success");
                                    //initMediaManager();
                                    android.util.Log.d(TAG, "take photo: success");
                                    //callback.success("success");
                                    pubMessage(publisher, "photo taken");
                                } else {
                                    //showToast(djiError.getDescription());
                                    android.util.Log.d(TAG, "take photo: failed");
                                    //callback.success("failed");
                                    pubMessage(publisher, "photo failed");
                                }
                            }
                        });
                    } else pubMessage(publisher, "photo failed");

                }
            });
        } else pubMessage(publisher, "photo failed");
    }

    protected void destroyMediaManager() {
        if (mMediaManager != null) {
            mMediaManager.stop(null);
            //mMediaManager.removeFileListStateCallback(this.updateFileListStateListener);
            mMediaManager.exitMediaDownloading();
            if (scheduler != null) {
                scheduler.removeAllTasks();
            }
        }

        FPVDemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError mError) {
                if (mError != null) {
                    android.util.Log.d(TAG, "Set Shoot Photo Mode Failed" + mError.getDescription());
                }
            }
        });

        if (mediaFileList != null) {
            mediaFileList.clear();
        }

        android.util.Log.d(TAG, "onDestroy");
    }

    private void initMediaManager(final int download_flag) {
        android.util.Log.d(TAG, "Entering initMediaManager");
        if (FPVDemoApplication.getProductInstance() == null) {
            mediaFileList.clear();
//			mListAdapter.notifyDataSetChanged();
            DJILog.e(TAG, "Product disconnected");
            return;
        } else {
            if (null != FPVDemoApplication.getCameraInstance() && FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                mMediaManager = FPVDemoApplication.getCameraInstance().getMediaManager();
                if (null != mMediaManager) {
                    //mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
                    FPVDemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                DJILog.e(TAG, "Set cameraMode success");
                                // showProgressDialog();
                                getFileList(download_flag);
                            } else {
                                android.util.Log.d(TAG, "Set cameraMode failed");
                            }
                        }
                    });
                    if (mMediaManager.isVideoPlaybackSupported()) {
                        DJILog.e(TAG, "Camera support video playback!");
                    } else {
                        android.util.Log.d(TAG, "Camera does not support video playback!");
                    }
                    scheduler = mMediaManager.getScheduler();
                }

            } else if (null != FPVDemoApplication.getCameraInstance()
                    && !FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                android.util.Log.d(TAG, "Media Download Mode not Supported");
            }
        }
        return;
    }

    private void getFileList(final int download_flag) {
        mMediaManager = FPVDemoApplication.getCameraInstance().getMediaManager();
        if (mMediaManager != null) {

            if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                DJILog.e(TAG, "Media Manager is busy.");
            } else {

                mMediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {

                            //Reset data
                            if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
                                mediaFileList.clear();
                            }

                            mediaFileList = mMediaManager.getSDCardFileListSnapshot();
                            Collections.sort(mediaFileList, new Comparator<MediaFile>() {
                                @Override
                                public int compare(MediaFile lhs, MediaFile rhs) {
                                    if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
                                        return 1;
                                    } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
                                        return -1;
                                    }
                                    return 0;
                                }
                            });
                            scheduler.resume(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError error) {
                                    if (error == null) {
                                        doFileOp(download_flag);
                                    }
                                }
                            });
                        } else {
                            // hideProgressDialog();
                            android.util.Log.d(TAG, "Get Media File List Failed:" + djiError.getDescription());
                        }
                    }
                });

            }
        }
    }

    private void doFileOp(final int download_flag) {
        if (mediaFileList.size() <= 0) {
            android.util.Log.d(TAG, "No File info for downloading thumbnails");
            pubMessage(publisher, "No File info for downloading thumbnails");
            return;
        }
        android.util.Log.d(TAG, mediaFileList.size() + " files");
        pubMessage(publisher, mediaFileList.size() + " files");

        if (download_flag == 1) {
            for (int i = 0; i < mediaFileList.size(); i++) {
                downloadFileByIndex(i);
            }
        } else if (download_flag == 0) {
            deleteAll();
        }
    }

    private void downloadFileByIndex(final int index) {
        if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
                || (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
            return;
        }

        mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<java.lang.String>() {
            @Override
            public void onFailure(DJIError error) {
                android.util.Log.d(TAG, "Download File to android Failed" + error.getDescription());
                pubMessage(publisher, "Download File to android Failed" + error.getDescription());
            }

            @Override
            public void onProgress(long total, long current) {
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {

            }

            @Override
            public void onStart() {
            }

            @Override
            public void onSuccess(java.lang.String filePath) {
                android.util.Log.d(TAG, "Download File Success to android " + ":" + filePath);
                pubMessage(publisher, "Download File Success to android " + ":" + filePath);
            }
        });
    }

//    private void deleteFileByIndex(final int index) {
//        ArrayList<MediaFile> fileToDelete = new ArrayList<MediaFile>();
//        if (mediaFileList.size() > index) {
//            fileToDelete.add(mediaFileList.get(index));
//            mMediaManager.deleteFiles(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
//                @Override
//                public void onSuccess(List<MediaFile> x, DJICameraError y) {
//                    MediaFile file = mediaFileList.remove(index);
//
//                    DJILog.e(TAG, "Delete file success");
//                    android.util.Log.d(TAG,"Delete file success");
//                    pubMessage(publisher, "Delete file success" );
//                }
//
//                @Override
//                public void onFailure(DJIError error) {
//                    android.util.Log.d(TAG,"Delete file failed");
//                    pubMessage(publisher, "Delete file failed" );
//                }
//            });
//        }
//    }

    private void deleteAll() {
        ArrayList<MediaFile> fileToDelete = new ArrayList<MediaFile>();

        for (int i = 0; i < mediaFileList.size(); i++) {
            fileToDelete.add(mediaFileList.get(i));
        }

        mMediaManager.deleteFiles(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
            @Override
            public void onSuccess(List<MediaFile> x, DJICameraError y) {
                //MediaFile file = mediaFileList.remove(index);

                DJILog.e(TAG, "Delete file success");
                android.util.Log.d(TAG, "Delete file success in SD card");
                pubMessage(publisher, "Delete file success in SD card");
            }

            @Override
            public void onFailure(DJIError error) {
                android.util.Log.d(TAG, "Delete file failed in SD card");
                pubMessage(publisher, "Delete file failed in SD card");
            }
        });

    }

//    private void allOp() {
//        //deleteFromAn
//        clearAndroidFolder();
//        //deleteFromSd
//        initMediaManager(0);
//        //photo
//        switchCameraModeAndCapture(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
//        //downloadToAn
//        initMediaManager(1);
//        //downloadToHost
//        for (File f : destDir.listFiles()) {
//            if (f.isFile()) {
//                pubMessage(publisher, "Attempting to publish image " + destDir + "/" + f.getName() + "from android");
//                pubImg(publisherImg, destDir + "/" + f.getName());
//            }
//        }
//    }
}

