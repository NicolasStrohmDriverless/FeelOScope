package com.example.feeloscope.services;

import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.List;

public class FaceDetectionHelper {
    private static final String TAG = "FaceDetectionHelper";
    private final FaceDetector faceDetector;
    private final FaceDetectionListener listener;

    public FaceDetectionHelper(FaceDetectionListener listener) {
        this.listener = listener;

        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        faceDetector = FaceDetection.getClient(highAccuracyOpts);
        Log.d(TAG, "FaceDetectionHelper initialized.");
    }

    public void process(InputImage image, ImageProxy imageProxy) { // Added imageProxy parameter
        if (faceDetector == null) {
            Log.e(TAG, "FaceDetector is not initialized.");
            if (listener != null) {
                // Pass imageProxy to onError and ensure it's closed there
                listener.onError(new IllegalStateException("FaceDetector not initialized."), imageProxy);
            }
            return;
        }
        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    Log.d(TAG, "Number of faces detected: " + faces.size());
                    // Detailed logging can be kept or removed as needed
                    for (Face face : faces) {
                        Rect bounds = face.getBoundingBox();
                        Log.d(TAG, "Face bounds: " + bounds.flattenToString());
                        // Add other specific logging if still necessary
                    }
                    
                    if (listener != null) {
                        listener.onFacesDetected(faces, image, imageProxy); // Pass imageProxy
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onError(e, imageProxy); // Pass imageProxy
                    }
                });
    }

    public void close() {
        if (faceDetector != null) {
            faceDetector.close();
            Log.d(TAG, "FaceDetector closed.");
        }
    }
}
