package com.ridwanfatur.blazefaceonnx.camera.camera_inference.utils

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max

@Composable
fun DetectionOverlay(
    detections: Array<FloatArray>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val frameAspectRatio =
            canvasWidth / canvasHeight

        val inputAspectRatio =
            INPUT_WIDTH.toFloat() / INPUT_HEIGHT.toFloat()

        val scale: Float
        val scaledWidth: Float
        val scaledHeight: Float

        if (frameAspectRatio > inputAspectRatio) {
            /*
             * Frame is wider than model input.
             *
             * Width fills 128.
             * Padding exists on top/bottom.
             */
            scale =
                INPUT_WIDTH.toFloat() / canvasWidth

            scaledWidth =
                INPUT_WIDTH.toFloat()

            scaledHeight =
                canvasHeight * scale

        } else {
            /*
             * Frame is taller than model input.
             *
             * Height fills 128.
             * Padding exists on left/right.
             */
            scale =
                INPUT_HEIGHT.toFloat() / canvasHeight

            scaledWidth =
                canvasWidth * scale

            scaledHeight =
                INPUT_HEIGHT.toFloat()
        }

        /*
         * Padding inside 128x128 model input.
         */
        val padX =
            (INPUT_WIDTH - scaledWidth) / 2f

        val padY =
            (INPUT_HEIGHT - scaledHeight) / 2f

        for (detection in detections) {
            if (detection.size < 17) {
                continue
            }

            /*
             * Detection format:
             *
             * 0  = ymin
             * 1  = xmin
             * 2  = ymax
             * 3  = xmax
             *
             * 4..15 = 6 keypoints
             *
             * 16 = score
             */

            /*
             * Bounding box:
             *
             * normalized
             * -> 128x128
             * -> remove padding
             * -> Canvas
             */

            val modelYMin =
                detection[0] * INPUT_HEIGHT

            val modelXMin =
                detection[1] * INPUT_WIDTH

            val modelYMax =
                detection[2] * INPUT_HEIGHT

            val modelXMax =
                detection[3] * INPUT_WIDTH

            val frameXMin =
                (modelXMin - padX) / scale

            val frameYMin =
                (modelYMin - padY) / scale

            val frameXMax =
                (modelXMax - padX) / scale

            val frameYMax =
                (modelYMax - padY) / scale

            val xmin =
                frameXMin.coerceIn(
                    0f,
                    canvasWidth
                )

            val ymin =
                frameYMin.coerceIn(
                    0f,
                    canvasHeight
                )

            val xmax =
                frameXMax.coerceIn(
                    0f,
                    canvasWidth
                )

            val ymax =
                frameYMax.coerceIn(
                    0f,
                    canvasHeight
                )

            /*
             * Draw bounding box.
             */
            drawRect(
                color = Color.Red,
                topLeft = Offset(
                    x = xmin,
                    y = ymin
                ),
                size = Size(
                    width = max(
                        xmax - xmin,
                        0f
                    ),
                    height = max(
                        ymax - ymin,
                        0f
                    )
                ),
                style = Stroke(
                    width = 4f
                )
            )

            /*
             * Draw 6 keypoints.
             *
             * Same transformation as bounding box:
             *
             * normalized
             * -> 128x128
             * -> remove padding
             * -> Canvas
             */
            for (k in 0 until 6) {
                val modelKpX =
                    detection[4 + k * 2] *
                            INPUT_WIDTH

                val modelKpY =
                    detection[4 + k * 2 + 1] *
                            INPUT_HEIGHT

                val frameKpX =
                    (modelKpX - padX) / scale

                val frameKpY =
                    (modelKpY - padY) / scale

                val kpX =
                    frameKpX.coerceIn(
                        0f,
                        canvasWidth
                    )

                val kpY =
                    frameKpY.coerceIn(
                        0f,
                        canvasHeight
                    )

                /*
                 * Blue keypoint.
                 */
                drawCircle(
                    color = Color.Blue,
                    radius = 6f,
                    center = Offset(
                        x = kpX,
                        y = kpY
                    )
                )
            }
        }
    }
}