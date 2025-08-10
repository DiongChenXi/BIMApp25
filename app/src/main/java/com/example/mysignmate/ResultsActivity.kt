package com.example.mysignmate

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import org.pytorch.executorch.Module
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ResultsActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var resultsTextView: TextView
    private var execuTorchModule: Module? = null
    private val TAG = "ExecuTorchDemo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        videoView = findViewById(R.id.replay_video_display)
        setVideoViewDimensions()

        val videoUriString = intent.getStringExtra(DisplayVideoActivity.DISPLAY_VIDEO_URI)

        if (videoUriString != null) {
            val videoUri = Uri.parse(videoUriString)
            setupVideoView(videoUri)
        } else {
            Toast.makeText(this, "Error: Video URI not found", Toast.LENGTH_LONG).show()
            finish() // Close the activity if no URI is provided
        }

        try {
            val modelPath = getAbsolutePath("model.pte") // Make sure filename matches
            execuTorchModule = Module.load(modelPath)
            Log.d(TAG, "ExecuTorch model loaded successfully!")
        } catch (e: IOException) {
            Log.e(TAG, "Error loading ExecuTorch model", e)
            // Handle the error appropriately (e.g., show a message to the user)
            Toast.makeText(this, "Error loading ExecuTorch model", Toast.LENGTH_LONG).show()
        }

        resultsTextView = findViewById(R.id.results_text)
        resultsTextView.text = "Process complete!\nThe predicted word is: "
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
}