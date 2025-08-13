package com.example.mysignmate

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.MediaController
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mysignmate.utils.KeypointProcessorUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pytorch.executorch.Module
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import kotlin.text.isNotBlank
import kotlin.text.trim

class ResultsActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var resultsTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var uploadButton: Button
    private lateinit var backButton3: Button

    private var execuTorchModule: Module? = null
    private val TAG = "ExecuTorchDemo"
    private lateinit var keypointProcessor: KeypointProcessorUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        videoView = findViewById(R.id.replay_video_display)
        setVideoViewDimensions()

        resultsTextView = findViewById(R.id.results_text)
        loadingProgressBar = findViewById(R.id.loading_progress_bar)
        uploadButton = findViewById(R.id.upload_button)
        backButton3 = findViewById(R.id.back_button_3)

        try {
            val modelPath = getAbsolutePath("proposed_model_full.pte") // Make sure filename matches
            execuTorchModule = Module.load(modelPath)
            Log.d(TAG, "ExecuTorch model loaded successfully!")
        } catch (e: IOException) {
            Log.e(TAG, "Error loading ExecuTorch model", e)
            // Handle the error appropriately (e.g., show a message to the user)
            Toast.makeText(this, "Error loading ExecuTorch model", Toast.LENGTH_LONG).show()
        }

        val videoUriString = intent.getStringExtra(DisplayVideoActivity.DISPLAY_VIDEO_URI)

        if (videoUriString != null) {
            val videoUri = Uri.parse(videoUriString)
            setupVideoView(videoUri)
            keypointProcessor = KeypointProcessorUtil(applicationContext)

            // Start processing and prediction
            processVideoAndPredict(videoUri)

        } else {
            Toast.makeText(this, "Error: Video URI not found", Toast.LENGTH_LONG).show()
            finish() // Close the activity if no URI is provided
        }
    }

    private fun showLoading(isLoading: Boolean) {
        // This function must be called from the Main thread
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        // Optionally, disable other UI elements while loading
        videoView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        resultsTextView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        // Disable buttons if needed
        uploadButton.isEnabled = !isLoading
        backButton3.isEnabled = !isLoading
    }

    private fun processVideoAndPredict(videoUri: Uri) {
        lifecycleScope.launch { // Launch on Main, then switch context for IO/Default
            showLoading(true) // Show loading UI (on Main thread)
            resultsTextView.text = "Processing video..." // Initial message (on Main thread)

            val processedKeypoints: List<FloatArray>? = try {
                withContext(Dispatchers.IO) { // Switch to IO for video file processing
                    keypointProcessor.processVideo(videoUri) // This should be a suspend function or run its own IO work
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during keypoint processing", e)
                withContext(Dispatchers.Main) { // Back to Main for UI
                    Toast.makeText(applicationContext, "Error processing video data: ${e.message}", Toast.LENGTH_LONG).show()
                    resultsTextView.text = "Error in processing."
                }
                null
            } finally {
                // Prediction will also take time, so loading might still be true
                // We'll hide loading after prediction OR if keypoint processing failed badly
            }

            if (processedKeypoints != null && processedKeypoints.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    resultsTextView.text = "Landmark processing complete. Running prediction..."
                }
                Log.d(TAG, "Processed keypoints shape: (${processedKeypoints.size}, ${processedKeypoints.firstOrNull()?.size ?: 0})")

                // Run prediction (potentially on Default dispatcher if CPU intensive)
                val predictionResultText: String = withContext(Dispatchers.Default) {
                    runPrediction(processedKeypoints)
                }

                withContext(Dispatchers.Main) { // Back to Main for UI
                    resultsTextView.text = predictionResultText
                    showLoading(false) // Hide loading UI
                }
            } else {
                Log.w(TAG, "Keypoint processing returned null or empty.")
                withContext(Dispatchers.Main) { // Back to Main for UI
                    if (!resultsTextView.text.toString().contains("Error")) { // Avoid overwriting specific error
                        resultsTextView.text = "Could not process landmarks from video."
                    }
                    showLoading(false) // Hide loading UI
                }
            }
        }
    }

    // Extracted prediction logic for clarity
    private fun runPrediction(processedKeypoints: List<FloatArray>): String {
        if (execuTorchModule == null) {
            Log.e(TAG, "ExecuTorch module is not loaded for prediction.")
            return "Error: Model not ready."
        }

        val numberOfFrames = processedKeypoints.size
        val keypointsPerFrame = processedKeypoints.firstOrNull()?.size ?: 0

        if (numberOfFrames == 0 || keypointsPerFrame == 0) {
            Log.w(TAG, "No keypoint data for prediction. Frames: $numberOfFrames, KeypointsPerFrame: $keypointsPerFrame")
            return "Error: No keypoint data for prediction."
        }

        val flatInputArray = FloatArray(1 * numberOfFrames * keypointsPerFrame)
        var index = 0
        processedKeypoints.forEach { frameKeypoints -> // Iterate through each frame's FloatArray
            if (frameKeypoints.size == keypointsPerFrame) { // Sanity check for inner array size
                frameKeypoints.forEach { keypointValue ->    // Iterate through each value in the frame
                    flatInputArray[index++] = keypointValue
                }
            } else {
                Log.e(TAG, "Frame has incorrect number of keypoints. Expected $keypointsPerFrame, got ${frameKeypoints.size}")
                // Handle this error appropriately - perhaps return an error message
                // or skip this frame, though skipping might not be what you want.
                // For now, let's assume valid data to avoid partial filling if an error occurs mid-way.
                return "Error: Inconsistent keypoint data per frame."
            }
        }
        val shape = longArrayOf(1, numberOfFrames.toLong(), keypointsPerFrame.toLong())

// ✅ Sanity check: log shape and data type
        Log.d(TAG, "ExecuTorch input shape: [${shape.joinToString()}], " +
                "Frames=$numberOfFrames, Keypoints/Frame=$keypointsPerFrame")

// ✅ Log min/max/sample values for quick inspection
        val minVal = flatInputArray.minOrNull() ?: Float.NaN
        val maxVal = flatInputArray.maxOrNull() ?: Float.NaN
        val sampleValues = flatInputArray.take(10).joinToString()
        Log.d(TAG, "Input tensor min=$minVal, max=$maxVal, sample=[${sampleValues}]")

// ✅ Also confirm exact total length matches product of dimensions
        val expectedLength = shape.reduce { acc, l -> acc * l }
        if (flatInputArray.size != expectedLength.toInt()) {
            Log.e(TAG, "Length mismatch: expected $expectedLength elements, got ${flatInputArray.size}")
            return "Error: Input length mismatch."
        }

        val inputExecutorchTensor = org.pytorch.executorch.Tensor.fromBlob(flatInputArray, shape)

        // 1. Create the single EValue
        val singleInputEValue = org.pytorch.executorch.EValue.from(inputExecutorchTensor)

        Log.d(TAG, "Input EValue is tensor: ${singleInputEValue.isTensor}")
        Log.d(TAG, "Input Tensor shape: ${inputExecutorchTensor.shape().joinToString()}")
        Log.d(TAG, "Input Tensor dtype: ${inputExecutorchTensor.dtype()}") // Assuming dtype is available

        return try {
            Log.d(TAG, "Calling execuTorchModule.forward with a single EValue.")

            try {
                val shapeArr = inputExecutorchTensor.shape()   // returns LongArray
                val shapeStr = shapeArr.joinToString(prefix="[", postfix="]") { it.toString() }
                val dtype = inputExecutorchTensor.dtype()     // Java API: dtype()
                Log.d(TAG, "ExecuTorch -> About to call forward(). Input Tensor shape: $shapeStr, numel=${inputExecutorchTensor.numel()}, dtype=$dtype")
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to introspect input tensor metadata", e)
            }

            try {
                val start = System.currentTimeMillis()
                val outputEValue = execuTorchModule!!.forward(singleInputEValue)
                val took = System.currentTimeMillis() - start
                Log.d(TAG, "ExecuTorch forward() returned in ${took}ms. outputEValue = $outputEValue")
                // If you can, inspect outputEValue (toString) or cast/convert per API.
                // Example safe call:
                Log.d(TAG, "Output EValue.toString(): ${outputEValue?.toString()}")
            } catch (t: Throwable) {
                // Log the Java exception and the full stack trace
                Log.e(TAG, "ExecuTorch forward() failed with exception: ${t.message}", t)
                // Helpful hint printed so you can capture native logs
                Log.e(TAG, "Now capture adb logcat (see instructions in app log or run: adb logcat -v time | grep -i executorch )")
            }

            // Call forward with the single EValue directly
            // This assumes the forward method is defined like:
            // fun forward(input: EValue): EValue  OR
            // fun forward(input: EValue): Array<EValue>
            val outputReturn = execuTorchModule?.forward(singleInputEValue) // PASS SINGLE EVALUE

            // Now we need to determine if outputReturn is a single EValue or Array<EValue>
            // This depends on the *return type* of your specific forward method.

            // OPTION A: If forward returns a single EValue
            // val outputEValue: org.pytorch.executorch.EValue? = outputReturn
            // if (outputEValue == null) {
            //     Log.e(TAG, "Model forward pass returned null EValue.")
            //     return "Model output error: No output received."
            // }
            // if (outputEValue.isTensor) {
            //     // ... process single outputEValue ...
            // } else { //...}


            val outputEValueArray: Array<org.pytorch.executorch.EValue>? = outputReturn as? Array<org.pytorch.executorch.EValue>

            if (outputEValueArray == null || outputEValueArray.isEmpty()) {
                Log.e(TAG, "Model forward pass returned null, empty, or non-array EValue.")
                return "Model output error: No output received or unexpected return type."
            }

            val firstOutputEValue: org.pytorch.executorch.EValue = outputEValueArray[0]

            if (firstOutputEValue.isTensor) {
                val outputExecutorchTensor: org.pytorch.executorch.Tensor = firstOutputEValue.toTensor()
                Log.d(TAG, "Output Tensor shape: ${outputExecutorchTensor.shape().joinToString()}") // Should be [1, 118]
                Log.d(TAG, "Output Tensor dtype: ${outputExecutorchTensor.dtype()}")     // Should be Float32

                // The output tensor data is what we need. It's an array of floats (logits).
                // For a shape of (1, num_classes), dataAsFloatArray will give a flat array of num_classes elements.
                val outputLogits: FloatArray = outputExecutorchTensor.dataAsFloatArray

                if (outputLogits.isEmpty()) {
                    Log.e(TAG, "Output logits array is empty.")
                    return "Model output error: Empty logits."
                }
                Log.d(TAG, "Number of output logits: ${outputLogits.size}") // Should be 118

                // Load your labels (ensure this path and file name are correct)
                // You might have already loaded this earlier in the function, if so, reuse the 'labels' variable
                val labels = loadLabels(applicationContext, "gesture_names.txt") // Or your actual label file name

                if (labels.isEmpty()) {
                    Log.e(TAG, "Labels file is empty or could not be loaded.")
                    return "Error: Labels not loaded."
                }

                if (labels.size != outputLogits.size) {
                    Log.e(TAG, "Mismatch between number of labels (${labels.size}) and model output logits (${outputLogits.size}).")
                    return "Error: Label count mismatch with model output."
                }

                // --- Find the predicted class ---
                // We need to find the index of the highest score (logit) in the outputLogits array.

                var maxScore = -Float.MAX_VALUE
                var predictedIndex = -1

                for (i in outputLogits.indices) {
                    if (outputLogits[i] > maxScore) {
                        maxScore = outputLogits[i]
                        predictedIndex = i
                    }
                }

                if (predictedIndex == -1) {
                    Log.e(TAG, "Could not determine predicted class from logits.")
                    return "Error: Prediction processing failed."
                }

                // --- Map predicted index to label ---
                val predictedLabel = labels[predictedIndex]

//               Apply Softmax if you want probabilities (for display or confidence)
                 val probabilities = softmax(outputLogits)
                 val confidence = probabilities[predictedIndex]
                 Log.i(TAG, "Predicted Sign: $predictedLabel, Confidence: $confidence")
//                 return "Predicted Sign: $predictedLabel (Confidence: ${String.format("%.2f", confidence)})"

                return "Predicted Sign: $predictedLabel"

            } else {
                Log.e(TAG, "First model output EValue is not a tensor.")
                return "Model output error: Output is not a tensor."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error running ExecuTorch model inference", e) // Your current error is likely caught here
            "Error during prediction: ${e.message}"
        } finally {
            // inputExecutorchTensor.destroy() // Optional: Consider if needed for your Tensor implementation
        }
    }

    private fun loadLabels(context: Context, fileName: String): List<String> {
        val labels = mutableListOf<String>()
        try {
            // Get an InputStream from the assets folder
            context.assets.open(fileName).use { inputStream ->
                // Use a BufferedReader to read the lines efficiently
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        if (line.isNotBlank()) { // Add only non-blank lines
                            labels.add(line.trim())
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading labels from $fileName", e)
            // You might want to throw an exception here or return an empty list
            // and handle it gracefully where you call this function.
            // For now, it will return an empty list or whatever was loaded before the error.
        }
        return labels
    }

    // Helper function to get absolute path to the asset
    // This copies the asset to internal storage to get a usable file path
    // which is often required by native libraries.
    @Throws(IOException::class)
    fun getAbsolutePath(assetName: String): String {
        val file = File(this.filesDir, assetName)
        // If the file already exists and has content, use it to avoid re-copying
        // Be careful with this if your model file in assets might change during development.
        // For development, you might want to always re-copy or implement versioning.
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        // Copy file from assets to internal storage
        this.assets.open(assetName).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
        }
        return file.absolutePath
    }

    private fun setVideoViewDimensions() {
        // Get screen dimensions
        val screenWidth: Int
        val screenHeight: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                android.view.WindowInsets.Type.systemBars()
            )
            screenWidth = windowMetrics.bounds.width() - insets.left - insets.right
            screenHeight = windowMetrics.bounds.height() - insets.top - insets.bottom
        } else {
            val displayMetrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        }

        // Calculate desired dimensions
        val desiredWidth = (screenWidth * 0.80).toInt()
        val desiredHeight = desiredWidth * 9 / 16 // Aspect ratio 16:9

        // Apply to VideoView LayoutParams
        val layoutParams = videoView.layoutParams
        layoutParams.width = desiredWidth
        layoutParams.height = desiredHeight
        videoView.layoutParams = layoutParams

        // Log the dimensions for debugging (optional)
        // Toast.makeText(this, "VideoView dimensions: W=$desiredWidth, H=$desiredHeight", Toast.LENGTH_LONG).show()
    }

    private fun setupVideoView(videoUri: Uri) {
        videoView.setVideoURI(videoUri) // [2]

        // Add media controller for play, pause, seek, etc.
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController) // [1]

        videoView.setOnPreparedListener { mp ->
            // Video is prepared and ready to play
            mp.isLooping = true // Optional: if you want the video to loop
            videoView.start()
            Toast.makeText(this, "Playing video...", Toast.LENGTH_SHORT).show()
        }

        videoView.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "Error playing video", Toast.LENGTH_LONG).show()
            // Optionally, you can investigate 'what' and 'extra' for more details
            true // Returning true indicates that the error has been handled
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause video playback when the activity is paused
        if (videoView.isPlaying) {
            videoView.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        // Stop video playback and release resources
        videoView.stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        // It's good practice to release the model resources if applicable,
        // though ExecuTorch's Module might handle this on garbage collection.
        // Check documentation for specific cleanup requirements.
        // For older PyTorch Mobile, module.destroy() was common.
        // The current ExecuTorch API might differ.
        execuTorchModule?.destroy();
    }

    public fun onUploadButtonClick(view: View) {
        Toast.makeText(this, "Upload Button Clicked!", Toast.LENGTH_SHORT).show()
    }

    public fun onBackButtonClick3(view: View) {
        Toast.makeText(this, "Home Button Clicked!", Toast.LENGTH_SHORT).show()
        finish()
    }

    fun softmax(input: FloatArray): FloatArray {
        // For numerical stability, subtract the max value from each input before exponentiating.
        // This prevents overflow if inputs are very large, and underflow if very small,
        // without changing the actual softmax output distribution.
        val maxInput = input.maxOrNull() ?: 0.0f

        val exps = FloatArray(input.size)
        var sumExps = 0.0f

        for (i in input.indices) {
            exps[i] = kotlin.math.exp(input[i] - maxInput)
            sumExps += exps[i]
        }

        if (sumExps == 0.0f) { // Avoid division by zero if all exps are zero (e.g., after extreme underflow)
            // Handle this case: either return a uniform distribution or signal an error
            // For simplicity, returning a copy or zeros, but a uniform distribution is better.
            // return FloatArray(input.size) { 1.0f / input.size } // Uniform distribution
            return exps // Or let it divide by zero if you want to catch it as an error explicitly
        }

        for (i in exps.indices) {
            exps[i] /= sumExps
        }
        return exps
    }

}