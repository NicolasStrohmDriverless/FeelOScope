package com.example.feeloscope.services;

import androidx.camera.core.ImageProxy;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import java.util.List;

public interface FaceDetectionListener {
    void onFacesDetected(List<Face> faces, InputImage image, ImageProxy imageProxy); // Pass original image for context and ImageProxy to close
    void onError(Exception e, ImageProxy imageProxy);
}
