package com.example.mysignmate.utils // Or your chosen package

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class VideoProcessorUtil(
    private val context: Context,
    private val processingListener: ProcessingListener? = null // Optional listener for results
) {

    // --- Configuration Constants ---
    companion object {
        private const val TAG = "VideoProcessorUtil"
        // Adjust these paths based on where your models are in the assets folder
        private const val HAND_LANDMARKER_BUNDLE = "hand_landmarker.task"
        private const val POSE_LANDMARKER_BUNDLE = "pose_landmarker_full.task"

        private const val MAX_NUM_HANDS = 2 // Or 1 if you need only one hand
        private const val MIN_HAND_DETECTION_CONFIDENCE = 0.5f
        private const val MIN_HAND_TRACKING_CONFIDENCE = 0.5f
        private const val MIN_HAND_PRESENCE_CONFIDENCE = 0.5f

    }

    // --- MediaPipe Landmarker Instances ---
    private var handLandmarker: HandLandmarker? = null
     private var poseLandmarker: PoseLandmarker? = null

    // --- Initialization Block ---
    init {
        setupHandLandmarker()
        setupPoseLandmarker()
    }

    // --- Listener Interface (Optional) ---
    // This allows the calling Activity/Fragment to get results asynchronously
    interface ProcessingListener {
        fun onHandLandmarksFound(result: HandLandmarkerResult, image: Bitmap)
        fun onPoseLandmarksFound(result: PoseLandmarkerResult, image: Bitmap)
        fun onError(error: String, errorCode: Int = 0)
    }

    // --- Setup Methods ---
    private fun setupHandLandmarker() {
        try {
            val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath(HAND_LANDMARKER_BUNDLE)
            // If using a delegate (e.g., GPU)
            // baseOptionsBuilder.setDelegate(Delegate.GPU) // Or Delegate.CPU

            val baseOptions = baseOptionsBuilder.build()

            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(MAX_NUM_HANDS)
                .setMinHandDetectionConfidence(MIN_HAND_DETECTION_CONFIDENCE)
//                .setMinHandTrackingConfidence(MIN_HAND_TRACKING_CONFIDENCE)
                .setMinHandPresenceConfidence(MIN_HAND_PRESENCE_CONFIDENCE)
                // For processing video frames (not a live stream with a listener)
                .setRunningMode(RunningMode.IMAGE) // Or RunningMode.VIDEO for frame-by-frame video files

            // If using RunningMode.LIVE_STREAM, you'd set a listener here:
            // .setRunningMode(RunningMode.LIVE_STREAM)
            // .setResultListener { result, image ->
            //     processingListener?.onHandLandmarksFound(result, image)
            // }
            // .setErrorListener { error ->
            //     processingListener?.onError(error.message ?: "Unknown MediaPipe Error", error.errorCode)
            // }

            val options = optionsBuilder.build()
            handLandmarker = HandLandmarker.createFromOptions(context, options)
            Log.d(TAG, "Hand Landmarker initialized successfully.")

        } catch (e: java.lang.Exception) {
            val errorMessage = "Failed to initialize Hand Landmarker: ${e.message}"
            Log.e(TAG, errorMessage, e)
            processingListener?.onError(errorMessage)
        }
    }

    private fun setupPoseLandmarker() {
        try {
            val baseOptionsBuilder = BaseOptions.builder().setModelAssetPath(POSE_LANDMARKER_BUNDLE)
            val baseOptions = baseOptionsBuilder.build()

            // PoseLandmarkerOptions allows configuring model complexity, segmentation, etc.
            val optionsBuilder = PoseLandmarker.PoseLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE) // Or RunningMode.VIDEO
                .setNumPoses(1) // Or more if needed
            // .setOutputSegmentationMasks(false) // Optional
            // Refer to PoseLandmarkerOptions for more configurations

            val options = optionsBuilder.build()
            poseLandmarker = PoseLandmarker.createFromOptions(context, options)
            android.util.Log.d(TAG, "Pose Landmarker initialized successfully.")

        } catch (e: java.lang.Exception) {
            val errorMessage = "Failed to initialize Pose Landmarker: ${e.message}"
            android.util.Log.e(TAG, errorMessage, e)
            processingListener?.onError(errorMessage)
        }
    }

    // --- Public Processing Function(s) ---

    /**
     * Detects hand landmarks from a single Bitmap image.
     * This function is synchronous and should be called from a background thread.
     *
     * @param bitmap The input image.
     * @return HandLandmarkerResult containing the detected landmarks, or null if an error occurs or no hands are found.
     */
    fun detectHandLandmarks(bitmap: Bitmap): HandLandmarkerResult? {
        if (handLandmarker == null) {
            Log.e(TAG, "Hand Landmarker is not initialized.")
            processingListener?.onError("Hand Landmarker not initialized.")
            return null
        }

        try {
            // MediaPipe HandLandmarker for IMAGE mode performs detection synchronously
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = handLandmarker?.detect(mpImage) // For RunningMode.IMAGE

            // If you were using RunningMode.VIDEO for processing a video file frame by frame:
            // val frameTimestampMs = System.currentTimeMillis() // Or a timestamp from the video frame
            // val result = handLandmarker?.detectForVideo(bitmap, frameTimestampMs)

            if (result == null) {
                Log.d(TAG, "No hand landmarks detected in the provided image.")
                // It's not necessarily an error if no hands are found,
                // but you might want to notify if the listener expects continuous updates.
            } else {
                Log.d(TAG, "Hand landmarks detected successfully.")
                // Optionally, call listener immediately if not using LIVE_STREAM
                processingListener?.onHandLandmarksFound(result, bitmap)
            }
            return result

        } catch (e: java.lang.Exception) {
            val errorMessage = "Error during hand landmark detection: ${e.message}"
            Log.e(TAG, errorMessage, e)
            processingListener?.onError(errorMessage)
            return null
        }
    }

    fun detectPoseLandmarks(bitmap: android.graphics.Bitmap): PoseLandmarkerResult? {
        if (poseLandmarker == null) {
            android.util.Log.e(TAG, "Pose Landmarker is not initialized.")
            processingListener?.onError("Pose Landmarker not initialized.")
            return null
        }
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            val result = poseLandmarker?.detect(mpImage) // For RunningMode.IMAGE
            // If using RunningMode.VIDEO:
            // val frameTimestampMs = System.currentTimeMillis()
            // val result = poseLandmarker?.detectForVideo(bitmap, frameTimestampMs)

            if (result == null || result.landmarks().isEmpty()) { // Check if landmarks list is empty
                android.util.Log.d(TAG, "No pose landmarks detected in the provided image.")
            } else {
                android.util.Log.d(TAG, "Pose landmarks detected successfully.")
                processingListener?.onPoseLandmarksFound(result, bitmap) // Notify listener
            }
            return result
        } catch (e: java.lang.Exception) {
            val errorMessage = "Error during pose landmark detection: ${e.message}"
            android.util.Log.e(TAG, errorMessage, e)
            processingListener?.onError(errorMessage)
            return null
        }
    }

    /**
     * Closes the landmarker instances to free up resources.
     * Call this when the helper is no longer needed (e.g., in Activity's onDestroy).
     */
    fun close() {
        handLandmarker?.close()
        handLandmarker = null
        android.util.Log.d(TAG, "Hand Landmarker closed.")

        poseLandmarker?.close()   // <-- CLOSE POSE LANDMARKER
        poseLandmarker = null
        android.util.Log.d(TAG, "Pose Landmarker closed.")
    }
}