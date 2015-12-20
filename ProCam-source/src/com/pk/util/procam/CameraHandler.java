/*
 * Copyright (c) 2013 ALLDATA, LLC. All rights reserved.
 * Created on : 02 August 2013
 * FileName: CameraHandler.java 
 */
package com.pk.util.procam;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;


/**
 * Handles all camera operations.
 * Requires CAMERA permission for Image and Video capture.
 * Requires RECORD_AUDIO permission for Video Capture
 */
public class CameraHandler {

	private final String TAG = getClass().getSimpleName();
	
	/** The m context. */
	private Context mContext;
	
	/** The m camera. */
	private Camera mCamera;
	
	/** The m media recorder. */
	private MediaRecorder mMediaRecorder = null;
	
	/** The m video properties. */
	private VideoProperties mVideoProperties = null;
	
	/** The m camera preview. */
	private ViewGroup mCameraPreview;
	
	/** The m camera surface. */
	private CameraSurface mCameraSurface;
	
	/** The m callback. */
	private CameraCallback mCallback;
	
	/** The m image storage path. */
	private String mImageStoragePath;
	
	/** The m video storage path. */
	private String mVideoStoragePath;
	
	/** The m camera id. */
	private int mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;		// Default - Back facing camera since most devices have it.
	
	private String BUILD_MANUFACTURER = Build.MANUFACTURER;
	private String BUILD_MODEL = Build.MODEL;

	/**
	 * Instantiates a new camera handler.
	 *
	 * @param context the context
	 * @param callback the callback
	 * @throws ClassNotFoundException the class not found exception
	 */
	public CameraHandler (Context context, CameraCallback callback) throws ClassNotFoundException {
		mContext = context;
		if (!deviceHasCamera()) {
			throw new ClassNotFoundException("Camera unavailable in device");
		}
		
		openCamera();
		mCallback = callback;
		Log.i(TAG, "Constructor - manufacturer: "+BUILD_MANUFACTURER+" modeL: "+BUILD_MODEL);
	}
	
	/**
	 * Open camera.
	 *
	 * @throws ClassNotFoundException the class not found exception
	 */
	private void openCamera() throws ClassNotFoundException {
		try {
			Log.i(TAG, "openCamera - cameraID: "+mCameraId);
			mCamera = Camera.open(mCameraId);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ClassNotFoundException("Unable to instantiate Camera");
		}
	}
	
	/**
	 * Device has camera.
	 *
	 * @return true, if successful
	 */
	public boolean deviceHasCamera() {
		return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
	}
	
	/**
	 * Gets the camera.
	 *
	 * @return the camera
	 */
	public Camera getCamera() {
		return mCamera;
	}
	
