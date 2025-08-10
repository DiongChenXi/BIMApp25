package com.example.mysignmate

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

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
            setupVideoView(videoUri)
        } else {
            Toast.makeText(this, "Error: Video URI not found", Toast.LENGTH_LONG).show()
            finish() // Close the activity if no URI is provided
        }
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

    public fun onContinueButtonClick(view: View) {
        Toast.makeText(this, "Continue Button Clicked!", Toast.LENGTH_SHORT).show()
        val videoUri = Uri.parse(videoUriString)
        launchResultsActivity(videoUri)
    }

    public fun onBackButtonClick2(view: View) {
        Toast.makeText(this, "Back Button Clicked!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun launchResultsActivity(uri: android.net.Uri) {
        val intent = Intent(this, ResultsActivity::class.java).apply {
            putExtra(DISPLAY_VIDEO_URI, uri.toString()) // Pass URI as a String
        }
        startActivity(intent)
    }
}
    