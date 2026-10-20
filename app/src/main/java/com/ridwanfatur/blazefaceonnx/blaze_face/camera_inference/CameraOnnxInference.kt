package com.ridwanfatur.blazefaceonnx.camera.camera_inference

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import java.io.File
import java.nio.FloatBuffer

class CameraOnnxInference(
    context: Context
) {
    private val environment =
        OrtEnvironment.getEnvironment()

    private val session: OrtSession

    init {
        val modelDir = File(
            context.filesDir,
            "onnx"
        )

        modelDir.mkdirs()

        val modelFile = File(
            modelDir,
            "blaze_face.onnx"
        )

        val dataFile = File(
            modelDir,
            "blaze_face.onnx.data"
        )

        if (!modelFile.exists()) {
            context.assets
                .open("blaze_face.onnx")
                .use { input ->
                    modelFile.outputStream()
                        .use { output ->
                            input.copyTo(output)
                        }
                }
        }

        if (!dataFile.exists()) {
            context.assets
                .open("blaze_face.onnx.data")
                .use { input ->
                    dataFile.outputStream()
                        .use { output ->
                            input.copyTo(output)
                        }
                }
        }

        session = environment.createSession(
            modelFile.absolutePath
        )
    }

    fun predict(
        value: FloatArray
    ): Array<Array<FloatArray>> {
        val shape = longArrayOf(
            1,
            3,
            128,
            128
        )

        val input = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(value),
            shape
        )

        input.use {
            val result = session.run(
                mapOf(
                    "input" to input
                )
            )

            result.use {
                @Suppress("UNCHECKED_CAST")
                return result[0].value
                        as Array<Array<FloatArray>>
            }
        }
    }

    fun close() {
        session.close()
    }
}