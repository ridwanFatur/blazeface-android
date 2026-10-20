package com.ridwanfatur.blazefaceonnx.camera.camera_inference.utils

import android.graphics.Bitmap

fun cropCenterToAspectRatio(
    bitmap: Bitmap,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    if (targetWidth <= 0 || targetHeight <= 0) {
        return bitmap
    }

    val bitmapRatio =
        bitmap.width.toFloat() / bitmap.height

    val targetRatio =
        targetWidth.toFloat() / targetHeight

    return if (bitmapRatio > targetRatio) {
        // Bitmap is wider -> crop left/right.
        val cropWidth =
            (bitmap.height * targetRatio).toInt()

        val x =
            (bitmap.width - cropWidth) / 2

        Bitmap.createBitmap(
            bitmap,
            x,
            0,
            cropWidth,
            bitmap.height
        )
    } else {
        // Bitmap is taller -> crop top/bottom.
        val cropHeight =
            (bitmap.width / targetRatio).toInt()

        val y =
            (bitmap.height - cropHeight) / 2

        Bitmap.createBitmap(
            bitmap,
            0,
            y,
            bitmap.width,
            cropHeight
        )
    }
}