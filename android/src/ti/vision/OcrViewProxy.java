package ti.vision;

import java.io.IOException;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.teapink.ocr_reader.ui.camera.CameraSource;
import com.teapink.ocr_reader.ui.camera.CameraSourcePreview;
import com.teapink.ocr_reader.ui.camera.GraphicOverlay;
import com.teapink.ocr_reader.utilities.OcrGraphic;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Message;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import ti.vision.utilities.OcrDetectorProcessor;

@Kroll.proxy(creatableInModule = VisionModule.class, propertyAccessors = { "onClick", "onError" })
public class OcrViewProxy extends TiViewProxy {
	private static final String LCAT = VisionModule.LCAT;
	private static final int RC_HANDLE_GMS = 9001;
	public static final String AutoFocus = "AutoFocus";
	public static final String UseFlash = "UseFlash";

	public static final String TextBlockObject = "String";
	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;
	private static final int MSG_START = MSG_FIRST_ID + 500;
	private static final int MSG_STOP = MSG_FIRST_ID + 501;
	private static final int MSG_TORCH_ON = MSG_FIRST_ID + 502;
	private static final int MSG_TORCH_OFF = MSG_FIRST_ID + 503;
	private static final int MSG_RELEASE = MSG_FIRST_ID + 504;
	

	protected View nativeView; 
   
    
	private CameraSource cameraSource;
	private CameraSourcePreview preview;
	
	private GraphicOverlay<OcrGraphic> graphicOverlay;
	private KrollFunction onClick;

	boolean autoFocus = true;
	boolean useFlash = false;
	float zoom = 1.0f;
	float fps = 20.0f;
	int type = VisionModule.TYPE_OCR;
	public String regex;
	public KrollDict textbox;

	int facing = CameraSource.CAMERA_FACING_BACK;
	
	Context ctx = TiApplication.getAppRootOrCurrentActivity().getApplicationContext();

