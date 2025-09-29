package com.example.feeloscope.services;

import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;

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
            if (listener != null) {
                listener.onError(new IllegalStateException("FaceDetector not initialized."));
            }
            return;
        }
        faceDetector.process(image)
                .addOnSuccessListener(faces -> {
                    Log.d(TAG, "Number of faces detected: " + faces.size());
                    for (Face face : faces) {
                        Rect bounds = face.getBoundingBox();
                        Log.d(TAG, "Face bounds: " + bounds.flattenToString());

                        if (face.getTrackingId() != null) {
                            int id = face.getTrackingId();
                            Log.d(TAG, "Face tracking ID: " + id);
                        }

                        float rotY = face.getHeadEulerAngleY(); // Head is rotated to the right rotY degrees
                        float rotZ = face.getHeadEulerAngleZ(); // Head is tilted sideways rotZ degrees
                        Log.d(TAG, "Head rotation Y: " + rotY + ", Z: " + rotZ);

                        // If classification was enabled:
                        if (face.getSmilingProbability() != null) {
                            float smileProb = face.getSmilingProbability();
                            Log.d(TAG, "Smiling probability: " + smileProb);
                        }
                        if (face.getLeftEyeOpenProbability() != null) {
                            float leftEyeOpenProb = face.getLeftEyeOpenProbability();
                            Log.d(TAG, "Left eye open probability: " + leftEyeOpenProb);
                        }
                        if (face.getRightEyeOpenProbability() != null) {
                            float rightEyeOpenProb = face.getRightEyeOpenProbability();
                            Log.d(TAG, "Right eye open probability: " + rightEyeOpenProb);
                        }

                        // If landmark detection was enabled:
                        FaceLandmark leftEar = face.getLandmark(FaceLandmark.LEFT_EAR);
                        if (leftEar != null) {
                            Log.d(TAG, "Left ear position: " + leftEar.getPosition().toString());
                        }
                        FaceLandmark rightEar = face.getLandmark(FaceLandmark.RIGHT_EAR);
                        if (rightEar != null) {
                            Log.d(TAG, "Right ear position: " + rightEar.getPosition().toString());
                        }
                        FaceLandmark noseBase = face.getLandmark(FaceLandmark.NOSE_BASE);
                        if (noseBase != null) {
                            Log.d(TAG, "Nose base position: " + noseBase.getPosition().toString());
                        }
                        // Add more landmark logging as needed...
                    }
                    
                    // Notify the listener after logging the details
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
