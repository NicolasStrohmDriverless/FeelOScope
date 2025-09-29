package com.example.feeloscope.services;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class FaceDetectionHelper {
    private static final String TAG = "FaceDetectionHelper";
    private final FaceDetector faceDetector;
    private final FaceDetectionListener listener;

    public FaceDetectionHelper(FaceDetectionListener listener) {
        this.listener = listener;

        // Configure ML Kit Face Detector (same options as before)
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        faceDetector = FaceDetection.getClient(highAccuracyOpts);
        Log.d(TAG, "FaceDetectionHelper initialized.");
    }

    public void process(InputImage image) {
        if (faceDetector == null) {
            Log.e(TAG, "FaceDetector is not initialized.");
            listener.onError(new IllegalStateException("FaceDetector not initialized."));
            return;
        }
        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    Log.d(TAG, "Number of faces detected: " + faces.size());
                    if (listener != null) {
                        listener.onFacesDetected(faces, image);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Face detection failed: " + e.getMessage(), e);
                    if (listener != null) {
                        listener.onError(e);
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
