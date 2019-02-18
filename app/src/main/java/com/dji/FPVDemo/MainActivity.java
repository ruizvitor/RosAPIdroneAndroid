package com.dji.FPVDemo;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import dji.common.camera.SettingsDefinitions;
import dji.common.camera.SystemState;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.useraccount.UserAccountManager;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.product.Model;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.base.BaseProduct;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.FetchMediaTask;
import dji.sdk.media.FetchMediaTaskContent;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;
import dji.sdk.sdkmanager.DJISDKManager;


public class MainActivity extends Activity implements SurfaceTextureListener,OnClickListener{

	private static final String TAG = MainActivity.class.getName();
	protected VideoFeeder.VideoDataListener mReceivedVideoDataListener = null;

	// Codec for video live view
	protected DJICodecManager mCodecManager = null;

	protected TextureView mVideoSurface = null;
//	private Button mCaptureBtn, mShootPhotoModeBtn, mRecordVideoModeBtn;
	private Button mCaptureBtn, mDownloadBtn, mDeleteBtn;
//	private ToggleButton mRecordBtn;
	private TextView recordingTime;

	private Handler handler;

	//BEGIN MediaManager
	private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
	private MediaManager mMediaManager;
	private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
	private int currentProgress = -1;
	File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/MediaManagerDemo/");
	private FetchMediaTaskScheduler scheduler;
	//END MediaManager

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		handler = new Handler();

		initUI();

		// The callback for receiving the raw H264 video data for camera live view
		mReceivedVideoDataListener = new VideoFeeder.VideoDataListener() {

			@Override
			public void onReceive(byte[] videoBuffer, int size) {
				if (mCodecManager != null) {
					mCodecManager.sendDataToDecoder(videoBuffer, size);
				}
			}
		};

		Camera camera = FPVDemoApplication.getCameraInstance();

		if (camera != null) {

			camera.setSystemStateCallback(new SystemState.Callback() {
				@Override
				public void onUpdate(SystemState cameraSystemState) {
					if (null != cameraSystemState) {

						int recordTime = cameraSystemState.getCurrentVideoRecordingTimeInSeconds();
						int minutes = (recordTime % 3600) / 60;
						int seconds = recordTime % 60;

						final String timeString = String.format("%02d:%02d", minutes, seconds);
						final boolean isVideoRecording = cameraSystemState.isRecording();

						MainActivity.this.runOnUiThread(new Runnable() {

							@Override
							public void run() {

								recordingTime.setText(timeString);

								/*
								 * Update recordingTime TextView visibility and mRecordBtn's check state
								 */
								if (isVideoRecording){
									recordingTime.setVisibility(View.VISIBLE);
								}else
								{
									recordingTime.setVisibility(View.INVISIBLE);
								}
							}
						});
					}
				}
			});

		}

	}

	protected void onProductChange() {
		initPreviewer();
//		loginAccount();
	}

//	private void loginAccount(){
//
//		UserAccountManager.getInstance().logIntoDJIUserAccount(this,
//				new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
//					@Override
//					public void onSuccess(final UserAccountState userAccountState) {
//						Log.e(TAG, "Login Success");
//					}
//					@Override
//					public void onFailure(DJIError error) {
//						showToast("Login Error:"
//								+ error.getDescription());
//					}
//				});
//	}

	@Override
	public void onResume() {
		Log.e(TAG, "onResume");
		super.onResume();
		initPreviewer();
		onProductChange();

		if(mVideoSurface == null) {
			Log.e(TAG, "mVideoSurface is null");
		}
	}

	@Override
	public void onPause() {
		Log.e(TAG, "onPause");
		uninitPreviewer();
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.e(TAG, "onStop");
		super.onStop();
	}

	public void onReturn(View view){
		Log.e(TAG, "onReturn");
		this.finish();
	}

//	@Override
//	protected void onDestroy() {
//		Log.e(TAG, "onDestroy");
//		uninitPreviewer();
//		super.onDestroy();
//	}

	@Override
	protected void onDestroy() {
//		lastClickView = null;
		if (mMediaManager != null) {
			mMediaManager.stop(null);
			mMediaManager.removeFileListStateCallback(this.updateFileListStateListener);
//			mMediaManager.removeMediaUpdatedVideoPlaybackStateListener(updatedVideoPlaybackStateListener);
			mMediaManager.exitMediaDownloading();
			if (scheduler!=null) {
				scheduler.removeAllTasks();
			}
		}

		FPVDemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
			@Override
			public void onResult(DJIError mError) {
				if (mError != null){
					setResultToToast("Set Shoot Photo Mode Failed" + mError.getDescription());
				}
			}
		});

		if (mediaFileList != null) {
			mediaFileList.clear();
		}

		Log.e(TAG, "onDestroy");
		uninitPreviewer();
		super.onDestroy();
	}

	private void initUI() {
		// init mVideoSurface
		mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);

		recordingTime = (TextView) findViewById(R.id.timer);
		mCaptureBtn = (Button) findViewById(R.id.btn_capture);
		mDownloadBtn = (Button) findViewById(R.id.btn_download);
		mDeleteBtn = (Button) findViewById(R.id.btn_delete);
