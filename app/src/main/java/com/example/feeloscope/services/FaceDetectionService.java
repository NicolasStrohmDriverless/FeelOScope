package com.example.feeloscope.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

public class FaceDetectionService extends Service {

    private static final String TAG = "FaceDetectionService";
    private FaceDetector faceDetector;
    private static final int NOTIFICATION_ID = 123;
    private static final String CHANNEL_ID = "FaceDetectionChannel";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "FaceDetectionService created");

        createNotificationChannel();

        // Configure ML Kit Face Detector
        FaceDetectorOptions highAccuracyOpts =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        faceDetector = FaceDetection.getClient(highAccuracyOpts);

        // TODO: Initialize CameraX or Camera2 API here
        // TODO: Start capturing frames and pass them to the faceDetector
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Face Detection Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    // Method to process an image frame (e.g., from CameraX or Camera2)
    private void processImage(InputImage image) {
        faceDetector.process(image)
                .addOnSuccessListener(
                        faces -> {
                            // Task completed successfully
                            // TODO: Process the detected faces (e.g., count, get properties)
                            Log.d(TAG, "Number of faces detected: " + faces.size());
                            // If you need to update UI, send a broadcast or use a Handler
                        })
                .addOnFailureListener(
                        e -> {
                            // Task failed with an exception
                            Log.e(TAG, "Face detection failed: " + e.getMessage(), e);
                        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "FaceDetectionService started");

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Face Detection Active")
                .setContentText("Detecting faces in the background.")
                // TODO: Ersetze dies durch ein geeignetes Icon deiner App
                .setSmallIcon(android.R.drawable.ic_dialog_info) 
                .build();

        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY; 
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // This service is not designed to be bound, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "FaceDetectionService destroyed");
        if (faceDetector != null) {
            faceDetector.close();
        }
        // TODO: Release camera resources here
    }
}
