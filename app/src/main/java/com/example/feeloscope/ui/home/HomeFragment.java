package com.example.feeloscope.ui.home;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.feeloscope.R;
import com.example.feeloscope.databinding.FragmentHomeBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class HomeFragment extends Fragment {

    private static final String KEY_AI_TOGGLE_DEFAULT = "pref_ai_toggle_default";
    private static final String KEY_AUTO_AI_AFTER_CAPTURE = "pref_auto_ai_after_capture";
    private static final String KEY_OVERLAY_OPACITY = "pref_overlay_opacity";
    private static final int DEFAULT_OVERLAY_OPACITY = 70;

    private FragmentHomeBinding binding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private SharedPreferences sharedPreferences;
    private ImageCapture imageCapture;
    private File lastPhotoFile;
    private CompoundButton.OnCheckedChangeListener aiSwitchChangeListener;
    private boolean suppressAiToast;

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (prefs, key) -> {
        if (binding == null) {
            return;
        }
        if (KEY_AI_TOGGLE_DEFAULT.equals(key)) {
            applyAiDefault();
        } else if (KEY_OVERLAY_OPACITY.equals(key)) {
            applyOverlayOpacity();
        }
    };

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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        aiSwitchChangeListener = (buttonView, isChecked) -> {
            if (sharedPreferences != null) {
                sharedPreferences.edit().putBoolean(KEY_AI_TOGGLE_DEFAULT, isChecked).apply();
            }
            updateAiUi(isChecked);
            if (isAdded() && !suppressAiToast) {
                int messageRes = isChecked ? R.string.ai_enabled_message : R.string.ai_disabled_message;
                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show();
            }
            suppressAiToast = false;
        };

        binding.aiToggleSwitch.setOnCheckedChangeListener(aiSwitchChangeListener);
        binding.aiActionButton.setOnClickListener(v -> handleAiAction());
        binding.discardPhotoButton.setOnClickListener(v -> {
            clearCapturedPhoto();
            if (isAdded()) {
                Toast.makeText(requireContext(), R.string.photo_discarded_message, Toast.LENGTH_SHORT).show();
            }
        });

        binding.shutterButton.setOnClickListener(v -> takePhoto());

        applyUserPreferences();

        ensureCameraPermission();
    }

    private void ensureCameraPermission() {
        if (!isAdded()) {
            return;
        }
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

                int rotation = previewView.getDisplay() != null
                        ? previewView.getDisplay().getRotation()
                        : Surface.ROTATION_0;

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(rotation)
                        .build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector, preview, imageCapture);
            } catch (ExecutionException e) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (!isAdded()) {
            return;
        }
        if (imageCapture == null) {
            Toast.makeText(requireContext(), R.string.photo_capture_generic_error, Toast.LENGTH_SHORT).show();
            return;
        }

        File outputDir = new File(requireContext().getCacheDir(), "captures");
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Toast.makeText(requireContext(), R.string.photo_capture_generic_error, Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile;
        try {
            photoFile = File.createTempFile("feeloscope_", ".jpg", outputDir);
        } catch (IOException e) {
            String message = e.getLocalizedMessage();
            if (message == null || message.isEmpty()) {
                message = getString(R.string.photo_capture_generic_error);
            } else {
                message = getString(R.string.photo_capture_error, message);
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            return;
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                deleteLastPhotoFile();
                lastPhotoFile = photoFile;
                if (isAdded()) {
                    showCapturedPhoto(photoFile);
                    Toast.makeText(requireContext(), R.string.photo_capture_success, Toast.LENGTH_SHORT).show();
                    if (shouldAutoEnableAi() && binding != null && !binding.aiToggleSwitch.isChecked()) {
                        suppressAiToast = true;
                        binding.aiToggleSwitch.setChecked(true);
                        Toast.makeText(requireContext(), R.string.ai_now_enabled_message, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                if (photoFile.exists()) {
                    // Clean up file that could not be used
                    //noinspection ResultOfMethodCallIgnored
                    photoFile.delete();
                }
                if (isAdded()) {
                    String message = exception.getLocalizedMessage();
                    if (message == null || message.isEmpty()) {
                        message = getString(R.string.photo_capture_generic_error);
                    } else {
                        message = getString(R.string.photo_capture_error, message);
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showCapturedPhoto(@NonNull File photoFile) {
        if (binding == null) {
            return;
        }
        Bitmap bitmap = decodeScaledBitmap(photoFile);
        if (bitmap != null) {
            binding.capturedPreviewImage.setImageBitmap(bitmap);
            binding.capturePreviewCard.setVisibility(View.VISIBLE);
            binding.capturedPreviewImage.setContentDescription(getString(R.string.captured_preview_description));
        } else if (isAdded()) {
            Toast.makeText(requireContext(), R.string.photo_capture_generic_error, Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeScaledBitmap(@NonNull File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    private int calculateInSampleSize(@NonNull BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private void handleAiAction() {
        if (binding == null || !isAdded()) {
            return;
        }
        if (!binding.aiToggleSwitch.isChecked()) {
            suppressAiToast = true;
            binding.aiToggleSwitch.setChecked(true);
            Toast.makeText(requireContext(), R.string.ai_now_enabled_message, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), R.string.ai_enabled_message, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearCapturedPhoto() {
        if (binding != null) {
            binding.capturedPreviewImage.setImageDrawable(null);
            binding.capturePreviewCard.setVisibility(View.GONE);
        }
        deleteLastPhotoFile();
    }

    private void deleteLastPhotoFile() {
        if (lastPhotoFile != null && lastPhotoFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            lastPhotoFile.delete();
        }
        lastPhotoFile = null;
    }

    private void applyUserPreferences() {
        applyAiDefault();
        applyOverlayOpacity();
    }

    private void applyAiDefault() {
        if (binding == null || sharedPreferences == null) {
            return;
        }
        boolean aiEnabled = sharedPreferences.getBoolean(KEY_AI_TOGGLE_DEFAULT, false);
        binding.aiToggleSwitch.setOnCheckedChangeListener(null);
        binding.aiToggleSwitch.setChecked(aiEnabled);
        updateAiUi(aiEnabled);
        binding.aiToggleSwitch.setOnCheckedChangeListener(aiSwitchChangeListener);
        suppressAiToast = false;
    }

    private void applyOverlayOpacity() {
        if (binding == null || sharedPreferences == null) {
            return;
        }
        int value = sharedPreferences.getInt(KEY_OVERLAY_OPACITY, DEFAULT_OVERLAY_OPACITY);
        value = Math.max(40, Math.min(95, value));
        int baseColor = ContextCompat.getColor(requireContext(), R.color.overlay_card_base);
        int backgroundColor = ColorUtils.setAlphaComponent(baseColor, (int) (value / 100f * 255));
        binding.topControls.setCardBackgroundColor(backgroundColor);
        binding.bottomControls.setCardBackgroundColor(backgroundColor);
        binding.capturePreviewCard.setCardBackgroundColor(backgroundColor);
    }

    private void updateAiUi(boolean isEnabled) {
        if (binding == null) {
            return;
        }
        binding.aiToggleIcon.setImageResource(isEnabled ? R.drawable.ic_sun : R.drawable.ic_moon);
        binding.aiToggleSubtitle.setText(isEnabled
                ? R.string.home_creative_toggle_hint_active
                : R.string.home_creative_toggle_hint);
    }

    private boolean shouldAutoEnableAi() {
        return sharedPreferences != null && sharedPreferences.getBoolean(KEY_AUTO_AI_AFTER_CAPTURE, true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.aiToggleSwitch.setOnCheckedChangeListener(null);
        }
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
        clearCapturedPhoto();
        binding = null;
        cameraProviderFuture = null;
        imageCapture = null;
        sharedPreferences = null;
        aiSwitchChangeListener = null;
    }
}
