# Ti.OCRvision

Axway Titanium module for OCR in preview camera.

## Manifest

Look into `timodule.xml` and dont forget to add to you app manifest.

## Permission

Module needs CAMERA permission for work.


## Usage

```javascript
const win = Ti.UI.createWindow();
const VISION = require('ti.vision');

if (VISION.isOperational()==true) {
	

	const cameraView VISION.createCameraview({
		flash : false,
		type : VISION.TYPE_OCR,
		top : 0,
		lifecycleContainer : win,
		height : "50%",
		regex : "[0-9.,]{4,6}", // 4…6 digits
		autofocus : true,
		zoom: 1.0,
		facing : VISION.CAMERA_FACING_BACK,
		requestedFps : 2.0,
		requestedPreviewSize : [1280, 1024]
	});
win.add(cameraView);
cameraView.open();
camera.onSuccess = (e) => {
	console.log(e.text);
	cameraView.close();
}	



```

