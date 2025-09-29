package com.example.feeloscope.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.mlkit.vision.face.Face;
import java.util.ArrayList;
import java.util.List;

public class FaceBoundingBoxOverlay extends View {
    private final Paint paint;
    private List<Face> faces = new ArrayList<>();
    private int imageWidth = -1;
    private int imageHeight = -1;
    private int imageRotationDegrees = 0;
    private boolean mirrorHorizontally = false; // For front camera, may need to mirror

    public FaceBoundingBoxOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8.0f);
    }

    public void updateFaces(List<Face> faces, int imageWidth, int imageHeight, int imageRotationDegrees, boolean mirrorHorizontally) {
        this.faces = faces != null ? faces : new ArrayList<>();
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.imageRotationDegrees = imageRotationDegrees;
        this.mirrorHorizontally = mirrorHorizontally; // This might be true if using LENS_FACING_FRONT
        invalidate(); // Redraw the view
    }

    public void clearFaces() {
        this.faces.clear();
        this.imageWidth = -1;
        this.imageHeight = -1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (faces.isEmpty() || imageWidth == -1 || imageHeight == -1) {
            return;
        }

        for (Face face : faces) {
            Rect boundingBox = face.getBoundingBox();
            RectF mappedBoundingBox = transformToViewCoordinates(boundingBox);
            canvas.drawRect(mappedBoundingBox, paint);
        }
    }

    private RectF transformToViewCoordinates(Rect boundingBox) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return new RectF(boundingBox); // Cannot transform if image dimensions are unknown
        }

        float viewWidth = getWidth();
        float viewHeight = getHeight();

        // Create a matrix for transforming coordinates from image space to view space.
        Matrix matrix = new Matrix();

        // 1. Calculate scale factors. The image from CameraX is often rotated and needs to be scaled to fit the PreviewView.
        // We need to consider the rotation of the image to correctly map width/height.
        float imageAspectRatio = (imageRotationDegrees % 180 == 0) ? (float) imageWidth / imageHeight : (float) imageHeight / imageWidth;
        float viewAspectRatio = viewWidth / viewHeight;

        float scaleX, scaleY;
        // This logic assumes the PreviewView is using a scale type like FIT_CENTER or FIT_XY.
        // For FIT_CENTER (most common for camera previews), the image is scaled to fit while maintaining aspect ratio.
        if (imageRotationDegrees % 180 == 0) { // Portrait or reverse portrait image
            scaleX = viewWidth / imageWidth;
            scaleY = viewHeight / imageHeight;
        } else { // Landscape or reverse landscape image
            scaleX = viewWidth / imageHeight; // Image width in view = imageHeight after rotation
            scaleY = viewHeight / imageWidth;  // Image height in view = imageWidth after rotation
        }

        // Adjust scale to maintain aspect ratio (letterboxing/pillarboxing)
        float postScaleX, postScaleY;
        if (imageAspectRatio > viewAspectRatio) { // Image is wider than view (letterboxed)
            postScaleX = viewWidth / ((imageRotationDegrees % 180 == 0) ? imageWidth : imageHeight);
            postScaleY = postScaleX;
        } else { // Image is taller than view (pillarboxed)
            postScaleY = viewHeight / ((imageRotationDegrees % 180 == 0) ? imageHeight : imageWidth);
            postScaleX = postScaleY;
        }

        matrix.postScale(postScaleX, postScaleY);

        // If mirroring is needed (e.g., for front camera), apply it.
        // This example assumes back camera, so mirrorHorizontally would be false.
        // If it were true, you'd do: matrix.postScale(-1, 1, viewWidth / 2f, viewHeight / 2f);
        if (mirrorHorizontally) {
             //This needs careful handling, for LENS_FACING_FRONT, coordinates are already mirrored by CameraX/MLKit in some cases.
             //For now, assuming this is handled or primarily for back camera.
             //If you enable front camera and mirroring is off, you might need: 
             // boundingBox.left = imageWidth - boundingBox.right;
             // boundingBox.right = imageWidth - boundingBox.left (before this transformation function)
        }

        RectF rectF = new RectF(boundingBox);
        matrix.mapRect(rectF);
        return rectF;
    }
}
