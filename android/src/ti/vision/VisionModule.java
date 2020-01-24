/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2018 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.vision;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import android.app.Dialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.text.TextRecognizer;
import com.teapink.ocr_reader.ui.camera.CameraSource;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.util.Log;

@Kroll.module(name = "Vision", id = "ti.vision")
public class VisionModule extends KrollModule {
	private Context ctx = TiApplication.getAppRootOrCurrentActivity().getApplicationContext();
	 // Intent request code to handle updating play services if needed.
	public static final String LCAT = "TiOCR";
    private static final int RC_HANDLE_GMS = 9001;
    @Kroll.constant
	public static final int TYPE_OCR = 1;
    @Kroll.constant
   	public static final int TYPE_FACE = 2;
    @Kroll.constant
   	public static final int TYPE_BARCODE = 3;
	@Kroll.constant
	public static final int CAMERA_FACING_BACK = CameraSource.CAMERA_FACING_BACK;
	
	@Kroll.constant
	public static final int CAMERA_FACING_FRONT = CameraSource.CAMERA_FACING_FRONT;
	@Kroll.constant 
	public static final int GOOGLE_PLAY_SERVICES_VERSION_CODE = GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE;

	public VisionModule() {
		super();

	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {

	}
	
    @Kroll.method 
    public boolean isGooglePlayServicesAvailable() {
    	int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx);
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(TiApplication.getAppRootOrCurrentActivity(), code, RC_HANDLE_GMS);
            dlg.show();
            return false;
        } else return true;
    }
    
    @Kroll.method
    public boolean isZoomSupported() {
    	
		Camera camera = Camera.open();
    	if (camera.getParameters()!=null) {
    		return camera.getParameters().isZoomSupported();
    	}
    	return false;
    }
    
    @Kroll.method
    public int getMaxZoom() {
    	Camera camera = Camera.open();
    	if (camera.getParameters()!=null) {
    		return camera.getParameters().getMaxZoom();
    	}
    	return -1;
    }
    
	@Kroll.method
	public boolean isOperational() {
		Context context = TiApplication.getAppRootOrCurrentActivity().getApplicationContext();
		TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
		if (!textRecognizer.isOperational()) {
			IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
			boolean hasLowStorage = context.registerReceiver(null, lowstorageFilter) != null;

			if (hasLowStorage) {
				return false;
			}
		}
		return true;
	}
}