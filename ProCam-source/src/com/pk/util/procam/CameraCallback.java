/*
 * Copyright (c) 2013 ALLDATA, LLC. All rights reserved.
 * Created on : 02 August 2013
 * FileName: CameraCallback.java 
 */
package com.pk.util.procam;

/**
 * The Interface CameraCallback.
 */
public interface CameraCallback {
	
	/**
	 * On image captured.
	 *
	 * @param fullFilePath the full file path
	 */
	public void onImageCaptured(String fullFilePath);
	
	/**
	 * On video capture started.
	 */
	public void onVideoCaptureStarted();
	
	/**
	 * On video captured.
	 *
	 * @param fullFilePath the full file path
	 */
	public void onVideoCaptured(String fullFilePath);
}
