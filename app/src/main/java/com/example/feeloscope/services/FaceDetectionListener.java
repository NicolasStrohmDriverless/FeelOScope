package com.example.feeloscope.services;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import java.util.List;

public interface FaceDetectionListener {
    void onFacesDetected(List<Face> faces, InputImage image); // Pass original image for context if needed by UI
    void onError(Exception e);
}
