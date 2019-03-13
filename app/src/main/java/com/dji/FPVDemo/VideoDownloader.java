package com.dji.FPVDemo;

import android.os.Environment;
import android.os.Handler;

import org.apache.commons.logging.Log;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dji.common.camera.ResolutionAndFrameRate;
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
import std_msgs.ByteMultiArray;
import std_msgs.String;

public class VideoDownloader extends AbstractNodeMain {

    private static final java.lang.String TAG = VideoDownloader.class.getName();

    private ConnectedNode mynode;

    //BEGIN MediaManager
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager mMediaManager;
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    private File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/MediaManagerDemo/");
    private FetchMediaTaskScheduler scheduler;
    //END MediaManager

    Publisher<String> publisher;
    Publisher<std_msgs.ByteMultiArray> publisherFile;
    private Camera camera;
    int cameraReadytoRecord = 0;
    int stopRecording = 0;
    long timer;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("rosjava/videoDownloader");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        mynode = connectedNode;

        final Log log = connectedNode.getLog();
//        Subscriber<String> subscriber = connectedNode.newSubscriber("chatter", std_msgs.String._TYPE);


        publisher = connectedNode.newPublisher("chatterResponse", std_msgs.String._TYPE);
        publisherFile = connectedNode.newPublisher("chatterFile", std_msgs.ByteMultiArray._TYPE);

        camera = FPVDemoApplication.getCameraInstance();
        mMediaManager = FPVDemoApplication.getCameraInstance().getMediaManager();

