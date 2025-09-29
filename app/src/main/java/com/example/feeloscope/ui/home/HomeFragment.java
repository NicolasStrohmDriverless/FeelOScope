package com.example.feeloscope.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.feeloscope.R;
import com.example.feeloscope.databinding.FragmentHomeBinding;
import com.example.feeloscope.services.FaceDetectionHelper;
import com.example.feeloscope.services.FaceDetectionListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment implements FaceDetectionListener {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private FaceDetectionHelper faceDetectionHelper;
    private ExecutorService cameraExecutor;
    private boolean isAiEnabled = false;
    private int lensFacing = CameraSelector.LENS_FACING_BACK; // Default to back camera

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();
        faceDetectionHelper = new FaceDetectionHelper(this);

        binding.shutterButton.setOnClickListener(v -> {
            if (isAdded()) {
                Toast.makeText(requireContext(), R.string.shutter_placeholder_message, Toast.LENGTH_SHORT).show();
            }
        });

        binding.aiToggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isAiEnabled = isChecked;
            if (isAdded()) {
                int messageRes = isChecked ? R.string.ai_enabled_message : R.string.ai_disabled_message;
                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
                if (isChecked) {
                    binding.faceDetectionResultsTextview.setVisibility(View.VISIBLE);
                    binding.faceBoundingBoxOverlay.setVisibility(View.VISIBLE);
                } else {
                    binding.faceDetectionResultsTextview.setVisibility(View.GONE);
                    binding.faceDetectionResultsTextview.setText(""); // Clear previous results
                    binding.faceBoundingBoxOverlay.clearFaces();
                    binding.faceBoundingBoxOverlay.setVisibility(View.GONE);
                }
            }
        });

        if (binding.aiToggleSwitch.isChecked()) {
            binding.faceDetectionResultsTextview.setVisibility(View.VISIBLE);
            binding.faceBoundingBoxOverlay.setVisibility(View.VISIBLE);
        } else {
            binding.faceDetectionResultsTextview.setVisibility(View.GONE);
            binding.faceBoundingBoxOverlay.setVisibility(View.GONE);
            binding.faceBoundingBoxOverlay.clearFaces();
        }

        ensureCameraPermission();
    }

    private void ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        if (!isAdded() || binding == null) {
            return;
        }

        final PreviewView previewView = binding.cameraPreview;
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        // Set target resolution for consistent analysis, adjust as needed
                        // .setTargetResolution(new android.util.Size(640, 480))
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    if (isAiEnabled && binding != null) {
                        @SuppressLint("UnsafeOptInUsageError")
                        Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null) {
                            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                            faceDetectionHelper.process(image, imageProxy);
                        } else {
                            imageProxy.close();
                        }
                    } else {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error starting camera: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "ExecutionException starting camera", e);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, "InterruptedException starting camera", e);
            }  catch (Exception e) {
                 if (isAdded()) {
                    Toast.makeText(requireContext(), "Unexpected error starting camera: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Unexpected error starting camera", e);
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (faceDetectionHelper != null) {
            faceDetectionHelper.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        binding = null;
    }

    @Override
    public void onFacesDetected(List<Face> faces, InputImage image, ImageProxy imageProxy) {
        try {
            if (!isAdded() || binding == null || !isAiEnabled) {
                return;
            }
            // Update overlay
            if (binding.faceBoundingBoxOverlay != null) {
                boolean mirror = lensFacing == CameraSelector.LENS_FACING_FRONT;
                binding.faceBoundingBoxOverlay.updateFaces(
                        faces,
                        image.getWidth(),
                        image.getHeight(),
                        imageProxy.getImageInfo().getRotationDegrees(),
                        mirror
                );
            }

            StringBuilder resultText = new StringBuilder();
            if (faces.isEmpty()) {
                resultText.append("No faces detected.");
            } else {
                resultText.append(faces.size()).append(" face(s) detected:\n");
                for (int i = 0; i < faces.size(); i++) {
                    Face face = faces.get(i);
                    resultText.append("  Face ").append(i + 1).append(":\n");
                    // Bounding box text removed, handled by overlay
                    if (face.getSmilingProbability() != null) {
                        resultText.append(String.format("    Smile: %.2f%%\n", face.getSmilingProbability() * 100));
                    }
                    if (face.getLeftEyeOpenProbability() != null) {
                        resultText.append(String.format("    Left Eye Open: %.2f%%\n", face.getLeftEyeOpenProbability() * 100));
                    }
                    if (face.getRightEyeOpenProbability() != null) {
                        resultText.append(String.format("    Right Eye Open: %.2f%%\n", face.getRightEyeOpenProbability() * 100));
                    }
                }
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && binding.faceDetectionResultsTextview != null) {
                        binding.faceDetectionResultsTextview.setText(resultText.toString());
                    }
                });
            }
        } finally {
            imageProxy.close();
        }
    }

    @Override
    public void onError(Exception e, ImageProxy imageProxy) {
        try {
            if (isAdded()) {
                Log.e(TAG, "Face detection error: ", e);
                if (binding != null && binding.faceBoundingBoxOverlay != null) {
                    binding.faceBoundingBoxOverlay.clearFaces();
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Face detection error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        } finally {
            imageProxy.close();
        }
    }
}
