# YOLO Android App

Android application for real-time object detection, segmentation, and pose estimation using YOLO models.

## Features

- **Detection**: Object detection with bounding boxes
- **Segment**: Instance segmentation with masks
- **Pose**: Human pose estimation with keypoints
- Real-time camera inference
- Class filtering for detection and segmentation
- **CPU/GPU selection**: Choose between CPU or GPU for inference
- **FPS display**: Real-time FPS counter in the top-right corner
- **Processing indicator**: Visual feedback during model loading
- GPU acceleration support (default)

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

## Usage

- **Task Selection**: Use the spinner at the bottom to switch between Detection, Segment, and Pose modes
- **CPU/GPU Selection**: Use the device spinner to choose between CPU or GPU inference (default: GPU)
- **Class Filtering**: Click the "Classes" button (available for Detection and Segment) to filter detected classes
- **FPS Display**: Real-time FPS is shown in the top-right corner
- **Model Loading**: A "Processing..." indicator appears during model loading or switching

## Models

Download YOLO models from [Ultralytics](https://github.com/ultralytics/ultralytics) and convert to [TensorFlow Lite](https://github.com/ultralytics/yolo-flutter-app/releases/tag/v0.2.0) format.

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
