### How eventing works

#### class variables

```java
private GestureDetector tapDetector;
private ScaleGestureDetector pinchDetector;
```

and

```java
View.OnTouchListener tapListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }
    };
View.OnTouchListener pinchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return scaleGestureDetector.onTouchEvent(event);
        }
};
```

#### In constructor of extended TiUIView
`cameraPreview` is child of container.

```
cameraPreview.setOnTouchListener(tapListener);
cameraPreview.setOnTouchListener(pinchListener);    
```

#### Method startCamera()

```java
gestureDetector = new GestureDetector(ctx, new TapListener());
scaleGestureDetector = new ScaleGestureDetector(ctx, new PinchListener());
```

#### Private intern classes

For tapping (selecting of text block) 

```java
private class TapListener extends GestureDetector.SimpleOnGestureListener {
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
			return onTextBlockTapped(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
	}
}
```

and for pinching

```java
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
		   cameraSource.doZoom(detector.getScaleFactor());
	}
}
```

