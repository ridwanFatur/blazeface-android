package com.ridwanfatur.blazefaceonnx.camera.camera_check

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

private fun cropCenterToAspectRatio(
    bitmap: Bitmap,
    targetWidth: Int,
    targetHeight: Int
): Bitmap {
    if (targetWidth <= 0 || targetHeight <= 0) {
        return bitmap
    }

    val bitmapRatio = bitmap.width.toFloat() / bitmap.height
    val targetRatio = targetWidth.toFloat() / targetHeight

    return if (bitmapRatio > targetRatio) {
        // Bitmap is wider -> crop left/right.
        val cropWidth = (bitmap.height * targetRatio).toInt()
        val x = (bitmap.width - cropWidth) / 2

        Bitmap.createBitmap(
            bitmap,
            x,
            0,
            cropWidth,
            bitmap.height
        )
    } else {
        // Bitmap is taller -> crop top/bottom.
        val cropHeight = (bitmap.width / targetRatio).toInt()
        val y = (bitmap.height - cropHeight) / 2

        Bitmap.createBitmap(
            bitmap,
            0,
            y,
            bitmap.width,
            cropHeight
        )
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current

    var previewView by remember {
        mutableStateOf<PreviewView?>(null)
    }

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var cameraOptions by remember {
        mutableStateOf<List<CameraOption>>(emptyList())
    }

    var selectedCamera by remember {
        mutableStateOf<CameraOption?>(null)
    }

    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()

        val options = buildList {
            val front = CameraOption(
                name = "Front Camera",
                lensFacing = CameraSelector.LENS_FACING_FRONT
            )

            val back = CameraOption(
                name = "Back Camera",
                lensFacing = CameraSelector.LENS_FACING_BACK
            )

            if (cameraProvider.hasCamera(
                    CameraSelector.Builder()
                        .requireLensFacing(front.lensFacing)
                        .build()
                )
            ) {
                add(front)
            }

            if (cameraProvider.hasCamera(
                    CameraSelector.Builder()
                        .requireLensFacing(back.lensFacing)
                        .build()
                )
            ) {
                add(back)
            }
        }

        cameraOptions = options

        if (selectedCamera == null) {
            selectedCamera = options.firstOrNull()
        }
    }

    LaunchedEffect(
        previewView,
        selectedCamera
    ) {
        val view = previewView ?: return@LaunchedEffect
        val camera = selectedCamera ?: return@LaunchedEffect

        val cameraProvider = cameraProviderFuture.get()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(camera.lensFacing)
            .build()

        val preview = Preview.Builder()
            .build()
            .also {
                it.surfaceProvider = view.surfaceProvider
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(
                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            )
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            try {
                var frame = imageProxy.toBitmap()

                val rotation = imageProxy.imageInfo.rotationDegrees

                if (rotation != 0) {
                    val matrix = Matrix()
                    matrix.postRotate(rotation.toFloat())

                    frame = Bitmap.createBitmap(
                        frame,
                        0,
                        0,
                        frame.width,
                        frame.height,
                        matrix,
                        true
                    )
                }

                if (camera.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    val matrix = Matrix()
                    matrix.postScale(-1f, 1f)

                    frame = Bitmap.createBitmap(
                        frame,
                        0,
                        0,
                        frame.width,
                        frame.height,
                        matrix,
                        true
                    )
                }

                // Match the crop used by PreviewView.ScaleType.FILL_CENTER.
                frame = cropCenterToAspectRatio(
                    bitmap = frame,
                    targetWidth = view.width,
                    targetHeight = view.height
                )

                bitmap = frame
            } finally {
                imageProxy.close()
            }
        }

        cameraProvider.unbindAll()

        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).also { view ->
                    view.scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = view
                }
            }
        )

        CameraDropdown(
            cameras = cameraOptions,
            selectedCamera = selectedCamera,
            onCameraSelected = {
                selectedCamera = it
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        bitmap?.let { frame ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(140.dp)
                    .background(Color.Black)
                    .padding(2.dp)
            ) {
                Image(
                    bitmap = frame.asImageBitmap(),
                    contentDescription = "Latest camera frame",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
