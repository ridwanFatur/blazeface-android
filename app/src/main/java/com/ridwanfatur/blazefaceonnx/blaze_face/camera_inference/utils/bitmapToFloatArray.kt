package com.ridwanfatur.blazefaceonnx.camera.camera_inference.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.min

const val INPUT_WIDTH = 128
const val INPUT_HEIGHT = 128

fun bitmapToFloatArray(
    bitmap: Bitmap
): FloatArray {
    val scale = min(
        INPUT_WIDTH.toFloat() / bitmap.width,
        INPUT_HEIGHT.toFloat() / bitmap.height
    )

    val resizedWidth = (bitmap.width * scale).toInt()
    val resizedHeight = (bitmap.height * scale).toInt()

    val resized = Bitmap.createScaledBitmap(
        bitmap,
        resizedWidth,
        resizedHeight,
        true
    )

    val padded = Bitmap.createBitmap(
        INPUT_WIDTH,
        INPUT_HEIGHT,
        Bitmap.Config.ARGB_8888
    )

    padded.eraseColor(Color.BLACK)

    val canvas = Canvas(padded)

    val left = (INPUT_WIDTH - resizedWidth) / 2f
    val top = (INPUT_HEIGHT - resizedHeight) / 2f

    canvas.drawBitmap(
        resized,
        left,
        top,
        Paint(Paint.FILTER_BITMAP_FLAG)
    )

    if (resized !== bitmap) {
        resized.recycle()
    }

    val pixels = IntArray(INPUT_WIDTH * INPUT_HEIGHT)

    padded.getPixels(
        pixels,
        0,
        INPUT_WIDTH,
        0,
        0,
        INPUT_WIDTH,
        INPUT_HEIGHT
    )

    val channelSize = INPUT_WIDTH * INPUT_HEIGHT

    val output = FloatArray(channelSize * 3)

    for (i in pixels.indices) {
        val pixel = pixels[i]

        val r = ((pixel shr 16) and 0xFF).toFloat()
        val g = ((pixel shr 8) and 0xFF).toFloat()
        val b = (pixel and 0xFF).toFloat()

        output[i] = r
        output[channelSize + i] = g
        output[channelSize * 2 + i] = b
    }

    padded.recycle()

    return output
}