package com.pk.util.procam;

import java.io.File;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pk.util.procam.CameraHandler.VideoProperties;

/**
 * The Class CameraCaptureActivity.
 */
public class CameraCaptureActivity extends Activity implements CameraCallback {

	private final String TAG = getClass().getSimpleName();
	
	private boolean mIsImage = false;
	
	public static int AUDIO_CAPTURE_REQUEST_CODE = 100;
	public static int IMAGE_CAPTURE_REQUEST_CODE = 101;
	public static int VIDEO_CAPTURE_REQUEST_CODE = 102;
	public static int PICK_IMAGE_GALLERY = 103;
	public static int PICK_VIDEO_GALLERY = 104;

	public static String CAMERA_CAPTURE_IMAGE = "CAPTURE_IMAGE";
	public static String CAMERA_CAPTURED_FILE_FULL_PATH = "FILE_FULL_PATH";
	
	public static final int CAMERA_PREVIEW_SCREEN = 1000;
	public static final int IMAGE_CONFIRMATION_SCREEN = 1001;
	public static final int VIDEO_CONFIRMATION_SCREEN = 1002;
	
	public static final String INTENT_VIDEO_TIME_TO_RECORD_IN_SECONDS = "INTENT_VIDEO_TIME_TO_RECORD"; 
	
	public static String CAMERA_CAPTURE_IMAGE_ACTION = "com.pk.utils.procam.CameraCaptureActivity.CAPTURE_IMAGE";
	public static String CAMERA_CAPTURE_VIDEO_ACTION = "com.pk.utils.procam.CameraCaptureActivity.CAPTURE_VIDEO";
	
	private CameraHandler mCameraHandler;
	private Button mBtnCapture;
	private Button mBtnSave;
	private Button mBtnDiscard;
	private FrameLayout mFlCameraPreview;
	private ImageView mIvPreview;
	private ImageView mIvMovieIndicator;
	private TextView mTvTimer;
	private RelativeLayout mRlActionButtons;
	private LinearLayout mLlAfterEffects;
	private LinearLayout mLlTimerLayout;
	
