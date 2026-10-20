package com.ridwanfatur.blazefaceonnx.camera.simple_inference

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color

fun imagePostProcess(
    input: Array<Array<FloatArray>>,
    minScoreThresh: Float = 0.5f,
    minSuppressionThreshold: Float = 0.3f
): Array<FloatArray> {
    val detections = input[0]

    // Score filtering
    val filtered = detections
        .filter { it[16] >= minScoreThresh }
        .map { it.copyOf() }

    if (filtered.isEmpty()) {
        return emptyArray()
    }

    // Sort by score descending
    var remaining = filtered
        .indices
        .sortedByDescending { filtered[it][16] }
        .toMutableList()

    val outputDetections = mutableListOf<FloatArray>()

    while (remaining.isNotEmpty()) {
        val firstIndex = remaining[0]
        val detection = filtered[firstIndex]
        val firstBox = detection.copyOfRange(0, 4)

        val overlapping = mutableListOf<Int>()
        val notOverlapping = mutableListOf<Int>()

        // IoU
        for (index in remaining) {
            val otherBox = filtered[index]

            val ymin = max(firstBox[0], otherBox[0])
            val xmin = max(firstBox[1], otherBox[1])
            val ymax = min(firstBox[2], otherBox[2])
            val xmax = min(firstBox[3], otherBox[3])

            val interW = max(xmax - xmin, 0f)
            val interH = max(ymax - ymin, 0f)
            val intersection = interW * interH

            val areaA =
                (firstBox[2] - firstBox[0]) *
                        (firstBox[3] - firstBox[1])

            val areaB =
                (otherBox[2] - otherBox[0]) *
                        (otherBox[3] - otherBox[1])

            val union = areaA + areaB - intersection

            val iou = intersection / (union + 1e-6f)

            if (iou > minSuppressionThreshold) {
                overlapping.add(index)
            } else {
                notOverlapping.add(index)
            }
        }

        remaining = notOverlapping

        // Weighted detection
        val weightedDetection = detection.copyOf()

        if (overlapping.size > 1) {
            val totalScore = overlapping.sumOf {
                filtered[it][16].toDouble()
            }.toFloat()

            for (coordinateIndex in 0 until 16) {
                var weightedValue = 0f

                for (index in overlapping) {
                    val score = filtered[index][16]
                    weightedValue +=
                        filtered[index][coordinateIndex] * score
                }

                weightedDetection[coordinateIndex] =
                    weightedValue / totalScore
            }

            weightedDetection[16] =
                totalScore / overlapping.size
        }

        outputDetections.add(weightedDetection)
    }

    return outputDetections.toTypedArray()
}

fun visualize(
    bitmap: Bitmap,
    detections: Array<FloatArray>,
    targetWidth: Int,
    targetHeight: Int,
    originalWidth: Int,
    originalHeight: Int,
): Bitmap {
    val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(output)

    val scaleX = originalWidth.toFloat() / targetWidth
    val scaleY = originalHeight.toFloat() / targetHeight

    val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    val keypointPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    for (detection in detections) {
        val ymin = detection[0] * targetHeight * scaleY
        val xmin = detection[1] * targetWidth * scaleX
        val ymax = detection[2] * targetHeight * scaleY
        val xmax = detection[3] * targetWidth * scaleX

        // Bounding box
        canvas.drawRect(
            xmin,
            ymin,
            xmax,
            ymax,
            boxPaint
        )

        // 6 keypoints
        for (k in 0 until 6) {
            val kpX =
                detection[4 + k * 2] *
                        targetWidth *
                        scaleX

            val kpY =
                detection[4 + k * 2 + 1] *
                        targetHeight *
                        scaleY

            val radius = 2f

            canvas.drawCircle(
                kpX,
                kpY,
                radius,
                keypointPaint
            )
        }
    }

    return output
}