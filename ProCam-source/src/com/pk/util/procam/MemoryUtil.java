package com.pk.util.procam;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class MemoryUtil {

	public static boolean isExternalStorageAvailable() {
		return isSufficientMemoryAvailable();
	}
	
	public static boolean isExternalStorageMounted() {
		String state = android.os.Environment.getExternalStorageState();
		Log.i("MemoryUtil", "isExternalStorageAvailable - state: "+state);
		if (!state.equals(android.os.Environment.MEDIA_MOUNTED)
				|| state.equals(android.os.Environment.MEDIA_MOUNTED_READ_ONLY)) {
			return false;
		}

		return true;
	}
	
	private static final int MINIMUM_SD_CARD_MEMORY = 50;	// in megabytes
	public static boolean isSufficientMemoryAvailable() {
		if (isExternalStorageMounted()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();

            long availableMemInBytes = availableBlocks * blockSize;
            int availableMemInMB = (int) (availableMemInBytes / (1024 * 1024)); 	// Available memory is in bytes

            Log.i("MemoryUtil", "isSufficientMemoryAvailable - availableMemInBytes: "+availableMemInBytes+" availableMemInMB: "+availableMemInMB);
            
            if (availableMemInMB < MINIMUM_SD_CARD_MEMORY) {
            	return false;
            } else {
            	return true;
            }
        }
		
		return false;
	}
	
}
