package com.example.feeloscope.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.mlkit.vision.face.Face;
import java.util.ArrayList;
import java.util.List;

public class FaceBoundingBoxOverlay extends View {
    private final Paint paint;
    private List<Face> faces = new ArrayList<>();
    private int imageWidth = -1; // Width of the image processed by ML Kit (upright)
    private int imageHeight = -1; // Height of the image processed by ML Kit (upright)
    private int imageRotationDegrees = 0; // Rotation of the original camera buffer
    private boolean mirrorHorizontally = false; // True if using front camera and preview is mirrored

    public FaceBoundingBoxOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f); // Slightly thinner stroke
    }

    public void updateFaces(List<Face> faces, int imageWidth, int imageHeight, int imageRotationDegrees, boolean mirrorHorizontally) {
        this.faces = faces != null ? faces : new ArrayList<>();
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.imageRotationDegrees = imageRotationDegrees; // Stored, but not directly used in the new transform if imageWidth/Height are already upright
        this.mirrorHorizontally = mirrorHorizontally;
        invalidate(); // Redraw the view
    }

    public void clearFaces() {
        this.faces.clear();
        // Optionally reset imageWidth and imageHeight if they should not persist
        // this.imageWidth = -1;
        // this.imageHeight = -1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (faces.isEmpty() || this.imageWidth <= 0 || this.imageHeight <= 0) {
            return;
        }

        for (Face face : faces) {
            Rect boundingBox = face.getBoundingBox();
            RectF mappedBoundingBox = transformToViewCoordinates(boundingBox);
            if (mappedBoundingBox != null && !mappedBoundingBox.isEmpty()) {
                canvas.drawRect(mappedBoundingBox, paint);
            }
        }
    }

    private RectF transformToViewCoordinates(Rect boundingBox) {
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        if (this.imageWidth <= 0 || this.imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            Log.w(TAG, "Cannot transform bounding box with invalid image or view dimensions.");
            return new RectF(boundingBox); // Return unscaled/unmoved or an empty RectF
        }

        // The imageWidth and imageHeight are from the InputImage provided to ML Kit.
        // This InputImage is already effectively "upright" for ML Kit processing.
        // We need to scale these dimensions to fit the PreviewView (viewWidth, viewHeight)
        // while maintaining aspect ratio (FIT_CENTER behavior).

        float scaleX = viewWidth / (float) this.imageWidth;
        float scaleY = viewHeight / (float) this.imageHeight;
        float scale = Math.min(scaleX, scaleY);

        // Calculate the actual dimensions of the scaled image within the view.
        float scaledImageActualWidth = this.imageWidth * scale;
        float scaledImageActualHeight = this.imageHeight * scale;

        // Calculate offsets to center the scaled image within the view.
        float offsetX = (viewWidth - scaledImageActualWidth) / 2f;
        float offsetY = (viewHeight - scaledImageActualHeight) / 2f;

        RectF transformedBoundingBox = new RectF();

        // Apply mirroring to the bounding box coordinates if necessary.
        // Mirroring is done with respect to the original image's coordinate system (this.imageWidth).
        if (this.mirrorHorizontally) {
            transformedBoundingBox.left = this.imageWidth - boundingBox.right;
            transformedBoundingBox.right = this.imageWidth - boundingBox.left;
        } else {
            transformedBoundingBox.left = boundingBox.left;
            transformedBoundingBox.right = boundingBox.right;
        }
        transformedBoundingBox.top = boundingBox.top;
        transformedBoundingBox.bottom = boundingBox.bottom;

        // Scale the (potentially mirrored) bounding box coordinates.
        transformedBoundingBox.left *= scale;
        transformedBoundingBox.top *= scale;
        transformedBoundingBox.right *= scale;
        transformedBoundingBox.bottom *= scale;

        // Apply the offsets to position the bounding box correctly in the view.
        transformedBoundingBox.left += offsetX;
        transformedBoundingBox.right += offsetX;
        transformedBoundingBox.top += offsetY;
        transformedBoundingBox.bottom += offsetY;

        return transformedBoundingBox;
    }

    private static final String TAG = "FaceBoundingBoxOverlay"; // Added TAG for logging
}