	/**
	 * Switch camera.
	 */
	public void switchCamera() {
		if (mCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
			mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
		} else {
			mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
		}
		
		mCamera.release();
		
		try {
			openCamera();
			showCameraPreview(mCameraPreview);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Show camera preview.
	 *
	 * @param cameraPreviewLayout the camera preview layout
	 */
	public void showCameraPreview(ViewGroup cameraPreviewLayout) {
		Log.i("CameraHandler", "showCameraPreview");
		if (mCameraSurface == null) {
			mCameraPreview = cameraPreviewLayout;
			mCameraSurface = new CameraSurface(mContext);
			mCameraPreview.removeAllViews();
			mCameraPreview.addView(mCameraSurface);
		}
		mCamera.setDisplayOrientation(getCameraDisplayOrientation());
		try {
			mCamera.setPreviewDisplay(mCameraSurface.getSurfaceHolder());
		} catch (IOException e) {
			e.printStackTrace();
		}
		mCamera.startPreview();
		if (mMediaRecorder != null) {
			try {
				mVideoProperties = null;
				mMediaRecorder.release();
				mMediaRecorder = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Release camera.
	 */
	public void releaseCamera() {
		if (mCamera != null) {
			try {
				stopPreview();
				mCamera.release();
				mCamera = null;
				mCameraPreview.removeAllViews();
				mCameraSurface = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Stop preview.
	 */
	public void stopPreview() {
		if (mCamera != null) {
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Checks if is front facing camera.
	 *
	 * @return true, if is front facing camera
	 */
	public boolean isFrontFacingCamera() {
		return mCameraId == CameraInfo.CAMERA_FACING_FRONT;
	}
	
	/**
	 * Take picture.
	 */
	public void takePicture() {
		if (mCamera != null) {
			mCamera.setParameters(getParams());
			mCamera.takePicture(null, null, new Camera.PictureCallback() {
				@Override
				public void onPictureTaken(byte[] data, Camera camera) {
					// Camera preview is stopped. Re-enable if needed by calling
//					showCameraPreview(mCameraPreview);

					String path = saveImageToFilesystem(data);
					if (mCallback != null) {
						mCallback.onImageCaptured(path);
					}
				}
			});
		}
	}
	
	/**
	 * Sets the video properties.
	 *
	 * @param properties the new video properties
	 */
	public void setVideoProperties(VideoProperties properties) {
		mVideoProperties = properties;
	}
	
	/**
	 * Start video recording.
	 */
	public void startVideoRecording() {
		if (mCamera == null) {
			return;
		}
		
		mCamera.unlock();
		prepareMediaRecorder();
	}
	
	/**
	 * Prepare media recorder.
	 */
	private void prepareMediaRecorder() {
		if (mMediaRecorder == null) {
			mMediaRecorder = new MediaRecorder();
			if (mVideoProperties == null) {
				mVideoProperties = new VideoProperties();
			}
			mVideoProperties.setPropertiesToMediaRecorder(mMediaRecorder);
		}
		try {
			mMediaRecorder.setPreviewDisplay(mCameraSurface.getSurfaceHolder().getSurface());
			mMediaRecorder.prepare();
			mMediaRecorder.start();
			mMediaRecorder.setOnInfoListener(infoListener);
			if (mCallback != null) {
				mCallback.onVideoCaptureStarted();
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
			mMediaRecorder.release();
		} catch (IOException e) {
			e.printStackTrace();
			mMediaRecorder.release();
		}
	}
	
	/**
	 * Stop video recording.
	 * @throws Exception 
	 */
	public void stopVideoRecording() throws Exception {
		try {
			if (mMediaRecorder != null) {
				mMediaRecorder.stop();
				mMediaRecorder.release();
				mMediaRecorder = null;
				if (mCamera != null) {
					mCamera.lock();
				}
				if (mCallback != null) {
					mCallback.onVideoCaptured(mVideoProperties.getPath());
				}
				mVideoProperties = null;
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (e instanceof RuntimeException) throw e;
		}
	}
	
	/**
	 * Gets the camera display orientation.
	 *
	 * @return the camera display orientation
	 */
	public int getCameraDisplayOrientation() {
	     android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
	     android.hardware.Camera.getCameraInfo(mCameraId, info);
	     int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
	     int degrees = 0;
	     switch (rotation) {
	         case Surface.ROTATION_0: degrees = 0; break;
	         case Surface.ROTATION_90: degrees = 90; break;
	         case Surface.ROTATION_180: degrees = 180; break;
	         case Surface.ROTATION_270: degrees = 270; break;
	     }

	     int result;
	     if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
	         result = (info.orientation + degrees) % 360;
	         result = (360 - result) % 360;  // compensate the mirror
	     } else {  // back-facing
	         result = (info.orientation - degrees + 360) % 360;
	     }
	     Log.i(TAG, "getCameraDisplayOrientation - rotation: "+result);
	     return result;
	 }
	
	/**
	 * Allows the app to save the captured images and videos to a location
	 * of their choice in the SD card.
	 *
	 * @param path - relative path under SD card where the media content is stored
	 */
	public void setImageStoragePath(String path) {
		mImageStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + path;
	}

	/**
	 * Sets the video storage path.
	 *
	 * @param path the new video storage path
	 */
	public void setVideoStoragePath(String path) {
		mVideoStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + path;
	}
	
	/**
	 * Save image to filesystem.
	 *
	 * @param data the data
	 * @return the string
	 */
	public String saveImageToFilesystem(byte[] data) {
		File picture = getMediaFile(true);		// Get image file container.
		if (picture == null) {
			return null;
		}
		try {
			FileOutputStream stream = new FileOutputStream(picture);
			stream.write(data);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return picture.getAbsolutePath();
	}
	
	/**
	 * Gets the media file.
	 *
	 * @param isImage the is image
	 * @return the media file
	 */
	private File getMediaFile(boolean isImage) {
		File mediaDir = null;
		Log.i(TAG, "getMediaFile - mMediaStorageDir: "+mImageStoragePath);
		
		if (isImage) {
			if (mImageStoragePath != null && mImageStoragePath.length() > 0) {
				mediaDir = new File(mImageStoragePath);
			} else {
				mImageStoragePath = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
				mediaDir = new File(Environment.getExternalStorageDirectory(), mImageStoragePath);
				mImageStoragePath = mediaDir.getAbsolutePath();
			}
		} else {
			if (mVideoStoragePath != null && mVideoStoragePath.length() > 0) {
				mediaDir = new File(mVideoStoragePath);
			} else {
				mVideoStoragePath = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
				mediaDir = new File(Environment.getExternalStorageDirectory(), mVideoStoragePath);
				mVideoStoragePath = mediaDir.getAbsolutePath();
			}
		}

		if (!mediaDir.exists()) {
			if (!mediaDir.mkdirs()) {
				Log.i(TAG, "getMediaFile - returning NULL");
				return null;
			}
		}
		
		String filename = "";
		String timestamp = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.US).format(new Date());
		File mediaFile;
		if (isImage) {
			filename = "PIC_";
			mediaFile = new File(mediaDir.getPath() + File.separator+ filename + timestamp + ".jpg");
		} else {
			filename = "VID_";
			mediaFile = new File(mediaDir.getPath() + File.separator+ filename + timestamp + ".mp4");
		}
		
		return mediaFile;
	}
	
	/** The info listener. */
	private MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
		@Override
		public void onInfo(MediaRecorder mr, int what, int extra) {
			if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
				try {
					stopVideoRecording();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	/**
	 * The Class CameraSurface.
	 */
	class CameraSurface extends SurfaceView implements SurfaceHolder.Callback {

		/** The m surface holder. */
		private SurfaceHolder mSurfaceHolder;
		
		/**
		 * Instantiates a new camera surface.
		 *
		 * @param context the context
		 */
		public CameraSurface(Context context) {
			super(context);
			
			mSurfaceHolder = getHolder();
			mSurfaceHolder.addCallback(this);
			mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		/* (non-Javadoc)
		 * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
		 */
		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			try {
				mCamera.setPreviewDisplay(mSurfaceHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/* (non-Javadoc)
		 * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
		 */
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			if (mSurfaceHolder.getSurface() == null) {
				return;
			}
			
			stopPreview();
			
			try {
				mCamera.setParameters(getParams());
				showCameraPreview(mCameraPreview);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/* (non-Javadoc)
		 * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
		 */
		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			releaseCamera();
		}
		
		/**
		 * Gets the surface holder.
		 *
		 * @return the surface holder
		 */
		public SurfaceHolder getSurfaceHolder() {
			return mSurfaceHolder;
		}
	}
	
	private Parameters getParams() {
		Parameters params = mCamera.getParameters();
		params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
		params.setJpegQuality(100);
		if (!BUILD_MODEL.equalsIgnoreCase("HTC One X")) {
			params.setRotation(getCameraDisplayOrientation());
		}
		params.set("cam_mode", 1);
		return params;
	}

	/**
	 * The Class VideoProperties.
	 */
	public class VideoProperties {
		
		/** The default video width. */
		private final int DEFAULT_VIDEO_WIDTH = 640;
		
		/** The default video height. */
		private final int DEFAULT_VIDEO_HEIGHT = 480;
		
		/** The default video framerate. */
		private final int DEFAULT_VIDEO_FRAMERATE = 24;
		
		/** The default negative int. */
		private final int DEFAULT_NEGATIVE_INT = -1;
		
		/** The path. */
		private String path;
		
		/** The frame rate. */
		private int frameRate;
		
		/** The width. */
		private int width;
		
		/** The height. */
		private int height;
		
		/** The max duration ms. */
		private int maxDurationMs;
		
		/** The max file size bytes. */
		private long maxFileSizeBytes;
		
		/**
		 * Sets the path.
		 *
		 * @param path the path
		 * @return the video properties
		 */
		public VideoProperties setPath(String path) {
			this.path = path;
			return this;
		}
		
		/**
		 * Sets the frame rate.
		 *
		 * @param frameRate the frame rate
		 * @return the video properties
		 */
		public VideoProperties setFrameRate(int frameRate) {
			this.frameRate = frameRate;
			return this;
		}
		
		/**
		 * Sets the width.
		 *
		 * @param width the width
		 * @return the video properties
		 */
		public VideoProperties setWidth(int width) {
			this.width = width;
			return this;
		}
		
		/**
		 * Sets the height.
		 *
		 * @param height the height
		 * @return the video properties
		 */
		public VideoProperties setHeight(int height) {
			this.height = height;
			return this;
		}
		
		/**
		 * Sets the max duration ms.
		 *
		 * @param maxDurationMs the max duration ms
		 * @return the video properties
		 */
		public VideoProperties setMaxDurationMs(int maxDurationMs) {
			this.maxDurationMs = maxDurationMs;
			return this;
		}
		
		/**
		 * Sets the max file size bytes.
		 *
		 * @param maxFileSizeBytes the max file size bytes
		 * @return the video properties
		 */
		public VideoProperties setMaxFileSizeBytes(long maxFileSizeBytes) {
			this.maxFileSizeBytes = maxFileSizeBytes;
			return this;
		}
		
		/**
		 * Gets the path.
		 *
		 * @return the path
		 */
		public String getPath() {
			return path;
		}
		
		/**
		 * Sets the properties to media recorder.
		 *
		 * @param mediaRecorder the new properties to media recorder
		 */
		protected void setPropertiesToMediaRecorder(MediaRecorder mediaRecorder) {
			// Populate the assumed/defaulted values.
			mediaRecorder.setCamera(mCamera);
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
			
//			mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
			
			// Now is the turn for user opted settings
			if (path != null && path.length() > 0) {
				mediaRecorder.setOutputFile(path);
			} else {
				path = getMediaFile(false).getAbsolutePath();
				mediaRecorder.setOutputFile(path);
			}
			
			if (!BUILD_MODEL.equalsIgnoreCase("HTC One X")) {
				mediaRecorder.setOrientationHint(getCameraDisplayOrientation());
				mediaRecorder.setVideoFrameRate(frameRate > 0 ? frameRate : DEFAULT_VIDEO_FRAMERATE);
				mediaRecorder.setVideoEncodingBitRate(7 * 1024 * 1024);
				mediaRecorder.setVideoSize(width > 0 ? width : DEFAULT_VIDEO_WIDTH, height > 0 ? height : DEFAULT_VIDEO_HEIGHT);
			}
			
			// Max duration for video recording. Negative or zero indicates no limit.
			mediaRecorder.setMaxDuration(maxDurationMs > 0 ? maxDurationMs : DEFAULT_NEGATIVE_INT);
			// Max file size of the video recorded. Negative or zero indicates no limit.
			mediaRecorder.setMaxFileSize(maxFileSizeBytes > 0 ? maxFileSizeBytes : DEFAULT_NEGATIVE_INT);	// negative or zero indicates no limit.
		}
	}
	
}
