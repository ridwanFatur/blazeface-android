# BlazeFace ONNX – Android

An Android application that performs real-time face detection using **BlazeFace with ONNX Runtime**.

This project integrates a pretrained BlazeFace ONNX model into an Android application, allowing face detection through camera input and image files from device storage.

The ONNX model used in this project was exported from the original PyTorch implementation in the following repository:

[**BlazeFace ONNX – PyTorch to ONNX Conversion**](https://github.com/ridwanFatur/blazeface-onnx)

## Features

- **Real-time Face Detection** – Perform face detection using live camera input.
- **Storage-based Inference** – Load images from device storage and run face detection.
- **Camera Debugging** – A dedicated page for checking camera functionality and debugging camera-related issues.
- **Simple Inference** – A dedicated page for testing and debugging ONNX model inference independently.

## Repository Structure

```text
src/
├── androidTest/
│   └── java/com/ridwanfatur/blazefaceonnx/
└── main/
    ├── assets/
    │   ├── blaze_face.onnx
    │   └── blaze_face.onnx.data
    └── java/com/ridwanfatur/blazefaceonnx/
        ├── blaze_face/
        │   ├── camera_check/
        │   ├── camera_inference/
        │   ├── read_storage/
        │   ├── simple_inference/
        │   └── BlazeFacePage.kt
        ├── ui/
        │   └── theme/
        └── MainActivity.kt
```

## Project Structure

### Model Assets

The `assets/` directory contains the ONNX model and its external tensor data.

- `blaze_face.onnx` – Exported BlazeFace ONNX model.
- `blaze_face.onnx.data` – External tensor data required by the ONNX model.

These files are generated from the PyTorch-to-ONNX conversion process in the [original repository](https://github.com/ridwanFatur/blazeface-onnx).

### BlazeFace Pages

The `blaze_face/` directory contains the different inference and debugging implementations.

| Directory / File | Description |
|---|---|
| `camera_check/` | Camera debugging page for checking camera functionality and input handling independently. |
| `camera_inference/` | Performs face detection using real-time camera input. |
| `read_storage/` | Handles image input from device storage for face detection. |
| `simple_inference/` | A simplified inference page for debugging and testing the ONNX model independently. |
| `BlazeFacePage.kt` | Main BlazeFace page that organizes the available functionality. |

### Application Entry Point

- `MainActivity.kt` – Main Android activity and application entry point.
- `ui/theme/` – UI theme configuration.

## Inference Workflow

The application supports two primary image input sources:

**1. Camera Input**

```text
Camera
   │
   ▼
Capture Image Frames
   │
   ▼
Image Preprocessing
   │
   ▼
ONNX Runtime
   │
   ▼
BlazeFace Inference
   │
   ▼
Post-processing
   │
   ▼
Face Detection Results
```

This pipeline allows the application to perform face detection continuously using live camera frames.

**2. Storage Input**

```text
Device Storage
      │
      ▼
Select Image
      │
      ▼
Image Preprocessing
      │
      ▼
ONNX Runtime
      │
      ▼
BlazeFace Inference
      │
      ▼
Post-processing
      │
      ▼
Face Detection Results
```

This workflow allows users to select an image from device storage and run face detection without requiring a live camera feed.

## Debugging Pages

Some pages are intentionally separated from the main inference workflow to make development and troubleshooting easier.

- **Camera Check** – Used to inspect and debug camera functionality without involving the complete face detection pipeline.
- **Simple Inference** – Used to test the ONNX model inference independently from camera integration.
- **Camera Inference** – Combines camera input processing with the BlazeFace inference pipeline.

This separation makes it easier to identify whether an issue originates from camera handling, image preprocessing, or ONNX model inference.

## Model Source

The ONNX model is not trained directly within this Android project.

It is generated from the PyTorch BlazeFace implementation in the following repository:

[https://github.com/ridwanFatur/blazeface-onnx](https://github.com/ridwanFatur/blazeface-onnx)

That repository covers the model adaptation, ONNX export, and validation process.

## Objective

This project demonstrates how a pretrained computer vision model can be integrated into an Android application using ONNX Runtime.

The main objective is to run BlazeFace face detection directly on Android devices, supporting both real-time camera inference and image-based inference from device storage, while maintaining separate debugging workflows for easier development and testing.
