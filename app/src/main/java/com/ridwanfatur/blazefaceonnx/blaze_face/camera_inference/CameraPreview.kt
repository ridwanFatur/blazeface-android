package com.ridwanfatur.blazefaceonnx.camera.camera_inference

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
import com.ridwanfatur.blazefaceonnx.camera.camera_inference.utils.DetectionOverlay
import com.ridwanfatur.blazefaceonnx.camera.camera_inference.utils.bitmapToFloatArray
import com.ridwanfatur.blazefaceonnx.camera.camera_inference.utils.cropCenterToAspectRatio
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    inference: CameraOnnxInference,
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current

    var previewView by remember {
        mutableStateOf<PreviewView?>(null)
    }

    var currentBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var cameraInferenceOptions by remember {
        mutableStateOf<List<CameraInferenceOption>>(emptyList())
    }

    var selectedCamera by remember {
        mutableStateOf<CameraInferenceOption?>(null)
    }

    var detections by remember {
        mutableStateOf(emptyArray<FloatArray>())
    }

    val cameraExecutor = remember {
        Executors.newSingleThreadExecutor()
    }

    val detectionChannel = remember {
        Channel<Array<FloatArray>>(capacity = Channel.CONFLATED)
    }

    val bitmapChannel = remember {
        Channel<Bitmap>(capacity = Channel.CONFLATED)
    }

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            detectionChannel.close()
            bitmapChannel.close()
            cameraExecutor.shutdown()
            inference.close()
        }
    }

    /*
     * Receive inference results.
     */
    LaunchedEffect(Unit) {
        detectionChannel
            .receiveAsFlow()
            .collect { result ->
                detections = result
            }
    }

    /*
     * Receive latest camera frame.
     */
    LaunchedEffect(Unit) {
        bitmapChannel
            .receiveAsFlow()
            .collect { frame ->
                val oldBitmap = currentBitmap

                currentBitmap = frame

                if (
                    oldBitmap != null &&
                    oldBitmap !== frame &&
                    !oldBitmap.isRecycled
                ) {
                    oldBitmap.recycle()
                }
            }
    }

    /*
     * Find available cameras.
     */
    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()

        val options = buildList {
            val front = CameraInferenceOption(
                name = "Front Camera",
                lensFacing = CameraSelector.LENS_FACING_FRONT
            )

            val back = CameraInferenceOption(
                name = "Back Camera",
                lensFacing = CameraSelector.LENS_FACING_BACK
            )

            if (
                cameraProvider.hasCamera(
                    CameraSelector.Builder()
                        .requireLensFacing(front.lensFacing)
                        .build()
                )
            ) {
                add(front)
            }

            if (
                cameraProvider.hasCamera(
                    CameraSelector.Builder()
                        .requireLensFacing(back.lensFacing)
                        .build()
                )
            ) {
                add(back)
            }
        }

        cameraInferenceOptions = options

        if (selectedCamera == null) {
            selectedCamera = options.firstOrNull()
        }
    }

    /*
     * Bind CameraX.
     */
    LaunchedEffect(
        previewView,
        selectedCamera
    ) {
        val view = previewView
            ?: return@LaunchedEffect

        val camera = selectedCamera
            ?: return@LaunchedEffect

        val cameraProvider = cameraProviderFuture.get()

        val cameraSelector =
            CameraSelector.Builder()
                .requireLensFacing(camera.lensFacing)
                .build()

        val preview =
            Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = view.surfaceProvider
                }

        val imageAnalysis =
            ImageAnalysis.Builder()
                .setBackpressureStrategy(
                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                )
                .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            var frame: Bitmap? = null

            try {
                frame = imageProxy.toBitmap()

                /*
                 * Rotate according to CameraX.
                 */
                val rotation =
                    imageProxy.imageInfo.rotationDegrees

                if (rotation != 0) {
                    val matrix = Matrix()
                    matrix.postRotate(rotation.toFloat())

                    val rotated =
                        Bitmap.createBitmap(
                            frame,
                            0,
                            0,
                            frame.width,
                            frame.height,
                            matrix,
                            true
                        )

                    if (rotated !== frame) {
                        frame.recycle()
                    }

                    frame = rotated
                }

                /*
                 * Mirror front camera.
                 */
                if (
                    camera.lensFacing ==
                    CameraSelector.LENS_FACING_FRONT
                ) {
                    val matrix = Matrix()
                    matrix.postScale(-1f, 1f)

                    val mirrored =
                        Bitmap.createBitmap(
                            frame,
                            0,
                            0,
                            frame.width,
                            frame.height,
                            matrix,
                            true
                        )

                    if (mirrored !== frame) {
                        frame.recycle()
                    }

                    frame = mirrored
                }

                /*
                 * Match PreviewView.ScaleType.FILL_CENTER.
                 */
                val croppedFrame =
                    cropCenterToAspectRatio(
                        bitmap = frame,
                        targetWidth = view.width,
                        targetHeight = view.height
                    )

                if (croppedFrame !== frame) {
                    frame.recycle()
                }

                frame = croppedFrame

                /*
                 * The frame shown in the UI is the same
                 * frame used for inference.
                 */
                val frameForUi = frame

                bitmapChannel.trySend(frameForUi)

                /*
                 * Preprocessing:
                 *
                 * Original aspect ratio is preserved.
                 * Remaining area is padded with black
                 * to 128x128.
                 */
                val input =
                    bitmapToFloatArray(frameForUi)

                /*
                 * ONNX inference.
                 */
                val output =
                    inference.predict(input)

                val processed =
                    imagePostProcess(
                        input = output,
                        minScoreThresh = 0.5f,
                        minSuppressionThreshold = 0.3f
                    )

                /*
                 * Detection coordinates are normalized
                 * relative to the 128x128 letterboxed input.
                 */
                detectionChannel.trySend(processed)

                /*
                 * UI owns this bitmap.
                 */
                frame = null

            } catch (e: Exception) {
                e.printStackTrace()

            } finally {
                frame?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }

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

        /*
         * Real-time CameraX preview.
         */
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).also { view ->
                    view.scaleType =
                        PreviewView.ScaleType.FILL_CENTER

                    previewView = view
                }
            }
        )

        /*
         * Detection overlay.
         */
        DetectionOverlay(
            detections = detections,
            modifier = Modifier.fillMaxSize()
        )

        /*
         * Camera selector.
         */
        CameraInferenceDropdown(
            cameras = cameraInferenceOptions,
            selectedCamera = selectedCamera,
            onCameraSelected = {
                selectedCamera = it
                detections = emptyArray()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )

        /*
         * Latest camera frame.
         */
        currentBitmap?.let { frame ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(140.dp)
                    .background(Color.Black)
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