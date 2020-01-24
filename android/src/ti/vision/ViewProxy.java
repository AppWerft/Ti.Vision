package ti.vision;

import java.io.IOException;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import ti.vision.utilities.OcrDetectorProcessor;

@Kroll.proxy(creatableInModule = VisionModule.class, propertyAccessors = { "onSuccess", "onError" })
public class ViewProxy extends TiViewProxy {
	private static final String LCAT = VisionModule.LCAT;
	private static final int RC_HANDLE_GMS = 9001;
	public static final String AutoFocus = "AutoFocus";
	public static final String UseFlash = "UseFlash";

	public static final String TextBlockObject = "String";

	protected View nativeView; 
    KrollProxy proxy;
	private CameraSource cameraSource;
	private CameraSourcePreview cameraPreview;
	private GraphicOverlay<OcrGraphic> graphicOverlay;
	private KrollFunction onSuccess;

	boolean autoFocus = true;
	boolean useFlash = false;
	float zoom = 1.0f;
	int type = VisionModule.TYPE_OCR;
	public String regex;
	public KrollDict textbox;

	int facing = CameraSource.CAMERA_FACING_BACK;
	float requestedFps = 2.0f;
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
			this.proxy=proxy;
			Log.d(LCAT,"Constructor VisionView");
			LinearLayout container = new LinearLayout(proxy.getActivity());
			LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			container.setLayoutParams(lp);
			container.addView(cameraPreview);
			cameraPreview.setOnTouchListener(touchListener);
			setNativeView(container);
		}

		@Override
		public void processProperties(KrollDict d) {
			super.processProperties(d);
		}
	}

	// Constructor
	public ViewProxy() {
		super();
	}

	public View getNativeView() {
		return nativeView;
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
			if (opts.containsKeyAndNotNull("textbox")) {
				textbox = opts.getKrollDict("textbox");
			}
			if (opts.containsKeyAndNotNull("regex")) {
				regex = opts.getString("regex");
			}
			if (opts.containsKeyAndNotNull("zoom")) {
				zoom = TiConvert.toFloat(opts.get("zoom"));
			}
			if (opts.containsKeyAndNotNull("onsuccess")) {
				onSuccess = (KrollFunction) (opts.get("onsuccess"));
			}
		}
		start();
	}

	
	@Kroll.method
	public void start() {
		graphicOverlay = new GraphicOverlay(ctx,null);
		cameraPreview = new CameraSourcePreview(ctx,null);
		cameraPreview.addView(graphicOverlay);

		tapDetector = new GestureDetector(ctx, new TapListener());
		pinchDetector = new ScaleGestureDetector(ctx, new PinchListener());

		Log.d(LCAT, "graphicOverlay + cameraPreview created CameraDistance=" + graphicOverlay.getCameraDistance());
		this.createCameraSource();
		Log.d(LCAT, "createCameraSource()");
		if (cameraSource != null) {
			try {
				cameraPreview.start(cameraSource, graphicOverlay);
			} catch (IOException e) {
				Log.e(LCAT, e.getLocalizedMessage());
				cameraSource.release();
				cameraSource = null;
			}
		}
		// tapDetector = new GestureDetector(new TapListener());
		TextRecognizer textRecognizer = new TextRecognizer.Builder(ctx).build();
		textRecognizer.setProcessor(new OcrDetectorProcessor(ViewProxy.this,graphicOverlay));
		cameraSource = new CameraSource.Builder(ctx, textRecognizer).setFacing(CameraSource.CAMERA_FACING_BACK)
				.setRequestedPreviewSize(1280, 1024).setRequestedFps(2.0f)
				.setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
				.setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null).build();

	}

	private void startCameraSource() throws SecurityException {

		if (cameraSource != null) {
			try {
				cameraPreview.start(cameraSource, graphicOverlay);
			} catch (IOException e) {
				Log.e(LCAT, "Unable to start camera source.", e);
				cameraSource.release();
				cameraSource = null;
			}
		}
	}

	private boolean onTextBlockTapped(float rawX, float rawY) {
		Log.d(LCAT, "onTextBlockTapped " + rawX + "X" + rawY);
		OcrGraphic graphic = graphicOverlay.getGraphicAtLocation(rawX, rawY);
		TextBlock text = null;
		if (graphic != null) {
			text = graphic.getTextBlock();
			if (text != null && text.getValue() != null) {
				cameraPreview.stop();
				KrollDict dict = new KrollDict();
				dict.put("text", text.getValue());
				if (hasProperty("onSuccess")) {
					onSuccess = (KrollFunction) (getProperty("onSuccess"));
					onSuccess.callAsync(getKrollObject(), dict);
				}
				if (hasListeners("success")) {
					fireEvent("success", dict);
				}
				if (onSuccess != null)
					onSuccess.callAsync(getKrollObject(), dict);
			}
		}
		return text != null;
	}

	private void createCameraSource() {
		TextRecognizer textRecognizer = new TextRecognizer.Builder(ctx).build();
		textRecognizer.setProcessor(new OcrDetectorProcessor(ViewProxy.this,graphicOverlay));
		this.cameraSource = new CameraSource.Builder(ctx, textRecognizer).setFacing(CameraSource.CAMERA_FACING_BACK)
				.setRequestedPreviewSize(1280, 1024).setRequestedFps(2.0f)
				.setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
				.setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null).build();
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
		if (cameraPreview != null) {
			cameraPreview.stop();
		}
	}
}