//		mRecordBtn = (ToggleButton) findViewById(R.id.btn_record);
//		mShootPhotoModeBtn = (Button) findViewById(R.id.btn_shoot_photo_mode);
//		mRecordVideoModeBtn = (Button) findViewById(R.id.btn_record_video_mode);

		if (null != mVideoSurface) {
			mVideoSurface.setSurfaceTextureListener(this);
		}

		mCaptureBtn.setOnClickListener(this);
		mDownloadBtn.setOnClickListener(this);
		mDeleteBtn.setOnClickListener(this);
//		mRecordBtn.setOnClickListener(this);
//		mShootPhotoModeBtn.setOnClickListener(this);
//		mRecordVideoModeBtn.setOnClickListener(this);

		recordingTime.setVisibility(View.INVISIBLE);

//		mRecordBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//				if (isChecked) {
//					startRecord();
//				} else {
//					stopRecord();
//				}
//			}
//		});
	}

	private void initPreviewer() {

		BaseProduct product = FPVDemoApplication.getProductInstance();

		if (product == null || !product.isConnected()) {
			showToast(getString(R.string.disconnected));
		} else {
			if (null != mVideoSurface) {
				mVideoSurface.setSurfaceTextureListener(this);
			}
			if (!product.getModel().equals(Model.UNKNOWN_AIRCRAFT)) {
				VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(mReceivedVideoDataListener);
			}
		}
	}

	private void uninitPreviewer() {
		Camera camera = FPVDemoApplication.getCameraInstance();
		if (camera != null){
			// Reset the callback
			VideoFeeder.getInstance().getPrimaryVideoFeed().addVideoDataListener(null);
		}
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		Log.e(TAG, "onSurfaceTextureAvailable");
		if (mCodecManager == null) {
			mCodecManager = new DJICodecManager(this, surface, width, height);
		}
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		Log.e(TAG, "onSurfaceTextureSizeChanged");
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		Log.e(TAG,"onSurfaceTextureDestroyed");
		if (mCodecManager != null) {
			mCodecManager.cleanSurface();
			mCodecManager = null;
		}

		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	}

	public void showToast(final String msg) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
			case R.id.btn_capture:{
//				switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
				captureAction();

//				Intent intent = new Intent(this, MainActivity.class);
//                Intent intent = new Intent(this, RosHelloWorldApp.class);
//				startActivity(intent);

				break;
			}
			case R.id.btn_download:{
				initMediaManager(1);
				break;
			}
			case R.id.btn_delete:{
				initMediaManager(0);
				break;
			}

//			case R.id.btn_shoot_photo_mode:{
//				switchCameraMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO);
//				break;
//			}
//			case R.id.btn_record_video_mode:{
//				switchCameraMode(SettingsDefinitions.CameraMode.RECORD_VIDEO);
//				break;
//			}
			default:
				break;
		}
	}

	private void switchCameraMode(SettingsDefinitions.CameraMode cameraMode){

		Camera camera = FPVDemoApplication.getCameraInstance();
		if (camera != null) {
			camera.setMode(cameraMode, new CommonCallbacks.CompletionCallback() {
				@Override
				public void onResult(DJIError error) {

					if (error == null) {
						showToast("Switch Camera Mode Succeeded");
					} else {
						showToast(error.getDescription());
					}
				}
			});
		}
	}

	// Method for taking photo
	private void captureAction(){

		final Camera camera = FPVDemoApplication.getCameraInstance();
		if (camera != null) {

			SettingsDefinitions.ShootPhotoMode photoMode = SettingsDefinitions.ShootPhotoMode.SINGLE; // Set the camera capture mode as Single mode
			camera.setShootPhotoMode(photoMode, new CommonCallbacks.CompletionCallback(){
				@Override
				public void onResult(DJIError djiError) {
					if (null == djiError) {
						handler.postDelayed(new Runnable() {
							@Override
							public void run() {
								camera.startShootPhoto(new CommonCallbacks.CompletionCallback() {
									@Override
									public void onResult(DJIError djiError) {
										if (djiError == null) {
											showToast("take photo: success");
//											initMediaManager();
										} else {
											showToast(djiError.getDescription());
										}
									}
								});
							}
						}, 100);
					}
				}
			});
		}
	}

