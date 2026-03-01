# YOLO Android App

Android application for real-time object detection, segmentation, and pose estimation using YOLO models.

## Features

- **Detection**: Object detection with bounding boxes
- **Segment**: Instance segmentation with masks
- **Pose**: Human pose estimation with keypoints
- Real-time camera inference
- Class filtering for detection and segmentation
- GPU acceleration support

## Requirements

- Android SDK 24+
- Camera permission

## Quick Start

1. Clone the repository
2. Place YOLO model files in `app/src/main/assets/`:
   - `yolo11n.tflite` (detection)
   - `yolo11n-seg.tflite` (segmentation)
   - `yolo11n-pose.tflite` (pose)
3. Build and run

## Models

Download YOLO models from [Ultralytics](https://github.com/ultralytics/ultralytics) and convert to TensorFlow Lite format.

## Build

```bash
./gradlew assembleDebug
```

## Technologies

- Kotlin
- CameraX
- TensorFlow Lite
- YOLO (Ultralytics)

## Acknowledgments

Thanks to [Ultralytics](https://github.com/ultralytics/ultralytics) for the YOLO models and framework.
