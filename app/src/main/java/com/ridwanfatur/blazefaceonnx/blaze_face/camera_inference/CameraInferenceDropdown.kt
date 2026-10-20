package com.ridwanfatur.blazefaceonnx.camera.camera_inference

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun CameraInferenceDropdown(
    cameras: List<CameraInferenceOption>,
    selectedCamera: CameraInferenceOption?,
    onCameraSelected: (CameraInferenceOption) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = modifier
    ) {
        TextButton(
            onClick = {
                expanded = true
            }
        ) {
            Text(
                text = selectedCamera?.name ?: "Camera"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            cameras.forEach { camera ->
                DropdownMenuItem(
                    text = {
                        Text(camera.name)
                    },
                    onClick = {
                        onCameraSelected(camera)
                        expanded = false
                    }
                )
            }
        }
    }
}