//	// Method for starting recording
//	private void startRecord(){
//
//		final Camera camera = FPVDemoApplication.getCameraInstance();
//		if (camera != null) {
//			camera.startRecordVideo(new CommonCallbacks.CompletionCallback(){
//				@Override
//				public void onResult(DJIError djiError)
//				{
//					if (djiError == null) {
//						showToast("Record video: success");
//					}else {
//						showToast(djiError.getDescription());
//					}
//				}
//			}); // Execute the startRecordVideo API
//		}
//	}
//
//	// Method for stopping recording
//	private void stopRecord(){
//
//		Camera camera = FPVDemoApplication.getCameraInstance();
//		if (camera != null) {
//			camera.stopRecordVideo(new CommonCallbacks.CompletionCallback(){
//
//				@Override
//				public void onResult(DJIError djiError)
//				{
//					if(djiError == null) {
//						showToast("Stop recording: success");
//					}else {
//						showToast(djiError.getDescription());
//					}
//				}
//			}); // Execute the stopRecordVideo API
//		}
//
//	}

	private void setResultToToast(final String result)
	{
		runOnUiThread(new Runnable()
		{
			public void run()
			{
				Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
			}
		});
	}

	//Listeners
	private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
		@Override
		public void onFileListStateChange(MediaManager.FileListState state) {
			currentFileListState = state;
		}
	};


	private void initMediaManager(final int download_flag)
	{
		setResultToToast("Entering Download");
		if (FPVDemoApplication.getProductInstance() == null)
		{
			mediaFileList.clear();
//			mListAdapter.notifyDataSetChanged();
			DJILog.e(TAG, "Product disconnected");
			return;
		}
		else {
			if (null != FPVDemoApplication.getCameraInstance() && FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported())
			{
				mMediaManager = FPVDemoApplication.getCameraInstance().getMediaManager();
				if (null != mMediaManager)
				{
					mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
//					mMediaManager.addMediaUpdatedVideoPlaybackStateListener(this.updatedVideoPlaybackStateListener);
					FPVDemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback()
					{
						@Override
						public void onResult(DJIError error)
						{
							if (error == null)
							{
								DJILog.e(TAG, "Set cameraMode success");
								// showProgressDialog();
								getFileList(download_flag);
							}
							else {
								setResultToToast("Set cameraMode failed");
							}
						}
					});
					if (mMediaManager.isVideoPlaybackSupported())
					{
						DJILog.e(TAG, "Camera support video playback!");
					}
					else {
						setResultToToast("Camera does not support video playback!");
					}
					scheduler = mMediaManager.getScheduler();
				}

			}
			else if (null != FPVDemoApplication.getCameraInstance()
					&& !FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported())
			{
				setResultToToast("Media Download Mode not Supported");
			}
		}
		return;
	}

	private void getFileList(final int download_flag)
	{
		mMediaManager = FPVDemoApplication.getCameraInstance().getMediaManager();
		if (mMediaManager != null)
		{

			if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING))
			{
				DJILog.e(TAG, "Media Manager is busy.");
			}
			else{

				mMediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, new CommonCallbacks.CompletionCallback()
				{
					@Override
					public void onResult(DJIError djiError)
					{
						if (null == djiError)
						{
							// hideProgressDialog();

							//Reset data
							if (currentFileListState != MediaManager.FileListState.INCOMPLETE)
							{
								mediaFileList.clear();
//					      lastClickViewIndex = -1;
//					      lastClickView = null;
							}

							mediaFileList = mMediaManager.getSDCardFileListSnapshot();
							Collections.sort(mediaFileList, new Comparator<MediaFile>()
							{
								@Override
								public int compare(MediaFile lhs, MediaFile rhs)
								{
									if (lhs.getTimeCreated() < rhs.getTimeCreated())
									{
										return 1;
									}
									else if (lhs.getTimeCreated() > rhs.getTimeCreated())
									{
										return -1;
									}
									return 0;
								}
							});
							scheduler.resume(new CommonCallbacks.CompletionCallback()
							{
								@Override
								public void onResult(DJIError error)
								{
									if (error == null)
									{
										getThumbnails(download_flag);
									}
								}
							});
						}
						else {
							// hideProgressDialog();
							setResultToToast("Get Media File List Failed:" + djiError.getDescription());
						}
					}
				});

			}
		}
	}

	private void getThumbnails(final int download_flag)
	{
		if (mediaFileList.size() <= 0)
		{
			setResultToToast("No File info for downloading thumbnails");
			return;
		}
		for (int i = 0; i < mediaFileList.size(); i++) {
			// getThumbnailByIndex(i);

			if(download_flag == 1){
				downloadFileByIndex(i);
			}
			else{
				deleteFileByIndex(i);
			}


		}
	}

	private void downloadFileByIndex(final int index)
	{
		if ((mediaFileList.get(index).getMediaType() == MediaFile.MediaType.PANORAMA)
				|| (mediaFileList.get(index).getMediaType() == MediaFile.MediaType.SHALLOW_FOCUS))
		{
			return;
		}

		mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<String>()
		{
			@Override
			public void onFailure(DJIError error)
			{
				// HideDownloadProgressDialog();
				setResultToToast("Download File Failed" + error.getDescription());
				currentProgress = -1;
			}

			@Override
			public void onProgress(long total, long current)
			{
			}

			@Override
			public void onRateUpdate(long total, long current, long persize)
			{
//			   int tmpProgress = (int) (1.0 * current / total * 100);
//			   if (tmpProgress != currentProgress)
//			   {
//			     mDownloadDialog.setProgress(tmpProgress);
//			     currentProgress = tmpProgress;
//			  }
			}

			@Override
			public void onStart()
			{
				currentProgress = -1;
//			   ShowDownloadProgressDialog();
			}

			@Override
			public void onSuccess(String filePath)
			{
				// HideDownloadProgressDialog();
				setResultToToast("Download File Success" + ":" + filePath);
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
					setResultToToast("Delete file failed");
				}
			});
		}
	}
}