	private boolean mVideoPlaybackInProgress = false;
	private boolean mIsSavePending = false;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.camera_preview);
		
		Log.i(TAG, "onCreate");
		
		mBtnCapture = (Button) findViewById(R.id.cp_BtnCapture);
		mBtnSave = (Button) findViewById(R.id.cp_BtnSave);
		mBtnDiscard = (Button) findViewById(R.id.cp_BtnCancel);
		mRlActionButtons = (RelativeLayout) findViewById(R.id.cp_RlActionButtons);
		mLlAfterEffects = (LinearLayout) findViewById(R.id.cp_LlAfterEffects);
		mFlCameraPreview = (FrameLayout) findViewById(R.id.cp_FlFrameLayout);
		mIvPreview = (ImageView) findViewById(R.id.cp_IvPreview);
		mIvMovieIndicator = (ImageView) findViewById(R.id.cp_IvMovieIndicator);
		mTvTimer = (TextView) findViewById(R.id.cp_TvTimer);
		mLlTimerLayout = (LinearLayout) findViewById(R.id.cp_LlTimer);
		
		setupUi(CAMERA_PREVIEW_SCREEN);
		
		Intent i = getIntent();
		mIsImage = i.getAction().equals(CameraCaptureActivity.CAMERA_CAPTURE_IMAGE_ACTION);
		setTimeToRecord(i.getIntExtra(INTENT_VIDEO_TIME_TO_RECORD_IN_SECONDS, 0));
		
		Log.i(TAG, "onCreate - mIsImage: "+mIsImage);
		//setupCamera();
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() {
		
		super.onStart();

		Log.i(TAG, "onStart");
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		
		super.onResume();
		
		Log.i(TAG, "onResume - mVideoPlaybackInProgress: "+mVideoPlaybackInProgress+" mIsSavePending: "+mIsSavePending+" mResetOnResume: "+mResetOnResume);
		
		if (mResetOnResume) {
			resetCameraUi();
		} else {
			if (!mVideoPlaybackInProgress && !mIsSavePending) {
				setupCamera();
			} else {
				mVideoPlaybackInProgress = false;
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		Log.i(TAG, "onPause - mVideoCaptureInProgress: "+mVideoCaptureInProgress+" mVideoPlaybackInProgress: "+mVideoPlaybackInProgress+" mIsSavePending: "+mIsSavePending);
		
		try {
			if (mVideoCaptureInProgress) {
				stopVideoRecording();
			}
		}  catch (Exception e) {
			Log.e(TAG, "RuntimeException on stopping video: "+e.getMessage());
			e.printStackTrace();
			mResetOnResume = true;
		} finally {
			if (mCameraHandler != null && mCameraHandler.getCamera() != null) {
				mCameraHandler.releaseCamera();
				mCameraHandler = null;
			}
		}
	}
	
	private boolean mResetOnResume = false;
	private void resetCameraUi() {
		mVideoCaptureInProgress = false;
		mTimer.cancel();
		mTimer = null;
		setupUi(CAMERA_PREVIEW_SCREEN);
		setupCamera();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		Log.i(TAG, "onStop");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		Log.i(TAG, "onDestroy");
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onBackPressed()
	 */
	@Override
	public void onBackPressed() {
//		super.onBackPressed();
		
		if (mVideoCaptureInProgress) {
			if(timeElapsed > 1){
				try {
					stopVideoRecording();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			super.onBackPressed();
		}
	}
	
	private void setupUi(int screen) {
		mFlCameraPreview.setVisibility(View.GONE);
		mRlActionButtons.setVisibility(View.GONE);
		mLlAfterEffects.setVisibility(View.GONE);
		mIvPreview.setVisibility(View.GONE);
		mIvMovieIndicator.setVisibility(View.GONE);
		mLlTimerLayout.setVisibility(View.GONE);
		
		switch (screen) {
			case CAMERA_PREVIEW_SCREEN: {
				// Show the Camera surface layout and the Capture button layout
				// Hide everything else
				mFlCameraPreview.setVisibility(View.VISIBLE);
				mRlActionButtons.setVisibility(View.VISIBLE);
				if (mIsImage) {
					mBtnCapture.setBackgroundResource(R.drawable.lens);
				} else {
					if (mVideoCaptureInProgress) {
						mLlTimerLayout.setVisibility(View.VISIBLE);
						mBtnCapture.setBackgroundResource(R.drawable.stop_icon);
					} else {
						mBtnCapture.setBackgroundResource(R.drawable.video_icon);
					}
				}
				break;
			}
			case IMAGE_CONFIRMATION_SCREEN: {
				// Show the image preview UI. Hide the movie indicator icon
				mIvPreview.setVisibility(View.VISIBLE);
				mLlAfterEffects.setVisibility(View.VISIBLE);
				break;
			}
			case VIDEO_CONFIRMATION_SCREEN: {
				// Show the image preview and the movie indicator icon
				mIvPreview.setVisibility(View.VISIBLE);
				mIvMovieIndicator.setVisibility(View.VISIBLE);
				mLlAfterEffects.setVisibility(View.VISIBLE);
				break;
			}
		}
	}
	
	/**
	 * Setup camera.
	 */
	private void setupCamera() {
		Log.i(TAG, "setupCamera");
		if (mCameraHandler == null) {
			try {
				mCameraHandler = new CameraHandler(this, this);
				
				mBtnCapture.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						enableDisableCaptureButton(false, false);
						doCapture();
					}
				});
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				mCameraHandler = null;
				enableDisableCaptureButton(false, false);
			}
		}
		showCameraPreview();
	}
	
	private void enableDisableCaptureButton(boolean enable, boolean isRecording) {
		mBtnCapture.setEnabled(enable);
		if (isRecording) {
			mBtnCapture.setBackgroundResource(R.drawable.stop_icon);
		}
	}
	
	/**
	 * Show camera preview.
	 */
	private void showCameraPreview() {
		Log.i(TAG, "showCameraPreview");
		if (mCameraHandler != null) {
			setupUi(CAMERA_PREVIEW_SCREEN);
			mCameraHandler.showCameraPreview(mFlCameraPreview);
			mCameraHandler.setImageStoragePath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
			mCameraHandler.setVideoStoragePath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath());
		}
	}
	
	/**
	 * Do capture.
	 */
	private void doCapture() {
		if (mCameraHandler != null) {
			if (mIsImage) {
				mCameraHandler.takePicture();
			} else {
				Log.i(TAG, "doCapture - mVideoCaptureInProgress: "+mVideoCaptureInProgress);
				if (mVideoCaptureInProgress) {
					if (timeElapsed > 1) {
					      try {
							stopVideoRecording();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
					      enableDisableCaptureButton(true, true);
					}
				} else {
					captureVideo();
				}
			}
		}
	}
	
	/**
	 * Capture video.
	 */
	private void captureVideo() {
		// Video width: 640, Video Height: 480, Maximum duration for video recording: 90 seconds
		VideoProperties properties = mCameraHandler.new VideoProperties()
				.setFrameRate(30).setWidth(640).setHeight(480).setMaxDurationMs(90*1000).setMaxFileSizeBytes(200 * 1024 * 1024);
		mCameraHandler.setVideoProperties(properties);
		mCameraHandler.startVideoRecording();
	}
	
	/**
	 * Stop video recording.
	 * @throws Exception 
	 */
	private void stopVideoRecording() throws Exception {
		mCameraHandler.stopVideoRecording();
	}

	private void enableDisableAfterEffectsButtons(boolean enable) {
		mBtnSave.setEnabled(enable);
		mBtnDiscard.setEnabled(enable);
	}
	
	/** The m video capture in progress. */
	private boolean mVideoCaptureInProgress = false;
	
	/* (non-Javadoc)
	 * @see com.alldata.carcue.camera.CameraCallback#onVideoCaptureStarted()
	 */
	@Override
	public void onVideoCaptureStarted() {
		Log.i(TAG, "onVideoCaptureStarted");
		enableDisableCaptureButton(true, true);
		mVideoCaptureInProgress = true;
		setupUi(CAMERA_PREVIEW_SCREEN);
		startTimer();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/* (non-Javadoc)
	 * @see com.alldata.carcue.camera.CameraCallback#onImageCaptured(java.lang.String)
	 */
	@Override
	public void onImageCaptured(final String fullFilePath) {
		Log.i(TAG, "onImageCaptured - fullFilePath: "+fullFilePath);
		
		setupUi(IMAGE_CONFIRMATION_SCREEN);
		
		Bitmap bitmap = ImageUtil.INSTANCE.getRotatedBitmap(fullFilePath);
		mIvPreview.setImageBitmap(bitmap);
		ImageUtil.INSTANCE.saveBitmapToNewFile(bitmap, fullFilePath);
		
		mIsSavePending = true;
		enableDisableAfterEffectsButtons(true);
		
		mBtnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//String picPath = mCameraHandler.saveImageToFilesystem(data);
				Intent intent = new Intent();
				intent.putExtra(CAMERA_CAPTURED_FILE_FULL_PATH, fullFilePath);
				setResult(RESULT_OK, intent);
				finish();
				mIsSavePending = false;
				enableDisableAfterEffectsButtons(false);
			}
		});
		
		mBtnDiscard.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setupUi(CAMERA_PREVIEW_SCREEN);
				setupCamera();
				
				// Delete this file from filesystem.
				deleteFileAt(fullFilePath);
				mIsSavePending = false;
				enableDisableCaptureButton(true, false);
				enableDisableAfterEffectsButtons(false);
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.alldata.carcue.camera.CameraCallback#onVideoCaptured(java.lang.String)
	 */
	@Override
	public void onVideoCaptured(final String fullFilePath) {
		Log.i(TAG, "onVideoCaptured - fullFilePath: "+fullFilePath);
		enableDisableCaptureButton(true, false);
		mVideoCaptureInProgress = false;
		
		setupUi(VIDEO_CONFIRMATION_SCREEN);
		
		mIvPreview.setImageBitmap(ImageUtil.INSTANCE.createFullscreenVideoThumbnail(fullFilePath));
		mIvPreview.setScaleType(ScaleType.FIT_XY);
		
		mCameraHandler.stopPreview();
		mIsSavePending = true;
		enableDisableAfterEffectsButtons(true);
		
		setVideoTime();
		
		if (timeElapsed < 1) {
			mIsSavePending = false;
			deleteFileAt(fullFilePath);
			Toast.makeText(this, "Unable to save recorded video", Toast.LENGTH_SHORT).show();
		}
		
		mIvMovieIndicator.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					mVideoPlaybackInProgress = true;
					if(fullFilePath != null){
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(Uri.fromFile(new File(fullFilePath)), "video/mp4");
						startActivity(intent);
					}
				} catch (ActivityNotFoundException e) {
					Toast.makeText(getApplicationContext(), "Video player not found on device. Unable to play video", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		});
		
		mBtnSave.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra(CAMERA_CAPTURED_FILE_FULL_PATH, fullFilePath);
				setResult(RESULT_OK, intent);
				finish();
				mIsSavePending = false;
				enableDisableAfterEffectsButtons(false);
			}
		});
		
		mBtnDiscard.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setupUi(CAMERA_PREVIEW_SCREEN);
				setupCamera();
				
				deleteFileAt(fullFilePath);
				mIsSavePending = false;
				enableDisableCaptureButton(true, false);
				enableDisableAfterEffectsButtons(false);
			}
		});
		
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	private void deleteFileAt(String fullFilePath) {
		// Delete this file from filesystem.
		File file = new File(fullFilePath);
		file.delete();
	}

	private int mTimeToRecord = Integer.MAX_VALUE;
	private void setTimeToRecord(int timeToRecordInSeconds) {
		mTimeToRecord = timeToRecordInSeconds > 0 ? timeToRecordInSeconds : Integer.MAX_VALUE;
	}
	
	private int timeElapsed = 0;
	private CountDownTimer mTimer;
	private void startTimer() {
		
		// A countdown timer that counts from 90 seconds till 0.
		mTimer = new CountDownTimer(mTimeToRecord * 1000, 1000) {
			
			@Override
			public void onTick(long millisUntilFinished) {
				timeElapsed = mTimeToRecord - (int) (millisUntilFinished / 1000);
				if (mTimeToRecord == Integer.MAX_VALUE) {
					mTvTimer.setText(getTimerValue(timeElapsed));
				} else {
					mTvTimer.setText(getTimerValue((int) (millisUntilFinished / 1000)));
				}
			}
			
			@Override
			public void onFinish() {
				if (mTimeToRecord == Integer.MAX_VALUE) {
					mTvTimer.setText(getTimerValue(timeElapsed));
				} else {
					mTvTimer.setText(getTimerValue(0));
				}
				timeElapsed = mTimeToRecord;
			}
		};
		mTimer.start();
	}
	
	private String getTimerValue(int seconds) {
		String timerText = "";
		if (seconds >= 60) {
			timerText = "01:" + String.format("%02d", seconds - 60);
		} else {
			timerText = "00:" + String.format("%02d", seconds);
		}
		return timerText;
	}
	
	private void setVideoTime() {
		mTimer.cancel();
		mTvTimer.setText(getTimerValue(timeElapsed));
	}
}


