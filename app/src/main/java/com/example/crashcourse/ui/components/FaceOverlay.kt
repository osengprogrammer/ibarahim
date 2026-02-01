package com.example.crashcourse.ui

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

@Composable
fun FaceOverlay(
    faceBounds: List<Rect>,
    imageSize: IntSize,
    imageRotation: Int = 0, // Used for orientation-aware scaling
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    paddingFactor: Float = 0.1f
) {
    // Law L3 Compliance: Do nothing if facts are incomplete
    if (faceBounds.isEmpty() || imageSize.width <= 0 || imageSize.height <= 0) return

    Canvas(modifier) {
        // 1. Calculate the scaling factors safely
        val scaleX = size.width / imageSize.width.toFloat()
        val scaleY = size.height / imageSize.height.toFloat()
        
        // Use the larger scale to simulate "Fill Center" behavior
        val scale = max(scaleX, scaleY)
        
        // 2. Calculate offsets to center the box
        val offX = (size.width - imageSize.width * scale) / 2f
        val offY = (size.height - imageSize.height * scale) / 2f

        faceBounds.forEach { r ->
            // Ensure the box is a square for consistent recognition framing
            val side = max(r.width(), r.height()) * (1 + paddingFactor)
            val cx = r.centerX().toFloat()
            val cy = r.centerY().toFloat()

            // 3. Coordinate Transformation (Handle Front/Back Camera Flip)
            val leftInImage = if (isFrontCamera) {
                // Flip X for selfie mode
                imageSize.width - (cx + side / 2f)
            } else {
                cx - side / 2f
            }
            val topInImage = cy - side / 2f

            // 4. Final Safety Check: Don't draw if coordinates are infinite/NaN
            val finalX = leftInImage * scale + offX
            val finalY = topInImage * scale + offY
            
            if (finalX.isFinite() && finalY.isFinite()) {
                drawRect(
                    color = Color.Green,
                    topLeft = Offset(finalX, finalY),
                    size = Size(side * scale, side * scale),
                    style = Stroke(width = 8f) // Thicker for visibility in Phase-1
                )
            }
        }
    }
}