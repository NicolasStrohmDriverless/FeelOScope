package com.example.feeloscope.ui.gallery;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.feeloscope.R;
import com.example.feeloscope.databinding.FragmentGalleryBinding;
import com.example.feeloscope.services.FaceDetectionHelper;
import com.example.feeloscope.services.FaceDetectionListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

import java.io.IOException;
import java.util.List;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private ActivityResultLauncher<String[]> imagePickerLauncher;
    private FaceDetectionHelper faceDetectionHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        faceDetectionHelper = new FaceDetectionHelper(new FaceDetectionListener() {
            @Override
            public void onFacesDetected(List<Face> faces, InputImage image) {
                if (!isAdded() || binding == null) {
                    return;
                }
                int count = faces.size();
                String message;
                if (count == 0) {
                    message = getString(R.string.gallery_analysis_result_none);
                } else if (count == 1) {
                    message = getString(R.string.gallery_analysis_result_single);
                } else {
                    message = getString(R.string.gallery_analysis_result_multiple, count);
                }
                binding.analysisResult.setText(message);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded() || binding == null) {
                    return;
                }
                String message = TextUtils.isEmpty(e.getLocalizedMessage())
                        ? e.getClass().getSimpleName()
                        : e.getLocalizedMessage();
                binding.analysisResult.setText(getString(R.string.gallery_analysis_error, message));
            }
        });

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImageResult);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        binding.selectImageButton.setOnClickListener(v -> imagePickerLauncher.launch(new String[]{"image/*"}));
        return binding.getRoot();
    }

    private void handleImageResult(@Nullable Uri uri) {
        if (!isAdded() || binding == null || uri == null) {
            return;
        }

        binding.analysisResult.setText(getString(R.string.gallery_analysis_in_progress));
        binding.selectedImage.setVisibility(View.VISIBLE);
        binding.selectedImage.setImageURI(uri);

        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // If the URI does not support persistable permissions we can still use it for this session.
        }

        analyzeImage(uri);
    }

    private void analyzeImage(@NonNull Uri uri) {
        if (!isAdded()) {
            return;
        }
        try {
            InputImage inputImage = InputImage.fromFilePath(requireContext(), uri);
            faceDetectionHelper.process(inputImage);
        } catch (IOException e) {
            if (binding != null) {
                binding.analysisResult.setText(getString(R.string.gallery_analysis_error, e.getLocalizedMessage()));
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (faceDetectionHelper != null) {
            faceDetectionHelper.close();
        }
    }
}