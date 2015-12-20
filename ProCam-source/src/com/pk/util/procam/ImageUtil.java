/*
 * Copyright (c) 2013 ALLDATA, LLC. All rights reserved.
 * Created on : 02 August 2013
 * FileName: ImageUtil.java 
 */
package com.pk.util.procam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Describes the Images and videos of the properties (width and height) here.
 * 
 */
public enum ImageUtil {

	INSTANCE;
	
	private final String TAG = getClass().getSimpleName();
	private final String JPEG_FILE_PREFIX = "IMG_";
	private final String JPEG_FILE_SUFFIX = ".jpg";

	/**
	 * Creates a thumbnail of the file (image or video) given. Size restriction
	 * of image thumbnail is given at 128*128 for now.
	 * 
	 * @param isVideo
	 *            - whether the file mentioned in path is a video or a image.
	 * @param path
	 *            - Path to the file for which thumbnail has to be created.
	 * @return - Bitmap object containing the thumbnail.
	 */
	public Bitmap createThumbnails(boolean isVideo, String path) {
		Bitmap thumb;
		if (isVideo) {
			thumb = ThumbnailUtils.createVideoThumbnail(path,
					MediaStore.Images.Thumbnails.MICRO_KIND);
		} else {
			thumb = optimizeBitmap(path, 128, 128);
		}
		return thumb;
	}

	/**
	 * Creates a full screen thumbnail of the video mentioned in path. Useful
	 * for showing video preview in lightbox.
	 * 
	 * @param path
	 *            - Path to the file for which thumbnail has to be created.
	 * @return - Bitmap object containing the thumbnail.
	 */
	public Bitmap createFullscreenVideoThumbnail(String path) {
		return ThumbnailUtils.createVideoThumbnail(path,
				MediaStore.Images.Thumbnails.MINI_KIND);
	}

	/**
	 * Read a file from path and create a image bitmap of the specified size Use
	 * optimizeBitmap. Unless inSampleSize needs to be calculated manually, do
	 * not use this function
	 * 
	 * @param path
	 *            - Path to the file
	 * @param reqWidth
	 *            - the width of the result image bitmap
	 * @param reqHeight
	 *            - the height of the result image bitmap
	 * @return - Bitmap with size equivalent to specified width and height
	 */
	public Bitmap createBitmapFromPath(String path, int reqWidth, int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(path, options);
	}

	public int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			if (width > height) {
				inSampleSize = Math.round((float) height / (float) reqHeight);
			} else {
				inSampleSize = Math.round((float) width / (float) reqWidth);
			}
		}
		return inSampleSize;
	}

	/**
	 * There isn't enough memory to open up more than a couple camera photos So
	 * pre-scale the target bitmap into which the file is decoded
	 * 
	 * @param path
	 *            - path to the picture
	 * @param targetW
	 *            - target width of the bitmap
	 * @param targetH
	 *            - target height of the bitmap
	 * @return
	 */
	public Bitmap optimizeBitmap(String path, int targetW, int targetH) {

		/* Get the size of the image */
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		/* Figure out which way needs to be reduced less */
		int scaleFactor = 1;
		if ((targetW > 0) || (targetH > 0)) {
			scaleFactor = Math.min(photoW / targetW, photoH / targetH);
		}

		/* Set bitmap options to scale the image decode target */
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		/* Decode the JPEG file into a Bitmap */
		return BitmapFactory.decodeFile(path, bmOptions);
	}

	public Bitmap getBitmapFromAsset(Context context, String filename) {
		AssetManager assetManager = context.getAssets();
		InputStream istr = null;
		try {
			istr = assetManager.open(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Bitmap bitmap = BitmapFactory.decodeStream(istr);
		return bitmap;
	}

	/**
	 * Static function to get an Image storage directory in case of problems
	 * with default camera application.
	 * 
	 * @return - File to store the image or video data.
	 */
	public File getImageStorageDirectory() {

		File storageDir = null;

		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())) {

			String albumName = "USB HERMES";
			storageDir = new File(
					Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
					albumName);

			if (storageDir != null) {
				if (!storageDir.mkdirs()) {
					if (!storageDir.exists()) {
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
		} else {
			return null;
		}
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
				.format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		try {
			return File.createTempFile(imageFileName, JPEG_FILE_SUFFIX,
					storageDir);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Bitmap getRotatedBitmap(String fullFilePath) {
		int exifOrientation = 0;
		ExifInterface exif;
		try {
			exif = new ExifInterface(fullFilePath);
			exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int rotate = 0;
		switch (exifOrientation) {
		case ExifInterface.ORIENTATION_ROTATE_90:
			rotate = 90;
			break;

		case ExifInterface.ORIENTATION_ROTATE_180:
			rotate = 180;
			break;

		case ExifInterface.ORIENTATION_ROTATE_270:
			rotate = 270;
			break;
		}

		Bitmap bitmap = ImageUtil.INSTANCE.createBitmapFromPath(fullFilePath, 1280, 720);
		Log.i(TAG, "getRotatedBitmap rotation in Image: "+rotate);
		if (rotate != 0) {
			int w = bitmap.getWidth();
			int h = bitmap.getHeight();

			// Setting pre rotate
			Matrix mtx = new Matrix();
			mtx.preRotate(rotate);

			// Rotating Bitmap & convert to ARGB_8888, required by tess
			bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
			bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
		}
		
		return bitmap;
	}
	
	public void saveBitmapToNewFile(Bitmap bitmap, String filePath) {
		File file = new File(filePath);
		file.delete();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			FileOutputStream out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}