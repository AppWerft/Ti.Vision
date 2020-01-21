# TiOCRvision

Axway Titanium module for OCR in preview camera.

## Usage

```javascript
const OCR = require('tiocrvision');
const camera OCR.createCamera({
	flash : false,
	autofocus : true,
	facing : OCR.CAMERA_FACING_BACK,
	requestedFps : 2.0,
	requestedPreviewSize : [1280, 1024]
});
camera.start();
camera.onSuccess = (e) => {
	console.log(e.text);
	camera.close();
}	



```

