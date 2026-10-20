package com.ridwanfatur.blazefaceonnx.blaze_face

import android.content.Context
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ridwanfatur.blazefaceonnx.camera.camera_check.CameraCheckRealTimePage
import com.ridwanfatur.blazefaceonnx.camera.camera_inference.CameraInferencePage
import com.ridwanfatur.blazefaceonnx.camera.camera_inference.CameraOnnxInference
import com.ridwanfatur.blazefaceonnx.camera.read_storage.ReadStoragePage
import com.ridwanfatur.blazefaceonnx.camera.simple_inference.ImageSimpleOnnxInference
import com.ridwanfatur.blazefaceonnx.camera.simple_inference.ImageSimpleOnnxInferencePage

fun NavGraphBuilder.blazefaceNavigation(
    navController: NavHostController,
    context: Context
) {
    composable("blazeface") {
        CameraPage(
            onCameraCheckClick = {
                navController.navigate(
                    "camera_check_real_time"
                )
            },
            onReadStorageClick = {
                navController.navigate(
                    "read_storage"
                )
            },
            onSimpleInferenceClick = {
                navController.navigate(
                    "image_simple_onnx_inference"
                )
            },
            onCameraInferenceClick = {
                navController.navigate(
                    "camera_onnx_inference"
                )
            }
        )
    }	

    composable("camera_check_real_time") {
        CameraCheckRealTimePage()
    }

    composable("read_storage") {
        ReadStoragePage()
    }

    composable("image_simple_onnx_inference") {
        ImageSimpleOnnxInferencePage(
            inference =
                ImageSimpleOnnxInference(context)
        )
    }

    composable("camera_onnx_inference") {
        CameraInferencePage(
            inference =
                CameraOnnxInference(context)
        )
    }	
}

@Composable
fun CameraPage(
    onCameraCheckClick: () -> Unit,
    onReadStorageClick: () -> Unit,
    onSimpleInferenceClick: () -> Unit,
    onCameraInferenceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onCameraCheckClick
        ) {
            Text("Camera Check Real Time")
        }

        Button(
            onClick = onReadStorageClick
        ) {
            Text("Read Storage")
        }

        Button(
            onClick = onSimpleInferenceClick
        ) {
            Text("Simple Onnx Inference")
        }

        Button(
            onClick = onCameraInferenceClick
        ) {
            Text("Camera Onnx Inference")
        }
    }
}