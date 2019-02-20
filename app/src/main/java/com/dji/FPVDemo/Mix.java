package com.dji.FPVDemo;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;


import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.io.File;
import java.net.URI;
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

public class Mix extends RosActivity implements OnClickListener {

    /*
        START ROS CONFIG
     */
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

        initUI();
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname(), getMasterUri());

        TalkerListener talkerListener = new TalkerListener();
        nodeConfiguration.setNodeName("talkerListener");
        nodeMainExecutor.execute(talkerListener, nodeConfiguration);
    }
    /*
        END ROS CONFIG
     */

    private static final String TAG = Mix.class.getName();
    private Button mRosBtn, mCaptureBtn, mDownloadBtn, mDeleteBtn;

    //BEGIN MediaManager
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager mMediaManager;
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    private int currentProgress = -1;
    File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/MediaManagerDemo/");
    private FetchMediaTaskScheduler scheduler;
    //END MediaManager

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.btn_rosjava: {
                //
                break;
            }
            case R.id.btn_capture: {
//                switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
                captureAction();
                break;
            }
            case R.id.btn_download: {
                initMediaManager(1);
                break;
            }
            case R.id.btn_delete: {
                initMediaManager(0);
                break;
            }
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        if (mMediaManager != null) {
            mMediaManager.stop(null);
            mMediaManager.removeFileListStateCallback(this.updateFileListStateListener);
            mMediaManager.exitMediaDownloading();
            if (scheduler != null) {
                scheduler.removeAllTasks();
            }
        }

        FPVDemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError mError) {
                if (mError != null) {
                    showToast("Set Shoot Photo Mode Failed" + mError.getDescription());
                }
            }
        });

        if (mediaFileList != null) {
            mediaFileList.clear();
        }

        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    private void initUI() {
        mRosBtn = (Button) findViewById(R.id.btn_rosjava);
        mCaptureBtn = (Button) findViewById(R.id.btn_capture);
        mDownloadBtn = (Button) findViewById(R.id.btn_download);
        mDeleteBtn = (Button) findViewById(R.id.btn_delete);

        mCaptureBtn.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(Mix.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode) {

        Camera camera = FPVDemoApplication.getCameraInstance();
        if (camera != null) {
            camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {

                    if (error == null) {
//                        showToast("Switch Camera Mode Succeeded");
                    } else {
                        showToast(error.getDescription());
                    }
                }
            });
        }
    }

    // Method for taking photo
    private void captureAction() {

        switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);

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
                                    showToast("take photo: success");
                                    //initMediaManager();
                                } else {
                                    showToast(djiError.getDescription());
                                }
                            }
                        });
                    }
                }
            });
        }
    }


    //Listeners
    private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
        @Override
        public void onFileListStateChange(MediaManager.FileListState state) {
            currentFileListState = state;
        }
    };


    private void initMediaManager(final int download_flag) {
        showToast("Entering initMediaManager");
        if (FPVDemoApplication.getProductInstance() == null) {
            mediaFileList.clear();
//			mListAdapter.notifyDataSetChanged();
            DJILog.e(TAG, "Product disconnected");
            return;
        } else {
            if (null != FPVDemoApplication.getCameraInstance() && FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                mMediaManager = FPVDemoApplication.getCameraInstance().getMediaManager();
                if (null != mMediaManager) {
                    mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
//					mMediaManager.addMediaUpdatedVideoPlaybackStateListener(this.updatedVideoPlaybackStateListener);
                    FPVDemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                DJILog.e(TAG, "Set cameraMode success");
                                // showProgressDialog();
                                getFileList(download_flag);
                            } else {
                                showToast("Set cameraMode failed");
                            }
                        }
                    });
                    if (mMediaManager.isVideoPlaybackSupported()) {
                        DJILog.e(TAG, "Camera support video playback!");
                    } else {
                        showToast("Camera does not support video playback!");
                    }
                    scheduler = mMediaManager.getScheduler();
                }

            } else if (null != FPVDemoApplication.getCameraInstance()
                    && !FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported()) {
                showToast("Media Download Mode not Supported");
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
                            showToast("Get Media File List Failed:" + djiError.getDescription());
                        }
                    }
                });

            }
        }
    }

    private void getThumbnails(final int download_flag) {
        if (mediaFileList.size() <= 0) {
            showToast("No File info for downloading thumbnails");
            return;
        }
        showToast(mediaFileList.size() + " files");
        for (int i = 0; i < mediaFileList.size(); i++) {
            // getThumbnailByIndex(i);

            if (download_flag == 1) {
                downloadFileByIndex(i);
            } else {
                deleteFileByIndex(i);
            }


        }
    }

    private void downloadFileByIndex(final int index) {
        if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
                || (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS)) {
            return;
        }

        mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<String>() {
            @Override
            public void onFailure(DJIError error) {
                // HideDownloadProgressDialog();
                showToast("Download File Failed" + error.getDescription());
                currentProgress = -1;
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
                currentProgress = -1;
//			   ShowDownloadProgressDialog();
            }

            @Override
            public void onSuccess(String filePath) {
                // HideDownloadProgressDialog();
                showToast("Download File Success" + ":" + filePath);

                //PUBLISH FILE in a topic

                currentProgress = -1;
            }
        });
    }

    private void deleteFileByIndex(final int index) {
        ArrayList<MediaFile> fileToDelete = new ArrayList<MediaFile>();
        if (mediaFileList.size() > index) {
            fileToDelete.add(mediaFileList.get(index));
            mMediaManager.deleteFiles(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
                @Override
                public void onSuccess(List<MediaFile> x, DJICameraError y) {
                    DJILog.e(TAG, "Delete file success");
                    showToast("Delete file success");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            MediaFile file = mediaFileList.remove(index);

                            //Reset select view
//							lastClickViewIndex = -1;
//							lastClickView = null;

                            //Update recyclerView
//							mListAdapter.notifyItemRemoved(index);
                        }
                    });
                }

                @Override
                public void onFailure(DJIError error) {
                    showToast("Delete file failed");
                }
            });
        }
    }
}
