package com.example.mysignmate

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.impl.Config

class TranslateSignActivity : AppCompatActivity() {
    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var requestStoragePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var recordVideoLauncher: ActivityResultLauncher<Intent>
    private lateinit var uploadVideoLauncher: ActivityResultLauncher<Intent>

    private var videoUri: android.net.Uri? = null // To store the URI of the recorded/selected video
    companion object {
        const val EXTRA_VIDEO_URI = "com.example.mysignmate.VIDEO_URI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translate_sign)

        requestCameraPermission()
        requestStoragePermission()
        initializeRecordVideoLauncher()
        initializeUploadVideoLauncher()
    }

    public fun onRecordButtonClick(view: View) {
        Toast.makeText(this, "Record Video Button Clicked!", Toast.LENGTH_SHORT).show()
        checkCameraPermissionAndOpen()
    }

    public fun onUploadButtonClick(view: View) {
        Toast.makeText(this, "Upload Video Button Clicked!", Toast.LENGTH_SHORT).show()
        checkStoragePermissionAndOpenGallery()
    }

    private fun requestCameraPermission() {
        requestCameraPermissionLauncher =
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                    openCameraToRecordVideo()
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun requestStoragePermission() {
        requestStoragePermissionLauncher =
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
                    openGalleryToUploadVideo()
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun initializeRecordVideoLauncher() {
        recordVideoLauncher =
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        videoUri = uri
                        Toast.makeText(this, "Video recorded: $videoUri", Toast.LENGTH_LONG).show()
                        // Proceed to display the video
                        launchDisplayVideoActivity(uri)
                    } ?: run {
                        Toast.makeText(this, "Failed to get video URI", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Video recording cancelled or failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun initializeUploadVideoLauncher() {

        uploadVideoLauncher =
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.data?.let { uri ->
                        videoUri = uri
                        Toast.makeText(this, "Video selected: $videoUri", Toast.LENGTH_LONG).show()
                        // Proceed to display the video
                        launchDisplayVideoActivity(uri)
                    } ?: run {
                        Toast.makeText(this, "Failed to get video URI", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Video selection cancelled or failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                openCameraToRecordVideo()
            }
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            ) -> {
                Toast.makeText(this, "Camera permission is needed to record video.", Toast.LENGTH_LONG).show()
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCameraToRecordVideo() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                recordVideoLauncher.launch(takeVideoIntent)
            } ?: run {
                Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkStoragePermissionAndOpenGallery() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                permission
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                openGalleryToUploadVideo()
            }
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                permission
            ) -> {
                Toast.makeText(this, "Storage permission is needed to upload video.", Toast.LENGTH_LONG).show()
                requestStoragePermissionLauncher.launch(permission)
            }
            else -> {
                requestStoragePermissionLauncher.launch(permission)
            }
        }
    }

    private fun openGalleryToUploadVideo() {
//        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)

        // Option 2: Using ACTION_GET_CONTENT with a MIME type
         val intent = Intent(Intent.ACTION_GET_CONTENT)
         intent.type = "video/*"
        uploadVideoLauncher.launch(intent)
    }

    private fun launchDisplayVideoActivity(uri: android.net.Uri) {
        val intent = Intent(this, DisplayVideoActivity::class.java).apply {
            putExtra(EXTRA_VIDEO_URI, uri.toString()) // Pass URI as a String
        }
        startActivity(intent)
    }
}