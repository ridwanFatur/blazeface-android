package com.ridwanfatur.blazefaceonnx.camera.simple_inference

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@Composable
fun ImageSimpleOnnxInferencePage(
    inference: ImageSimpleOnnxInference,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var outputShape by remember {
        mutableStateOf("")
    }

    var selectedImage by remember {
        mutableStateOf<Bitmap?>(null)
    }

    var originalWidth by remember {
        mutableIntStateOf(0)
    }

    var originalHeight by remember {
        mutableIntStateOf(0)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }

            if (bitmap != null) {
                originalWidth = bitmap.width
                originalHeight = bitmap.height

                val resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    128,
                    128,
                    true
                )

                val pixels = IntArray(128 * 128)

                resizedBitmap.getPixels(
                    pixels,
                    0,
                    128,
                    0,
                    0,
                    128,
                    128
                )

                val input = FloatArray(3 * 128 * 128)

                for (i in pixels.indices) {
                    val pixel = pixels[i]

                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    input[i] = r.toFloat()
                    input[128 * 128 + i] = g.toFloat()
                    input[2 * 128 * 128 + i] = b.toFloat()
                }

                val output = inference.predict(input)

                outputShape =
                    "[${output.size}, ${output[0].size}, ${output[0][0].size}]"

                val detections = imagePostProcess(output)

                selectedImage = visualize(
                    bitmap = bitmap,
                    detections = detections,
                    targetWidth = 128,
                    targetHeight = 128,
                    originalWidth = originalWidth,
                    originalHeight = originalHeight
                )
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = {
                imagePicker.launch("image/*")
            }
        ) {
            Text("Read Storage")
        }

        Button(
            onClick = {
                val input = FloatArray(3 * 128 * 128)

                val output = inference.predict(input)

                outputShape = "[${output.size}, ${output[0].size}, ${output[0][0].size}]"
            }
        ) {
            Text("Run Inference")
        }

        TextField(
            value = outputShape,
            onValueChange = {
                outputShape = it
            },
            label = {
                Text("Output")
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (originalWidth > 0 && originalHeight > 0) {
            Text(
                text = "Original size: ${originalWidth} x $originalHeight"
            )
        }

        selectedImage?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}