/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ti.vision.utilities;

import android.util.SparseArray;

import java.util.ArrayList;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.util.TiConvert;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.teapink.ocr_reader.ui.camera.GraphicOverlay;
import com.teapink.ocr_reader.utilities.OcrGraphic;

import ti.vision.OcrViewProxy;
import ti.vision.VisionModule;

public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

	private GraphicOverlay<OcrGraphic> graphicOverlay;
	private OcrViewProxy proxy;
	private String regex;
	private static String LCAT = VisionModule.LCAT;

	public OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay) {
		graphicOverlay = ocrGraphicOverlay;
	}

	public OcrDetectorProcessor(OcrViewProxy proxy, GraphicOverlay<OcrGraphic> ocrGraphicOverlay) {
		graphicOverlay = ocrGraphicOverlay;
		this.proxy = proxy;
		this.regex = proxy.regex;
	}

	/**
	 * Called by the detector to deliver detection results. If your application
	 * called for it, this could be a place to check for equivalent detections by
	 * tracking TextBlocks that are similar in location and content from previous
	 * frames, or reduce noise by eliminating TextBlocks that have not persisted
	 * through multiple detections.
	 */
	@Override
	public void receiveDetections(Detector.Detections<TextBlock> detections) {
		graphicOverlay.clear();
		SparseArray<TextBlock> items = detections.getDetectedItems();
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < items.size(); ++i) {
			TextBlock item = items.valueAt(i);
			String text = item.getValue();
			if (regex == null || item.getValue().matches(regex)) {
				OcrGraphic graphic = new OcrGraphic(graphicOverlay, item);
				
				graphicOverlay.add(graphic);
				list.add(text);
			}
		}
		if (proxy.hasListeners("receivedetections") && items.size() > 0) {
			KrollDict res = new KrollDict();
			res.put("total", items.size());
			res.put("detections", list.toArray());
			res.put("receivedetections", list.toArray());
			proxy.fireEvent("receivedetections", res);
		}
	}

	/**
	 * Frees the resources associated with this detection processor.
	 */
	@Override
	public void release() {
		graphicOverlay.clear();
	}
}
