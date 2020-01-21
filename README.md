# Ti.OCRvision

Axway Titanium module for OCR in preview camera.

## Usage

```javascript
const win = Ti.UI.createWindow();
const OCR = require('tiocrvision');
const cameraView OCR.createCameraview({
	flash : false,
	top : 0,
	lifecycleContainer : win,
	height : "50%",
	autofocus : true,
	facing : OCR.CAMERA_FACING_BACK,
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

