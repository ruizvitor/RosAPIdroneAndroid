package com.dji.FPVDemo;

import android.os.Environment;

import org.apache.commons.logging.Log;
import org.ros.concurrent.CancellableLoop;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.Node;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


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
import std_msgs.String;

//import org.ros.android.BitmapFromCompressedImage;
//
//import java.io.IOException;
//
//import cv_bridge.CvImage;
//import sensor_msgs.Image;
//
//
//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.LoaderCallbackInterface;
//import org.opencv.android.OpenCVLoader;
//import org.opencv.core.Core;
//import org.opencv.core.Point;
//import org.opencv.core.Scalar;

//import org.ros.android.BitmapFromCompressedImage;

//{
//    private RosImageView<sensor_msgs.CompressedImage> image;
//
//    image = (RosImageView<sensor_msgs.CompressedImage>) findViewById(R.id.image);
//    image.setTopicName("/usb_cam/image_raw/compressed");
//    image.setMessageType(sensor_msgs.CompressedImage._TYPE);
//    image.setMessageToBitmapCallable(new BitmapFromCompressedImage());
//}


public class TalkerListener extends AbstractNodeMain {
    private static final java.lang.String TAG = TalkerListener.class.getName();

    //BEGIN MediaManager
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager mMediaManager;
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/MediaManagerDemo/");
    private FetchMediaTaskScheduler scheduler;
    //END MediaManager

    Publisher<std_msgs.String> publisher;
//    Publisher<sensor_msgs.CompressedImage> publisherImg;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava/talkerListener");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {


        final Log log = connectedNode.getLog();
        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber("chatter", std_msgs.String._TYPE);


        publisher = connectedNode.newPublisher("chatterResponse", std_msgs.String._TYPE);

//        publisherImg = connectedNode.newPublisher("chatterImg", sensor_msgs.CompressedImage._TYPE);


        subscriber.addMessageListener(new MessageListener<String>() {
            @Override
            public void onNewMessage(std_msgs.String message) {


                log.info("I heard: \"" + message.getData() + "\"");

                if (message.getData().equals("photo")) {
                    switchCameraModeAndCapture(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                } else if (message.getData().equals("download")) {
                    initMediaManager(1);
                } else if (message.getData().equals("delete")) {
                    initMediaManager(0);
                }

            }
        });
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

//    void pubImg(Publisher<sensor_msgs.CompressedImage> publisherImg, java.lang.String msg) {
////        BitmapFromCompressedImage bitmap = new BitmapFromCompressedImage();
////        publisherImg.publish(bitmap);
//
//        try {
//            publisherImg.publish(cvImage.toImageMsg(publisherImg.newMessage()));
//        } catch (IOException e) {
//            android.util.Log.d(TAG, "cv_bridge exception: " + e.getMessage());
//        }
//    }


    private void switchCameraModeAndCapture(SettingsDefinitions.CameraMode cameraMode) {

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
                        //showToast("Switch Camera Mode Succeeded");
                        android.util.Log.d(TAG, "Switch Camera Mode Succeeded");
                        captureAction();

                    } else {
                        //showToast(error.getDescription());
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
                    }
                }
            });
        }
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
//					mMediaManager.addMediaUpdatedVideoPlaybackStateListener(this.updatedVideoPlaybackStateListener);
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
                            // hideProgressDialog();

                            //Reset data
                            if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
                                mediaFileList.clear();
                                //lastClickViewIndex = -1;
                                //lastClickView = null;
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
                                        getThumbnails(download_flag);
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

    private void getThumbnails(final int download_flag) {
        if (mediaFileList.size() <= 0) {
            android.util.Log.d(TAG, "No File info for downloading thumbnails");
            pubMessage(publisher, "No File info for downloading thumbnails");
            return;
        }
        android.util.Log.d(TAG, mediaFileList.size() + " files");
        pubMessage(publisher, mediaFileList.size() + " files");
        for (int i = 0; i < mediaFileList.size(); i++) {
            // getThumbnailByIndex(i);

            if (download_flag == 1) {
                downloadFileByIndex(i);
            } else {
//                deleteFileByIndex(i);
            }


        }
        if (download_flag == 0) {
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
                // HideDownloadProgressDialog();
                android.util.Log.d(TAG, "Download File Failed" + error.getDescription());
                pubMessage(publisher, "Download File Failed" + error.getDescription());
            }

            @Override
            public void onProgress(long total, long current) {
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {
//			   int tmpProgress = (int) (1.0 * current / total * 100);
//			   if (tmpProgress != currentProgress)
//			   {
//			     mDownloadDialog.setProgress(tmpProgress);
//			     currentProgress = tmpProgress;
//			  }
            }

            @Override
            public void onStart() {
                //currentProgress = -1;
                //ShowDownloadProgressDialog();
            }

            @Override
            public void onSuccess(java.lang.String filePath) {
                // HideDownloadProgressDialog();
                android.util.Log.d(TAG, "Download File Success" + ":" + filePath);

                //PUBLISH FILE in a topic
                pubMessage(publisher, "Download File Success" + ":" + filePath);

                //currentProgress = -1;
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
                android.util.Log.d(TAG, "Delete file success");
                pubMessage(publisher, "Delete file success");
            }

            @Override
            public void onFailure(DJIError error) {
                android.util.Log.d(TAG, "Delete file failed");
                pubMessage(publisher, "Delete file failed");
            }
        });

    }
}

