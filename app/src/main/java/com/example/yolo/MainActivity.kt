package com.example.yolo

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.ultralytics.yolo.YOLOView
import com.ultralytics.yolo.YOLOTask

class MainActivity : AppCompatActivity(), LifecycleOwner {
    
    private val TAG = "MainActivity"
    private lateinit var yoloView: YOLOView
    private lateinit var taskSpinner: Spinner
    private lateinit var yoloViewContainer: FrameLayout
    private lateinit var classButton: Button
    
    // Task configuration: [Display Name, Model File, YOLOTask]
    private val tasks = listOf(
        Triple("Detection", "yolo11n.tflite", YOLOTask.DETECT),
        Triple("Segment", "yolo11n-seg.tflite", YOLOTask.SEGMENT),
        Triple("Pose", "yolo11n-pose.tflite", YOLOTask.POSE)
    )
    
    private var currentTaskIndex = 0
    private var availableClasses: List<String> = emptyList()
    private val classCheckBoxes = mutableMapOf<String, CheckBox>()
    private val enabledClasses = mutableSetOf<String>()
    private var classPopupWindow: PopupWindow? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "onCreate: Starting initialization")
            
            // Set content view with layout
            setContentView(R.layout.activity_main)
            
            // Initialize views
            taskSpinner = findViewById(R.id.taskSpinner)
            yoloViewContainer = findViewById(R.id.yoloViewContainer)
            classButton = findViewById(R.id.classButton)
            
            // Create YOLOView
            yoloView = YOLOView(this)
            Log.d(TAG, "onCreate: YOLOView created")
            
            // Add YOLOView to container with match_parent layout
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            yoloViewContainer.addView(yoloView, layoutParams)
            
            // Set lifecycle owner for camera
            yoloView.onLifecycleOwnerAvailable(this)
            Log.d(TAG, "onCreate: Lifecycle owner set")
            
            // Setup spinner
            setupTaskSpinner()
            
            // Setup class button
            setupClassButton()
            
            // Load initial model
            loadModel(0)
            
            Log.d(TAG, "onCreate: Content view set")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Exception occurred", e)
            e.printStackTrace()
            throw e
        }
    }
    
    private fun setupTaskSpinner() {
        // Create adapter with task display names
        val taskNames = tasks.map { it.first }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, taskNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        taskSpinner.adapter = adapter
        
        // Set selection listener
        taskSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != currentTaskIndex) {
                    Log.d(TAG, "Task changed from ${tasks[currentTaskIndex].first} to ${tasks[position].first}")
                    currentTaskIndex = position
                    loadModel(position)
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    
    private fun setupClassButton() {
        classButton.setOnClickListener {
            showClassSelectionPopup()
        }
    }
    
    private fun showClassSelectionPopup() {
        // Dismiss existing popup if any
        classPopupWindow?.dismiss()
        
        // Inflate popup layout
        val popupView = layoutInflater.inflate(R.layout.class_selection_popup, null)
        val classContainer = popupView.findViewById<LinearLayout>(R.id.classContainer)
        val closeButton = popupView.findViewById<ImageButton>(R.id.closeButton)
        val selectAllButton = popupView.findViewById<Button>(R.id.selectAllButton)
        val deselectAllButton = popupView.findViewById<Button>(R.id.deselectAllButton)
        
        // Setup close button
        closeButton.setOnClickListener {
            classPopupWindow?.dismiss()
        }
        
        // Setup select all button
        selectAllButton.setOnClickListener {
            selectAllClasses()
        }
        
        // Setup deselect all button
        deselectAllButton.setOnClickListener {
            deselectAllClasses()
        }
        
        // Setup class checkboxes
        setupClassCheckBoxes(classContainer)
        
        // Calculate height: 6 checkboxes visible (each ~40dp), plus header, button bar and padding
        val checkboxHeight = (40 * resources.displayMetrics.density).toInt()
        val headerHeight = (56 * resources.displayMetrics.density).toInt()
        val buttonBarHeight = (48 * resources.displayMetrics.density).toInt()
        val maxHeight = checkboxHeight * 6 + headerHeight + buttonBarHeight + 32 // 6 checkboxes + header + button bar + padding
        
        // Create popup window
        classPopupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            // Set max height
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(resources.displayMetrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(maxHeight, View.MeasureSpec.AT_MOST)
            )
            height = popupView.measuredHeight.coerceAtMost(maxHeight)
            
            // Set background
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            isOutsideTouchable = true
            isFocusable = true
            
            // Animation - slide up from bottom
            animationStyle = android.R.style.Animation_Translucent
            
            // Show at bottom
            showAtLocation(
                findViewById(android.R.id.content),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                0,
                0
            )
        }
    }
    
    private fun setupClassCheckBoxes(container: LinearLayout) {
        // Clear existing checkboxes
        container.removeAllViews()
        classCheckBoxes.clear()
        
        // Create checkboxes for each class
        availableClasses.forEach { className ->
            val checkBox = CheckBox(this).apply {
                text = className
                isChecked = className in enabledClasses
                setPadding(16, 12, 16, 12)
                textSize = 14f
                minHeight = (40 * resources.displayMetrics.density).toInt()
            }
            
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    enabledClasses.add(className)
                } else {
                    enabledClasses.remove(className)
                }
                // Update YOLOView with enabled classes
                yoloView.setEnabledClasses(enabledClasses.toSet())
                Log.d(TAG, "Class selection updated: ${enabledClasses.size} classes enabled")
            }
            
            classCheckBoxes[className] = checkBox
            container.addView(checkBox)
        }
    }
    
    private fun selectAllClasses() {
        // Select all classes
        enabledClasses.clear()
        enabledClasses.addAll(availableClasses)
        
        // Update all checkboxes
        classCheckBoxes.values.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener(null) // Temporarily remove listener
            checkBox.isChecked = true
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val className = checkBox.text.toString()
                if (isChecked) {
                    enabledClasses.add(className)
                } else {
                    enabledClasses.remove(className)
                }
                yoloView.setEnabledClasses(enabledClasses.toSet())
                Log.d(TAG, "Class selection updated: ${enabledClasses.size} classes enabled")
            }
        }
        
        // Update YOLOView
        yoloView.setEnabledClasses(enabledClasses.toSet())
        Log.d(TAG, "All classes selected: ${enabledClasses.size} classes")
    }
    
    private fun deselectAllClasses() {
        // Deselect all classes
        enabledClasses.clear()
        
        // Update all checkboxes
        classCheckBoxes.values.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener(null) // Temporarily remove listener
            checkBox.isChecked = false
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                val className = checkBox.text.toString()
                if (isChecked) {
                    enabledClasses.add(className)
                } else {
                    enabledClasses.remove(className)
                }
                yoloView.setEnabledClasses(enabledClasses.toSet())
                Log.d(TAG, "Class selection updated: ${enabledClasses.size} classes enabled")
            }
        }
        
        // Update YOLOView
        yoloView.setEnabledClasses(enabledClasses.toSet())
        Log.d(TAG, "All classes deselected")
    }
    
    private fun loadModel(taskIndex: Int) {
        val (taskName, modelFile, task) = tasks[taskIndex]
        Log.d(TAG, "Loading model: $modelFile for task: $taskName")
        
        // Lower confidence threshold for testing
        yoloView.setConfidenceThreshold(0.15)
        
        // Stop current camera if running
        yoloView.stop()
        
        // Show/hide class selection button based on task
        val showClassSelection = task == YOLOTask.DETECT || task == YOLOTask.SEGMENT
        classButton.visibility = if (showClassSelection) View.VISIBLE else View.GONE
        
        // Dismiss popup if switching to POSE
        if (!showClassSelection) {
            classPopupWindow?.dismiss()
        }
        
        // Load new model
        yoloView.setModel(modelFile, task, useGpu = true) { success ->
            Log.d(TAG, "Model load callback - success: $success for task: $taskName")
            if (success) {
                // Get available classes from the model
                if (showClassSelection) {
                    // Get labels from YOLOView after model loads
                    // Use a small delay to ensure predictor is set
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        availableClasses = yoloView.getAvailableClasses()
                        if (availableClasses.isEmpty()) {
                            // Fallback to COCO classes if model doesn't provide labels
                            availableClasses = getCOCOClasses()
                        }
                        // Initialize all classes as enabled
                        enabledClasses.clear()
                        enabledClasses.addAll(availableClasses)
                        yoloView.setEnabledClasses(enabledClasses.toSet())
                        Log.d(TAG, "Available classes loaded: ${availableClasses.size}")
                    }, 100)
                } else {
                    // Clear class selection for POSE
                    enabledClasses.clear()
                    yoloView.setEnabledClasses(emptySet())
                }
                
                // Model loaded successfully, initialize camera
                Log.d(TAG, "Model loaded, initializing camera")
                yoloView.initCamera()
            } else {
                Log.e(TAG, "Model loading failed for task: $taskName")
                // Try to start camera even if model loading failed
                yoloView.initCamera()
            }
        }
    }
    
    private fun getCOCOClasses(): List<String> {
        // COCO dataset 80 classes
        return listOf(
            "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat",
            "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", "dog",
            "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", "umbrella",
            "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", "kite",
            "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", "bottle",
            "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", "sandwich",
            "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", "couch",
            "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", "remote",
            "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", "book",
            "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
        )
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward permission result to YOLOView
        yoloView.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    
    override fun onResume() {
        super.onResume()
        // Start camera when activity resumes
        yoloView.startCamera()
    }
    
    override fun onPause() {
        super.onPause()
        // Dismiss popup if showing
        classPopupWindow?.dismiss()
        // Stop camera when activity pauses
        yoloView.stop()
    }
}