	private GestureDetector tapDetector;
	private ScaleGestureDetector pinchDetector;
	View.OnTouchListener touchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			boolean b = pinchDetector.onTouchEvent(event);
			boolean c = tapDetector.onTouchEvent(event);
			return b || c;
		}
	};

	private class VisionView extends TiUIView {
		public VisionView(TiViewProxy proxy) {
			super(proxy);
			Log.d(LCAT,"Constructor VisionView");
			LinearLayout container = new LinearLayout(proxy.getActivity());
			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			container.setLayoutParams(lp);
			setNativeView(container);
		}

		@Override
		public void processProperties(KrollDict d) {
			super.processProperties(d);
		}
	}

	// Constructor
	public OcrViewProxy() {
		super();
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		AsyncResult result = null;
		switch (msg.what) {
		
		case MSG_START: {
			result = (AsyncResult) msg.obj;
			handleStart();
			result.setResult(null);
			return true;
		}
		case MSG_STOP: {
			result = (AsyncResult) msg.obj;
			handleStop();
			result.setResult(null);
			return true;
		}
		case MSG_TORCH_ON: {
			result = (AsyncResult) msg.obj;
			handleTorch(true);
			result.setResult(null);
			return true;
		}
		case MSG_TORCH_OFF: {
			result = (AsyncResult) msg.obj;
			handleTorch(false);
			result.setResult(null);
			return true;
		}
		default: {
			return super.handleMessage(msg);
		}
		}
	}
	public View getNativeView() {
		return nativeView;
	}
	
	@Override
	public void release(){
		Log.d(LCAT,"OCrViewProxy release()");
		if (preview != null) {
			preview.stop();
		}
		super.release();
	}

	@Kroll.method
	public void start() {
		if (TiApplication.isUIThread()) 
		handleStart(); 
		else TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_START));
	}
	@Kroll.method
	public void stop() {
		if (TiApplication.isUIThread()) 
		handleStop(); 
		else TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_STOP));
	}
	@Kroll.method
	public void enableTorch() {
		if (TiApplication.isUIThread()) 
		handleTorch(true); 
		else TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_TORCH_ON));
	}
	@Kroll.method
	public void disableTorch() {
		if (TiApplication.isUIThread()) 
		handleTorch(false); 
		else TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_TORCH_OFF));
	}
	
	
	@Override
	public TiUIView createView(Activity activity) {
		TiUIView view = new VisionView(this);
		Log.d(LCAT,"createView");
		view.getLayoutParams().autoFillsHeight = true;
		view.getLayoutParams().autoFillsWidth = true;
		return view;
	}

	@Override
	public void handleCreationDict(@Kroll.argument(optional = true) KrollDict opts) {
		super.handleCreationDict(opts);
		if (opts != null) {
			if (opts.containsKeyAndNotNull("autofocus")) {
				autoFocus = opts.getBoolean("autofocus");
			}
			if (opts.containsKeyAndNotNull("flash")) {
				useFlash = opts.getBoolean("flash");
			}
			if (opts.containsKeyAndNotNull("torch")) {
				useFlash = opts.getBoolean("torch");
			}
			if (opts.containsKeyAndNotNull("regex")) {
				regex = opts.getString("regex");
			}
			if (opts.containsKeyAndNotNull("fps")) {
				fps = TiConvert.toFloat(opts.get("fps"));
			}
			if (opts.containsKeyAndNotNull("onclick")) {
				onClick = (KrollFunction) (opts.get("onclick"));
			}
		}
	}

	@Kroll.method
	public void handleStop() {
		
		
	}
	
	@Kroll.method
	public void handleTorch(boolean flash) {
		cameraSource.setFlashMode(flash ? Camera.Parameters.FLASH_MODE_TORCH:null);
		
	}
	
	@Kroll.method
	public void handleStart() {
		Log.d(LCAT,">>>>>>>>>>>>>>>> handleStart");
		/* graphicOverlay holds all captured texts*/
		graphicOverlay = new GraphicOverlay(ctx,null);
		/* preview is container for camera proview*/
		preview = new CameraSourcePreview(ctx,null);
		/* graphicOverlay will added */
		preview.addView(graphicOverlay);
		Log.d(LCAT,"graphicOverlay added to created preview");
		tapDetector = new GestureDetector(ctx, new TapListener());
		pinchDetector = new ScaleGestureDetector(ctx, new PinchListener());

		//Log.d(LCAT, "graphicOverlay + cameraPreview created CameraDistance=" + graphicOverlay.getCameraDistance());

		createCameraSourceWithRecognizerAndProcessor();
		startCameraSource();
	
	}

	private void createCameraSourceWithRecognizerAndProcessor() {
		Log.d(LCAT,"createCameraSourceWithRecognizerAndProcessor()");
		TextRecognizer textRecognizer = new TextRecognizer.Builder(ctx).build();
		textRecognizer.setProcessor(new OcrDetectorProcessor(OcrViewProxy.this,graphicOverlay));
		this.cameraSource = new CameraSource.Builder(ctx, textRecognizer).setFacing(CameraSource.CAMERA_FACING_BACK)
				.setRequestedPreviewSize(1280, 1024).setRequestedFps(fps)
				.setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
				.setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null).build();
	}
	
	private void startCameraSource() throws SecurityException {
		Log.d(LCAT,"startCameraSource");
		if (cameraSource != null) {
			try {
				preview.start(cameraSource, graphicOverlay);
			} catch (IOException e) {
				Log.e(LCAT, "Unable to start camera source.", e);
				cameraSource.release();
				cameraSource = null;
			}
		} else Log.d(LCAT,"cameraSource was null, cannot restart camera");
	}

	private boolean onTextBlockTapped(float rawX, float rawY) {
		Log.d(LCAT, "onTextBlockTapped " + rawX + "X" + rawY);
		OcrGraphic graphic = graphicOverlay.getGraphicAtLocation(rawX, rawY);
		TextBlock text = null;
		if (graphic != null) {
			text = graphic.getTextBlock();
			if (text != null && text.getValue() != null) {
				preview.stop();
				KrollDict dict = new KrollDict();
				dict.put("text", text.getValue());
				if (hasProperty("onClick")) {
					onClick = (KrollFunction) (getProperty("onClick"));
					onClick.callAsync(getKrollObject(), dict);
				}
				if (hasListeners("click")) {
					fireEvent("click", dict);
				}
				if (onClick != null)
					onClick.callAsync(getKrollObject(), dict);
			}
		}
		return text != null;
	}

	private class TapListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			Log.d(LCAT, "onSingleTapConfirmed");
			return onTextBlockTapped(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
		}
	}

	private class PinchListener implements ScaleGestureDetector.OnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			return false;
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			float scaleFactor = detector.getScaleFactor();
			cameraSource.doZoom(scaleFactor);
			KrollDict res = new KrollDict();
			res.put("zoom", scaleFactor);
			res.put("distance", graphicOverlay.getCameraDistance());
			if (hasListeners("zoomchange"))
				fireEvent("zoomchange", res);

		}
	}

	@Override
	public void onResume(Activity activity) {
		super.onResume(activity);
		startCameraSource();
	}

	@Override
	public void onPause(Activity activity) {
		super.onPause(activity);
		if (preview != null) {
			preview.stop();
		}
	}
	@Override
	public void onDestroy(Activity activity) {
		super.onDestroy(activity);
		if (preview != null) {
			preview.stop();
		}
	}
}
