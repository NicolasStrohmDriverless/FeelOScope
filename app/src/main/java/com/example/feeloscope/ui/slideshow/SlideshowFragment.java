package com.example.feeloscope.ui.slideshow;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.feeloscope.R;
import com.example.feeloscope.databinding.FragmentSlideshowBinding;
import com.example.feeloscope.services.FaceDetectionHelper;
import com.example.feeloscope.services.FaceDetectionListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;

import java.util.List;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private ActivityResultLauncher<String[]> videoPickerLauncher;
    private FaceDetectionHelper faceDetectionHelper;
    private MediaController mediaController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        faceDetectionHelper = new FaceDetectionHelper(new FaceDetectionListener() {
            @Override
            public void onFacesDetected(List<Face> faces, InputImage image) {
                if (!isAdded() || binding == null) {
                    return;
                }
                String faceSummary;
                int count = faces.size();
                if (count == 0) {
                    faceSummary = getString(R.string.gallery_analysis_result_none);
                } else if (count == 1) {
                    faceSummary = getString(R.string.gallery_analysis_result_single);
                } else {
                    faceSummary = getString(R.string.gallery_analysis_result_multiple, count);
                }
                binding.videoAnalysisResult.setText(getString(R.string.video_analysis_result_template, faceSummary));
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded() || binding == null) {
                    return;
                }
                String message = TextUtils.isEmpty(e.getLocalizedMessage())
                        ? e.getClass().getSimpleName()
                        : e.getLocalizedMessage();
                binding.videoAnalysisResult.setText(getString(R.string.gallery_analysis_error, message));
            }
        });

        videoPickerLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleVideoResult);
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        binding.selectVideoButton.setOnClickListener(v -> videoPickerLauncher.launch(new String[]{"video/*"}));
        return binding.getRoot();
    }

    private void handleVideoResult(@Nullable Uri uri) {
        if (!isAdded() || binding == null || uri == null) {
            return;
        }

        binding.videoAnalysisResult.setText(getString(R.string.gallery_analysis_in_progress));
        binding.selectedVideo.setVisibility(View.VISIBLE);
        binding.selectedVideo.setVideoURI(uri);

        if (mediaController == null) {
            mediaController = new MediaController(requireContext());
        }
        mediaController.setAnchorView(binding.selectedVideo);
        binding.selectedVideo.setMediaController(mediaController);
        binding.selectedVideo.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            binding.selectedVideo.start();
        });
        binding.selectedVideo.seekTo(1);

        try {
            requireContext().getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
            // Ignore when persistable permissions are not supported.
        }

        analyzeVideo(uri);
    }

    private void analyzeVideo(@NonNull Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(requireContext(), uri);
            Bitmap frame = retriever.getFrameAtTime(0);
            if (frame == null) {
                if (binding != null) {
                    binding.videoAnalysisResult.setText(R.string.video_analysis_frame_missing);
                }
                return;
            }
            InputImage image = InputImage.fromBitmap(frame, 0);
            faceDetectionHelper.process(image);
        } catch (RuntimeException e) {
            if (binding != null) {
                binding.videoAnalysisResult.setText(getString(R.string.gallery_analysis_error, e.getLocalizedMessage()));
            }
        } finally {
            retriever.release();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null) {
            binding.selectedVideo.stopPlayback();
        }
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