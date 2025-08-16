package com.example.mysignmate

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

private const val LOG_TAG = "Display Video Activity"

class DisplayVideoActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private var videoUriString: String? = null
    companion object {
        const val DISPLAY_VIDEO_URI = "com.example.mysignmate.VIDEO_URI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_video)

        videoView = findViewById(R.id.video_display)
        setVideoViewDimensions()

        videoUriString = intent.getStringExtra(TranslateSignActivity.EXTRA_VIDEO_URI)

        if (videoUriString != null) {
            val videoUri = Uri.parse(videoUriString)
            Log.d(LOG_TAG, "Video selected, now playing: $videoUri")
            setupVideoView(videoUri)
        } else {
            Toast.makeText(this, "Error: Video URI not found", Toast.LENGTH_LONG).show()
            finish() // Close the activity if no URI is provided
        }
    }

    private fun setVideoViewDimensions() {
            // Get screen dimensions
            val screenWidth: Int

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowMetrics = windowManager.currentWindowMetrics
                val insets = windowMetrics.windowInsets.getInsetsIgnoringVisibility(
                    android.view.WindowInsets.Type.systemBars()
                )
                screenWidth = windowMetrics.bounds.width() - insets.left - insets.right
            } else {
                val displayMetrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getMetrics(displayMetrics)
                screenWidth = displayMetrics.widthPixels
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
            Log.d(LOG_TAG, "VideoView dimensions: W=$desiredWidth, H=$desiredHeight")
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
            Log.d(LOG_TAG, "Playing video...")
        }

        videoView.setOnErrorListener { mp, what, extra ->
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

    fun onContinueButtonClick(view: View) {
        Log.d(LOG_TAG, "Navigating to Results Activity.")
        val videoUri = Uri.parse(videoUriString)
        launchResultsActivity(videoUri)
    }

    fun onBackButtonClick2(view: View) {
        Log.d(LOG_TAG, "Navigating back to Translate Sign Activity.")
        finish()
    }

    private fun launchResultsActivity(uri: Uri) {
        val intent = Intent(this, ResultsActivity::class.java).apply {
            putExtra(DISPLAY_VIDEO_URI, uri.toString()) // Pass URI as a String
        }
        startResultsActivityLauncher.launch(intent)
    }

    private val startResultsActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == ResultsActivity.RESULT_NAVIGATE_HOME) {
                // ResultsActivity.kt signaled to go back further, so PreviousActivity also finishes
                finish()
            }
            // Handle other results if any
        }
}
    