        setupVideoRecord();
    }

    @Override
    public void onShutdown(Node node) {
        destroyMediaManager();
    }

    protected void destroyMediaManager() {

        if (camera != null) {
            camera.stopRecordVideo(null);
            stopRecording = 1;
        }

        if (mMediaManager != null) {
            mMediaManager.stop(null);
            //mMediaManager.removeFileListStateCallback(this.updateFileListStateListener);
            mMediaManager.exitMediaDownloading();
            if (scheduler != null) {
                scheduler.removeAllTasks();
            }
        }

        if (mediaFileList != null) {
            mediaFileList.clear();
        }

        android.util.Log.d(TAG, "onDestroy");
    }

    private Runnable mRepeatVideo = new Runnable() {
        @Override
        public void run() {
            if (stopRecording == 0) {
                generateVideo();
            }
        }
    };

    private Runnable mDownloadToAndroid = new Runnable() {
        @Override
        public void run() {
            initMediaManager();
        }
    };

    private Runnable mStopRecord = new Runnable() {
        @Override
        public void run() {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        android.util.Log.d(TAG, "Stop recording: success");
                        //callback
                        (new Handler()).postDelayed(mDownloadToAndroid, 100);//
//                        (new Handler()).postDelayed(mRepeatVideo, 1000);//
                    } else {
                        android.util.Log.d(TAG, djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }
    };

    void generateVideo() {
        if (cameraReadytoRecord != 1) {
            android.util.Log.d(TAG, "cameraReadytoRecord != 1");
            setupVideoRecord();
        } else {
            android.util.Log.d(TAG, "cameraReadytoRecord == 1");
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        android.util.Log.d(TAG, "startRecordVideo: success");

                        android.util.Log.d(TAG, "DELTA:" + Long.toString(System.currentTimeMillis() - timer));
                        timer = System.currentTimeMillis();

//                        (new Handler()).postDelayed(mStopRecord, 2000);//stop after 2seconds
                        (new Handler()).postDelayed(mStopRecord, 1000);//stop after 2seconds
                    } else {
                        android.util.Log.d(TAG, djiError.getDescription());
                        (new Handler()).postDelayed(mRepeatVideo, 100);//
//                        generateVideo();
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    void setupVideoRecord() {
        //START set mode to record
        if (camera != null) {
            camera.setMode(SettingsDefinitions.CameraMode.RECORD_VIDEO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        android.util.Log.d(TAG, "Switch Camera Mode Succeeded");

                        //START set resolution and frameRate
                        SettingsDefinitions.VideoResolution resolution = SettingsDefinitions.VideoResolution.RESOLUTION_1280x720;
                        SettingsDefinitions.VideoFrameRate frameRate = SettingsDefinitions.VideoFrameRate.FRAME_RATE_25_FPS;
                        ResolutionAndFrameRate r = new ResolutionAndFrameRate(resolution, frameRate);
                        r.setResolution(resolution);
                        r.setFrameRate(frameRate);

                        camera.setVideoResolutionAndFrameRate(r, new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError error) {
                                if (error == null) {
                                    android.util.Log.d(TAG, "setVideoResolutionAndFrameRate Succeeded");
                                    cameraReadytoRecord = 1;
                                    generateVideo();
                                } else {
                                    android.util.Log.d(TAG, error.getDescription());
                                    cameraReadytoRecord = 0;
//                                    generateVideo();
                                }
                            }
                        });
                        //END  set resolution and frameRate
                    } else {
                        android.util.Log.e(TAG, error.getDescription());
//                        cameraReadytoRecord = 0;
                        cameraReadytoRecord = 1;
                        generateVideo();
                    }
                }
            });
        }
        //END set mode to record
    }


    private void initMediaManager() {
//        android.util.Log.d(TAG, "Entering initMediaManager");
        if (FPVDemoApplication.getProductInstance() == null) {
            mediaFileList.clear();
            DJILog.e(TAG, "Product disconnected");
            return;
        } else {
            if (null != FPVDemoApplication.getCameraInstance() && FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                if (null != mMediaManager) {
                    //mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
                    FPVDemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
//                                DJILog.d(TAG, "Set cameraMode success");
                                getFileList();
                            } else {
                                android.util.Log.e(TAG, "Set cameraMode failed");
                                (new Handler()).postDelayed(mDownloadToAndroid, 100);//
                            }
                        }
                    });
                    scheduler = mMediaManager.getScheduler();
                }

            } else if (null != FPVDemoApplication.getCameraInstance()
                    && !FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                android.util.Log.e(TAG, "Media Download Mode not Supported");
            }
        }
        return;
    }

    private void getFileList() {
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
//                            Collections.sort(mediaFileList, new Comparator<MediaFile>() {
//                                @Override
//                                public int compare(MediaFile lhs, MediaFile rhs) {
//                                    if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
//                                        return 1;
//                                    } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
//                                        return -1;
//                                    }
//                                    return 0;
//                                }
//                            });
                            scheduler.resume(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError error) {
                                    if (error == null) {
                                        doFileOp();
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

    private void doFileOp() {
        if (mediaFileList.size() <= 0) {
            android.util.Log.d(TAG, "No File info for downloading thumbnails");
//            pubMessage(publisher, "No File info for downloading thumbnails");
            return;
        }
        android.util.Log.d(TAG, mediaFileList.size() + " files");
//        pubMessage(publisher, mediaFileList.size() + " files");

        downloadFileByIndex(0);//most recent
    }

    private void downloadFileByIndex(final int index) {
        mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<java.lang.String>() {
            @Override
            public void onFailure(DJIError error) {
                android.util.Log.d(TAG, "Download File to android Failed" + error.getDescription());
//                pubMessage(publisher, "Download File to android Failed" + error.getDescription());
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
//                cameraReadytoRecord = 0;
//                (new Handler()).postDelayed(mRepeatVideo, 100);//
//
//                if (mediaFileList.size() > 100) {
//                    deleteAll();
//                }

                deleteAll();
//                pubMessage(publisher, "Download File Success to android " + ":" + filePath);
            }
        });
    }

    private void deleteAll() {
        ArrayList<MediaFile> fileToDelete = new ArrayList<MediaFile>();

        for (int i = 0; i < mediaFileList.size(); i++) {
            fileToDelete.add(mediaFileList.get(i));
        }

        mMediaManager.deleteFiles(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
            @Override
            public void onSuccess(List<MediaFile> x, DJICameraError y) {
                //MediaFile file = mediaFileList.remove(index);

                android.util.Log.d(TAG, "Delete file success in SD card");
                cameraReadytoRecord = 0;
                (new Handler()).postDelayed(mRepeatVideo, 100);//
//                generateVideo();
//                pubMessage(publisher, "Delete file success in SD card");
            }

            @Override
            public void onFailure(DJIError error) {
                android.util.Log.e(TAG, "Delete file failed in SD card");
//                pubMessage(publisher, "Delete file failed in SD card");
            }
        });

    }

    // Method for starting recording
    private void startRecord() {
        if (camera != null) {
            camera.startRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        android.util.Log.d(TAG, "Record video: success");
                    } else {
                        android.util.Log.d(TAG, djiError.getDescription());
                    }
                }
            }); // Execute the startRecordVideo API
        }
    }

    // Method for stopping recording
    private void stopRecord() {
        if (camera != null) {
            camera.stopRecordVideo(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        android.util.Log.d(TAG, "Stop recording: success");
                    } else {
                        android.util.Log.d(TAG, djiError.getDescription());
                    }
                }
            }); // Execute the stopRecordVideo API
        }
    }

    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode) {
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        android.util.Log.d(TAG, "Switch Camera Mode Succeeded");
                    } else {
                        android.util.Log.d(TAG, error.getDescription());
                    }
                }
            });
        }
    }


}
