package com.example.mysignmate.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.IOException
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

// --- Constants for landmark indices ---
// Pose (based on MediaPipe Pose Landmarker)
const val POSE_NOSE = 0
const val POSE_LEFT_SHOULDER = 11
const val POSE_RIGHT_SHOULDER = 12
const val POSE_LEFT_WRIST = 15
const val POSE_RIGHT_WRIST = 16

// Hand (landmark 0 is the wrist for MediaPipe Hand Landmarker)
const val HAND_WRIST = 0

class KeypointProcessorUtil(
    private val context: Context,
) {

    private val TAG = "ExecuTorchDemo"
    private var videoProcessor: VideoProcessorUtil = VideoProcessorUtil(context)

    suspend fun processVideo(videoUri: Uri): MutableList<FloatArray>? {
        Log.d(TAG, "Starting video processing for URI: $videoUri")

        // Declare the lists to store results for all processed frames
        val allFrameHandLandmarks = kotlin.collections.mutableListOf<HandLandmarkerResult?>()
        val allFramePoseLandmarks = kotlin.collections.mutableListOf<PoseLandmarkerResult?>()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)

            val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val videoDurationMs = durationString?.toLong() ?: 0L

            if (videoDurationMs > 0) {
                // Determine how many frames you want or at what interval
                // Example: Process 10 frames per second (adjust frameIntervalMs)
                val desiredFps = 10
                val frameIntervalMs: Int = 1000 / desiredFps
                var currentTimeMs: Long = 0

                Log.d(TAG, "Video duration: $videoDurationMs ms. Processing at ~${desiredFps} FPS.")

                while (currentTimeMs < videoDurationMs) {
                    // getFrameAtTime expects microseconds.
                    // OPTION_CLOSEST_SYNC is usually faster but might skip to the nearest sync frame.
                    // OPTION_CLOSEST will be more precise but potentially slower.
                    val bitmapFrame = retriever.getFrameAtTime(
                        currentTimeMs * 1000, // Time in microseconds
                        MediaMetadataRetriever.OPTION_CLOSEST // Or OPTION_CLOSEST_SYNC
                    )

                    if (bitmapFrame != null) {
                        Log.d(TAG, "Processing frame at $currentTimeMs ms")
                        // Detect Hand Landmarks
                        val handResult = videoProcessor.detectHandLandmarks(bitmapFrame)
                        allFrameHandLandmarks.add(handResult)

                        // Detect Pose Landmarks
                        val poseResult = videoProcessor.detectPoseLandmarks(bitmapFrame)
                        allFramePoseLandmarks.add(poseResult)

                        // Optional: If you want to provide immediate feedback or draw on frames as they process
                        // you might need to switch to the Main thread here for UI updates with the 'bitmapFrame'
                        // and 'landmarksResult'.
                        // withContext(Dispatchers.Main) {
                        //     updateUiWithFrameAndLandmarks(bitmapFrame, landmarksResult)
                        // }

                        // Important: Recycle the bitmap if you are done with it to save memory,
                        // especially in a loop. Be careful if the listener or other parts
                        // of your code need it for longer. If you're displaying it immediately
                        // and then moving to the next, recycling here is good.
                        // bitmapFrame.recycle() // Comment out if listener or UI needs it longer.
                    } else {
                        Log.w(TAG, "Could not retrieve frame at $currentTimeMs ms for URI: $videoUri")
                    }
                    currentTimeMs += frameIntervalMs

                    // If you only want to process a certain number of frames, add a counter and break.
                }
                Log.d(TAG, "Finished processing hand landmarks for all frames. Total frames processed (attempted): ${allFrameHandLandmarks.size}")
                Log.d(TAG, "Finished processing pose landmarks for all frames. Total frames processed (attempted): ${allFramePoseLandmarks.size}")
                // Now you have 'allFrameLandmarks' - a list of landmark results for each processed frame.
                // You can pass this list to another function for further analysis or to your ML model.
                return handleProcessedLandmarks(allFrameHandLandmarks, allFramePoseLandmarks)

            } else {
                Log.e(TAG, "Video duration is 0 or could not be determined for URI: $videoUri")
//                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
//                        processingListener?.onError("Could not determine video duration for URI: $uri")
//                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during video processing for URI: $videoUri", e)
//                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
//                    // Update UI with error
//                    processingListener?.onError("Error processing video: ${e.message}")
//                }
        } finally {
            try {
                retriever.release() // Always release the retriever
            } catch (e: IOException) {
                Log.e(TAG, "Error releasing MediaMetadataRetriever", e)
            }
        }
        return null
    }

    private suspend fun handleProcessedLandmarks(
        handLandmarkResults: List<HandLandmarkerResult?>,
        poseLandmarkResults: List<PoseLandmarkerResult?>
    ): MutableList<FloatArray>? {
        Log.d(TAG, "All landmarks collected. Hand results: ${handLandmarkResults.size}, Pose results: ${poseLandmarkResults.size}")

        // 1. Combine or process these two lists of landmark data.
        // Ensure both lists have the same number of frames, or decide how to handle discrepancies
        // For simplicity, this example assumes they correspond 1:1.
        // You might need more robust handling if frame counts can differ.
        val numFrames =
            minOf(handLandmarkResults.size, poseLandmarkResults.size)
        if (handLandmarkResults.size != poseLandmarkResults.size) {
            Log.e(TAG, "Warning: Hand and Pose landmark result counts differ. Processing up to the minimum count: $numFrames")
        }

        val allFramesKeypoints = mutableListOf<FloatArray>() // List to hold the combined keypoints for each frame

        for (i in 0 until numFrames) {
            val handResult = handLandmarkResults[i]
            val poseResult = poseLandmarkResults[i]

            val frameKeypoints = extractKeypointsForFrame(poseResult, handResult)
            allFramesKeypoints.add(frameKeypoints)
        }

        val targetFrameCount = 512
        var processedKeypoints: List<FloatArray> = allFramesKeypoints

        if (allFramesKeypoints.isNotEmpty()) {
            // 2. Frame Sampling to 512 frames for consistency
            if (processedKeypoints.size != targetFrameCount) {
                Log.i(TAG, "Adjusting frame count from ${allFramesKeypoints.size} to $targetFrameCount.")
                processedKeypoints = adjustFramesToCount(allFramesKeypoints, targetFrameCount)
                Log.i(TAG, "Frame count after adjustment: ${allFramesKeypoints.size}.")
            }

            val mutableProcessedKeypoints = processedKeypoints.toMutableList()

            // 3. Initialize first and last keypoints.
            Log.d(TAG, "Initializing hand keypoints for first and last frames...")
            initializeHandKeypoints(mutableProcessedKeypoints) // Modify in place
            Log.d(TAG, "Hand keypoint initialization complete.")

            // 4. Reconstruct missing keypoints with bilinear interpolation.
            Log.d(TAG, "Starting bilinear interpolation for missing hand keypoints...")
            bilinearInterpolationKeypoints(mutableProcessedKeypoints) // Modifies in place
            Log.d(TAG, "Bilinear interpolation complete.")

            // 5. Normalize keypoints.
            Log.d(TAG, "Normalizing all frame keypoints...")
            val finalKeypoints = normalizeAllFramesKeypoints(mutableProcessedKeypoints) // Assign the new normalized list
            Log.d(TAG, "Keypoint normalization complete.")

            return finalKeypoints

        } else {
            Log.w(TAG, "No frames keypoints found.")
        }

        return null
    }

    // Equivalent to your Python extract_keypoints function for a single frame's results
    private fun extractKeypointsForFrame(
        poseResult: PoseLandmarkerResult?,
        handResult: HandLandmarkerResult?
    ): FloatArray {
        val poseLandmarks = mutableListOf<Float>()
        if (poseResult != null && poseResult.landmarks().isNotEmpty()) {
            // Assuming one person detected for pose
            for (landmark in poseResult.landmarks()[0]) {
                poseLandmarks.add(landmark.x())
                poseLandmarks.add(landmark.y())
                poseLandmarks.add(landmark.z())
                poseLandmarks.add(landmark.visibility().orElse(0.0f)) // Use orElse for Optional<Float>
            }
        } else {
            // 33 landmarks * 4 values (x, y, z, visibility)
            poseLandmarks.addAll(List(33 * 4) { 0.0f })
        }

        val leftHandLandmarks = mutableListOf<Float>()
        val rightHandLandmarks = mutableListOf<Float>()

        if (handResult != null && handResult.landmarks().isNotEmpty()) {
            // MediaPipe HandLandmarkerResult can contain multiple hands.
            // We need to check handedness.
            // results.handedness() returns a list of Category objects for each detected hand.
            // results.landmarks() returns a list of NormalizedLandmark lists, corresponding to handedness.

            for (handIndex in handResult.handedness().indices) {
                val handednessCategoryList = handResult.handedness()[handIndex] // List<Category> for this hand
                val handLandmarkList = handResult.landmarks()[handIndex]        // List<NormalizedLandmark>

                if (handednessCategoryList.isNotEmpty()) {
                    val handedness = handednessCategoryList[0].categoryName() // "Left" or "Right"

                    if (handedness == "Left") {
                        for (landmark in handLandmarkList) {
                            leftHandLandmarks.add(landmark.x())
                            leftHandLandmarks.add(landmark.y())
                            leftHandLandmarks.add(landmark.z())
                        }
                    } else if (handedness == "Right") {
                        for (landmark in handLandmarkList) {
                            rightHandLandmarks.add(landmark.x())
                            rightHandLandmarks.add(landmark.y())
                            rightHandLandmarks.add(landmark.z())
                        }
                    }
                }
            }
        }

        // Fill with zeros if landmarks were not detected or the specific hand was missing
        if (leftHandLandmarks.isEmpty()) {
            // 21 landmarks * 3 values (x, y, z)
            leftHandLandmarks.addAll(List(21 * 3) { 0.0f })
        }
        if (rightHandLandmarks.isEmpty()) {
            // 21 landmarks * 3 values (x, y, z)
            rightHandLandmarks.addAll(List(21 * 3) { 0.0f })
        }

        // Ensure they have the correct size before concatenation if a hand was detected
        // but somehow had an unexpected number of landmarks (shouldn't happen with MediaPipe defaults)
        while (poseLandmarks.size < 33 * 4) poseLandmarks.add(0.0f)
        while (leftHandLandmarks.size < 21 * 3) leftHandLandmarks.add(0.0f)
        while (rightHandLandmarks.size < 21 * 3) rightHandLandmarks.add(0.0f)


        // Concatenate using toFloatArray() for efficiency if these are already Float lists
        return (poseLandmarks.take(33 * 4) +
                leftHandLandmarks.take(21 * 3) +
                rightHandLandmarks.take(21 * 3))
            .toFloatArray()
    }

    // Helper extension function to check if a FloatArray slice is all zeros
    private fun FloatArray.isSliceAllZeros(startIndex: Int, endIndex: Int): Boolean {
        if (startIndex < 0 || endIndex > this.size || startIndex >= endIndex) {
            // Handle invalid range if necessary, or assume valid input
            return true // Or throw IllegalArgumentException
        }
        for (i in startIndex until endIndex) {
            if (this[i] != 0.0f) {
                return false
            }
        }
        return true
    }

    // Function to calculate element-wise mean of a list of FloatArrays
    private fun calculateMeanFloatArray(arrays: List<FloatArray>): FloatArray {
        if (arrays.isEmpty()) {
            return FloatArray(0) // Or handle as an error, or return expected zero array
        }
        val arraySize = arrays[0].size
        if (arraySize == 0) {
            return FloatArray(0)
        }
        val sums = FloatArray(arraySize)
        for (arr in arrays) {
            if (arr.size == arraySize) { // Ensure consistent sizes
                for (i in 0 until arraySize) {
                    sums[i] += arr[i]
                }
            }
        }
        val means = FloatArray(arraySize)
        for (i in 0 until arraySize) {
            means[i] = sums[i] / arrays.size
        }
        return means
    }

    private fun initializeHandKeypoints(framesKeypoints: MutableList<FloatArray>): MutableList<FloatArray> {
        if (framesKeypoints.isEmpty()) {
            return framesKeypoints // Nothing to do
        }

        val poseKeypointsCount = 33 * 4
        val leftHandKeypointsCount = 21 * 3
        val rightHandKeypointsCount = 21 * 3

        val leftHandStartIndex = poseKeypointsCount
        val leftHandEndIndex = leftHandStartIndex + leftHandKeypointsCount
        val rightHandStartIndex = leftHandEndIndex
        val rightHandEndIndex = rightHandStartIndex + rightHandKeypointsCount

        // Extract valid hand keypoints for averaging
        // A hand is considered "valid" if its keypoint data is not all zeros
        // (assuming your extractKeypointsForFrame already padded with zeros for missing hands)

        val validLeftHandKeypoints = framesKeypoints.mapNotNull { frameKp ->
            // Check if the frame has enough keypoints and the left hand slice is not all zeros
            if (frameKp.size >= leftHandEndIndex && !frameKp.isSliceAllZeros(leftHandStartIndex, leftHandEndIndex)) {
                frameKp.copyOfRange(leftHandStartIndex, leftHandEndIndex)
            } else {
                null
            }
        }

        val validRightHandKeypoints = framesKeypoints.mapNotNull { frameKp ->
            // Check if the frame has enough keypoints and the right hand slice is not all zeros
            if (frameKp.size >= rightHandEndIndex && !frameKp.isSliceAllZeros(rightHandStartIndex, rightHandEndIndex)) {
                frameKp.copyOfRange(rightHandStartIndex, rightHandEndIndex)
            } else {
                null
            }
        }

        Log.d("InitializeHandKeypoints", "Valid left hand sequences found: ${validLeftHandKeypoints.size}")
        Log.d("InitializeHandKeypoints", "Valid right hand sequences found: ${validRightHandKeypoints.size}")

        // Calculate average hand keypoints
        val avgLeftHand = if (validLeftHandKeypoints.isNotEmpty()) {
            calculateMeanFloatArray(validLeftHandKeypoints)
        } else {
            FloatArray(leftHandKeypointsCount) { 0.0f }
        }

        val avgRightHand = if (validRightHandKeypoints.isNotEmpty()) {
            calculateMeanFloatArray(validRightHandKeypoints)
        } else {
            FloatArray(rightHandKeypointsCount) { 0.0f }
        }

        // --- Initialize the first frame if its hand keypoints are missing (all zeros) ---
        val firstFrame = framesKeypoints.first() // Assuming framesKeypoints is not empty
        if (firstFrame.size >= leftHandEndIndex && firstFrame.isSliceAllZeros(leftHandStartIndex, leftHandEndIndex)) {
            Log.d("InitializeHandKeypoints", "Initializing missing left hand for first frame.")
            avgLeftHand.copyInto(destination = firstFrame, destinationOffset = leftHandStartIndex)
        }
        if (firstFrame.size >= rightHandEndIndex && firstFrame.isSliceAllZeros(rightHandStartIndex, rightHandEndIndex)) {
            Log.d("InitializeHandKeypoints", "Initializing missing right hand for first frame.")
            avgRightHand.copyInto(destination = firstFrame, destinationOffset = rightHandStartIndex)
        }

        // --- Initialize the last frame if its hand keypoints are missing (all zeros) ---
        if (framesKeypoints.size > 1) { // Only if there's more than one frame
            val lastFrame = framesKeypoints.last()
            if (lastFrame.size >= leftHandEndIndex && lastFrame.isSliceAllZeros(leftHandStartIndex, leftHandEndIndex)) {
                Log.d("InitializeHandKeypoints", "Initializing missing left hand for last frame.")
                avgLeftHand.copyInto(destination = lastFrame, destinationOffset = leftHandStartIndex)
            }
            if (lastFrame.size >= rightHandEndIndex && lastFrame.isSliceAllZeros(rightHandStartIndex, rightHandEndIndex)) {
                Log.d("InitializeHandKeypoints", "Initializing missing right hand for last frame.")
                avgRightHand.copyInto(destination = lastFrame, destinationOffset = rightHandStartIndex)
            }
        } else { // If only one frame, it was already handled by the "firstFrame" logic
            Log.d("InitializeHandKeypoints", "Only one frame, no separate last frame initialization needed.")
        }


        return framesKeypoints
    }

    /**
     * Finds the nearest valid preceding (alpha) and succeeding (beta) frames with keypoints
     * for a specific hand and performs linear interpolation if both are found.
     *
     * @param framesKeypoints The list of all frame keypoints.
     * @param currentFrameIndex The index (k) of the frame whose hand keypoints are being interpolated.
     * @param frameToModify The FloatArray of the current frame (k) that will be modified in place.
     * @param handStartIndex The starting index of the hand's keypoints in the FloatArray.
     * @param handEndIndex The ending index (exclusive) of the hand's keypoints.
     * @return True if interpolation was performed, false otherwise.
     */
    private fun findAndInterpolateNeighbourKeypoints(
        framesKeypoints: List<FloatArray>,
        currentFrameIndex: Int,
        frameToModify: FloatArray, // This is framesKeypoints[currentFrameIndex]
        handStartIndex: Int,
        handEndIndex: Int
    ): Boolean {
        var alphaDistance: Int? = null
        var betaDistance: Int? = null

        // Search for α (distance to previous frame with keypoints)
        // 'a' will be the distance (1, 2, ...)
        for (a in 1..currentFrameIndex) { // Check frames k-1, k-2, ..., 0
            val prevFrameIndex = currentFrameIndex - a
            if (prevFrameIndex < 0) break // Should be covered by loop range, but good practice

            val prevFrameKeypoints = framesKeypoints[prevFrameIndex]
            if (prevFrameKeypoints.size >= handEndIndex && !prevFrameKeypoints.isSliceAllZeros(handStartIndex, handEndIndex)) {
                alphaDistance = a
                break
            }
        }

        // Search for β (distance to next frame with keypoints)
        // 'b' will be the distance (1, 2, ...)
        for (b in 1 until (framesKeypoints.size - currentFrameIndex)) { // Check frames k+1, k+2, ..., end
            val nextFrameIndex = currentFrameIndex + b
            if (nextFrameIndex >= framesKeypoints.size) break // Should be covered by loop range

            val nextFrameKeypoints = framesKeypoints[nextFrameIndex]
            if (nextFrameKeypoints.size >= handEndIndex && !nextFrameKeypoints.isSliceAllZeros(handStartIndex, handEndIndex)) {
                betaDistance = b
                break
            }
        }

        // If both α and β are found, interpolate
        if (alphaDistance != null && betaDistance != null) {
            val frameAlpha = framesKeypoints[currentFrameIndex - alphaDistance]
            val frameBeta = framesKeypoints[currentFrameIndex + betaDistance]

            // Ensure slices are valid before access (should be if isSliceAllZeros passed)
            if (frameAlpha.size < handEndIndex || frameBeta.size < handEndIndex || frameToModify.size < handEndIndex) {
                Log.e("Interpolate", "Frame array too short for hand slice during interpolation.")
                return false
            }

            for (i in 0 until (handEndIndex - handStartIndex)) {
                val keypointIndexInSlice = i
                val actualIndexInFrame = handStartIndex + keypointIndexInSlice

                val valAlpha = frameAlpha[actualIndexInFrame]
                val valBeta = frameBeta[actualIndexInFrame]

                // Interpolation: (β * val_α + α * val_β) / (α + β)
                frameToModify[actualIndexInFrame] = (betaDistance * valAlpha + alphaDistance * valBeta) / (alphaDistance + betaDistance).toFloat()
            }
            return true // Interpolation was performed
        }
        return false // Interpolation was not performed
    }

    /**
     * Applies linear interpolation to fill missing hand keypoints in a sequence of frames.
     * Modifies the framesKeypoints list in place.
     *
     * @param framesKeypoints MutableList of FloatArray, where each FloatArray represents a frame's keypoints.
     */
    private fun bilinearInterpolationKeypoints(framesKeypoints: MutableList<FloatArray>) {
        if (framesKeypoints.isEmpty()) {
            Log.w("BilinearInterpolation", "No frames to interpolate.")
            return
        }

        val poseKeypointsCount = 33 * 4
        val leftHandKeypointsCount = 21 * 3
        val rightHandKeypointsCount = 21 * 3 // Not strictly needed here but good for consistency

        val leftHandStartIndex = poseKeypointsCount
        val leftHandEndIndex = leftHandStartIndex + leftHandKeypointsCount
        val rightHandStartIndex = leftHandEndIndex
        val rightHandEndIndex = rightHandStartIndex + rightHandKeypointsCount

        var interpolatedLeftCount = 0
        var interpolatedRightCount = 0

        Log.d("BilinearInterpolation", "Starting bilinear interpolation for ${framesKeypoints.size} frames.")

        for (k in framesKeypoints.indices) { // Iterate from 0 to last frame index
            val currentFrameArray = framesKeypoints[k]

            // Ensure current frame array is large enough
            if (currentFrameArray.size < rightHandEndIndex) { // Check against the largest possible end index
                Log.w("BilinearInterpolation", "Frame $k is too short to contain hand keypoints. Skipping.")
                continue
            }

            // Check and interpolate missing left hand keypoints
            if (currentFrameArray.isSliceAllZeros(leftHandStartIndex, leftHandEndIndex)) {
                Log.d("BilinearInterpolation", "Frame $k: Left hand missing. Attempting interpolation.")
                if (findAndInterpolateNeighbourKeypoints(
                        framesKeypoints,
                        k,
                        currentFrameArray,
                        leftHandStartIndex,
                        leftHandEndIndex
                    )
                ) {
                    interpolatedLeftCount++
                    Log.d("BilinearInterpolation", "Frame $k: Left hand interpolated.")
                } else {
                    Log.d("BilinearInterpolation", "Frame $k: Left hand could not be interpolated (no neighbours?).")
                }
            }

            // Check and interpolate missing right hand keypoints
            if (currentFrameArray.isSliceAllZeros(rightHandStartIndex, rightHandEndIndex)) {
                Log.d("BilinearInterpolation", "Frame $k: Right hand missing. Attempting interpolation.")
                if (findAndInterpolateNeighbourKeypoints(
                        framesKeypoints,
                        k,
                        currentFrameArray,
                        rightHandStartIndex,
                        rightHandEndIndex
                    )
                ) {
                    interpolatedRightCount++
                    Log.d("BilinearInterpolation", "Frame $k: Right hand interpolated.")
                } else {
                    Log.d("BilinearInterpolation", "Frame $k: Right hand could not be interpolated (no neighbours?).")
                }
            }
        }

        Log.i("BilinearInterpolation", "Interpolation complete. Interpolated $interpolatedLeftCount left hand(s) and $interpolatedRightCount right hand(s).")
    }

    /**
     * Normalizes keypoints for a sequence of frames.
     *
     * This normalization makes the keypoints relative to the body pose (specifically neck to nose distance)
     * and aligns hand keypoints with their respective pose wrists.
     *
     * @param framesKeypoints MutableList of FloatArray, where each FloatArray is the combined
     *                        (pose + left_hand + right_hand) keypoints for a frame.
     * @return A new MutableList of FloatArray containing the normalized keypoints for each frame.
     */
    private fun normalizeAllFramesKeypoints(framesKeypoints: List<FloatArray>): MutableList<FloatArray> {
        if (framesKeypoints.isEmpty()) {
            return mutableListOf()
        }

        val normalizedFrames = mutableListOf<FloatArray>()

        val poseKeypointsCountFlat = 33 * 4
        val handKeypointsCountFlat = 21 * 3

        val leftHandStartIndexFlat = poseKeypointsCountFlat
        val rightHandStartIndexFlat = poseKeypointsCountFlat + handKeypointsCountFlat

        Log.d("NormalizeKeypoints", "Starting normalization for ${framesKeypoints.size} frames.")

        for ((frameIndex, currentFrameKeypoints) in framesKeypoints.withIndex()) {
            if (currentFrameKeypoints.size < poseKeypointsCountFlat + 2 * handKeypointsCountFlat) {
                Log.w("NormalizeKeypoints", "Frame $frameIndex is too short. Skipping normalization for this frame.")
                normalizedFrames.add(currentFrameKeypoints.copyOf()) // Add a copy of original if too short
                continue
            }

            val normalizedKeypointsForFrame = currentFrameKeypoints.copyOf() // Work on a copy

            // --- 1. Extract and Reshape (Conceptually) ---
            // We'll access them from the flat array directly using offsets and strides

            // --- 2. Calculate Normalization Reference Points (Pose) ---
            // (x, y) of left shoulder
            val lShoulderX = currentFrameKeypoints[POSE_LEFT_SHOULDER * 4 + 0]
            val lShoulderY = currentFrameKeypoints[POSE_LEFT_SHOULDER * 4 + 1]

            // (x, y) of right shoulder
            val rShoulderX = currentFrameKeypoints[POSE_RIGHT_SHOULDER * 4 + 0]
            val rShoulderY = currentFrameKeypoints[POSE_RIGHT_SHOULDER * 4 + 1]

            // Midpoint of shoulders for neck (x, y)
            val neckX = (lShoulderX + rShoulderX) / 2.0f
            val neckY = (lShoulderY + rShoulderY) / 2.0f

            // (x, y) of nose (head)
            val headX = currentFrameKeypoints[POSE_NOSE * 4 + 0]
            val headY = currentFrameKeypoints[POSE_NOSE * 4 + 1]

            // Normalization factor: distance between head and neck
            val dxHeadNeck = headX - neckX
            val dyHeadNeck = headY - neckY
            var normFactor = sqrt(dxHeadNeck.pow(2) + dyHeadNeck.pow(2))

            if (normFactor == 0.0f) {
                Log.w("NormalizeKeypoints", "Frame $frameIndex: Norm factor is 0 (head and neck are at the same 2D point). Setting to 1 to avoid division by zero.")
                normFactor = 1.0f
            }

            // --- 3. Normalize Pose Keypoints (x, y only) ---
            for (k in 0 until 33) {
                val landmarkBaseIndex = k * 4
                val originalX = currentFrameKeypoints[landmarkBaseIndex + 0]
                val originalY = currentFrameKeypoints[landmarkBaseIndex + 1]
                // Z (landmarkBaseIndex + 2) and Visibility (landmarkBaseIndex + 3) remain unchanged

                normalizedKeypointsForFrame[landmarkBaseIndex + 0] = (originalX - neckX) / normFactor
                normalizedKeypointsForFrame[landmarkBaseIndex + 1] = (originalY - neckY) / normFactor
            }

            // --- 4. Normalize Hand Keypoints (x, y only) ---

            // Get original (unnormalized) pose wrist coordinates (needed for hand alignment)
            val poseLWristX = currentFrameKeypoints[POSE_LEFT_WRIST * 4 + 0]
            val poseLWristY = currentFrameKeypoints[POSE_LEFT_WRIST * 4 + 1]
            val poseRWristX = currentFrameKeypoints[POSE_RIGHT_WRIST * 4 + 0]
            val poseRWristY = currentFrameKeypoints[POSE_RIGHT_WRIST * 4 + 1]

            // --- Normalize Left Hand ---
            val leftHandSlice = currentFrameKeypoints.copyOfRange(leftHandStartIndexFlat, leftHandStartIndexFlat + handKeypointsCountFlat)
            if (leftHandSlice.isSliceAllZeros(0, handKeypointsCountFlat)) {
                // Hand is missing, keep it as zeros in normalizedKeypointsForFrame (already copied)
                Log.d("NormalizeKeypoints", "Frame $frameIndex: Left hand is all zeros, skipping normalization.")
            } else {
                val handOwnWristX = leftHandSlice[HAND_WRIST * 3 + 0] // x of landmark 0 of this hand
                val handOwnWristY = leftHandSlice[HAND_WRIST * 3 + 1] // y of landmark 0 of this hand

                for (k in 0 until 21) {
                    val handLandmarkBaseIndexInSlice = k * 3
                    val originalHandX = leftHandSlice[handLandmarkBaseIndexInSlice + 0]
                    val originalHandY = leftHandSlice[handLandmarkBaseIndexInSlice + 1]
                    // Z (handLandmarkBaseIndexInSlice + 2) remains unchanged for this normalization type

                    // Part 1: (hand_coord - hand_wrist_coord) / norm_factor
                    val relativeToHandWristX = (originalHandX - handOwnWristX) / normFactor
                    val relativeToHandWristY = (originalHandY - handOwnWristY) / normFactor

                    // Part 2: (pose_wrist_coord - neck_coord) / norm_factor
                    val normalizedPoseWristX = (poseLWristX - neckX) / normFactor
                    val normalizedPoseWristY = (poseLWristY - neckY) / normFactor

                    // Combine
                    normalizedKeypointsForFrame[leftHandStartIndexFlat + handLandmarkBaseIndexInSlice + 0] = relativeToHandWristX + normalizedPoseWristX
                    normalizedKeypointsForFrame[leftHandStartIndexFlat + handLandmarkBaseIndexInSlice + 1] = relativeToHandWristY + normalizedPoseWristY
                }
            }

            // --- Normalize Right Hand ---
            val rightHandSlice = currentFrameKeypoints.copyOfRange(rightHandStartIndexFlat, rightHandStartIndexFlat + handKeypointsCountFlat)
            if (rightHandSlice.isSliceAllZeros(0, handKeypointsCountFlat)) {
                // Hand is missing
                Log.d("NormalizeKeypoints", "Frame $frameIndex: Right hand is all zeros, skipping normalization.")
            } else {
                val handOwnWristX = rightHandSlice[HAND_WRIST * 3 + 0]
                val handOwnWristY = rightHandSlice[HAND_WRIST * 3 + 1]

                for (k in 0 until 21) {
                    val handLandmarkBaseIndexInSlice = k * 3
                    val originalHandX = rightHandSlice[handLandmarkBaseIndexInSlice + 0]
                    val originalHandY = rightHandSlice[handLandmarkBaseIndexInSlice + 1]

                    val relativeToHandWristX = (originalHandX - handOwnWristX) / normFactor
                    val relativeToHandWristY = (originalHandY - handOwnWristY) / normFactor

                    val normalizedPoseWristX = (poseRWristX - neckX) / normFactor
                    val normalizedPoseWristY = (poseRWristY - neckY) / normFactor

                    normalizedKeypointsForFrame[rightHandStartIndexFlat + handLandmarkBaseIndexInSlice + 0] = relativeToHandWristX + normalizedPoseWristX
                    normalizedKeypointsForFrame[rightHandStartIndexFlat + handLandmarkBaseIndexInSlice + 1] = relativeToHandWristY + normalizedPoseWristY
                }
            }
            normalizedFrames.add(normalizedKeypointsForFrame)
        }
        Log.i("NormalizeKeypoints", "Normalization complete for ${normalizedFrames.size} frames.")
        return normalizedFrames
    }

    /**
     * Adjusts a list of items (frames) to a specific target size using uniform sampling.
     *
     * @param T The type of items in the list.
     * @param originalFrames The original list of frames.
     * @param targetFrameCount The desired number of frames (e.g., 512).
     * @return A new list containing the adjusted number of frames.
     */
    private fun <T> adjustFramesToCount(originalFrames: List<T>, targetFrameCount: Int): List<T> {
        val currentFrameCount = originalFrames.size
        Log.d("FrameAdjustment", "Original frame count: $currentFrameCount, Target: $targetFrameCount")

        if (currentFrameCount == 0) {
            Log.w("FrameAdjustment", "Original frame list is empty. Returning empty list.")
            return emptyList()
        }

        if (currentFrameCount == targetFrameCount) {
            Log.d("FrameAdjustment", "Frame count already matches target. No adjustment needed.")
            return originalFrames // No adjustment needed, return the original list (or a copy if preferred)
        }

        val adjustedFrames = mutableListOf<T>()
        val indices = generateUniformIndices(currentFrameCount, targetFrameCount)

        Log.d("FrameAdjustment", "Generated ${indices.size} indices: $indices")


        for (index in indices) {
            // Ensure index is within bounds, though generateUniformIndices should handle this
            if (index in 0..<currentFrameCount) {
                adjustedFrames.add(originalFrames[index])
            } else {
                // This case should ideally not happen if generateUniformIndices is correct
                Log.e("FrameAdjustment", "Generated index $index is out of bounds for original size $currentFrameCount.")
                // Fallback: add the last valid frame or handle error appropriately
                if (originalFrames.isNotEmpty()) {
                    adjustedFrames.add(originalFrames.last())
                }
            }
        }
        Log.d("FrameAdjustment", "Adjusted frame count: ${adjustedFrames.size}")
        return adjustedFrames
    }

    /**
     * Generates a list of indices for uniform sampling.
     * Equivalent to numpy.linspace(0, currentSize - 1, targetSize, dtype=int).
     *
     * @param currentSize The current number of items in the sequence.
     * @param targetSize The desired number of items.
     * @return A list of integer indices.
     */
    private fun generateUniformIndices(currentSize: Int, targetSize: Int): List<Int> {
        if (currentSize == 0) return emptyList()
        if (targetSize == 0) return emptyList()

        val indices = mutableListOf<Int>()
        // If targetSize is 1, always pick the first element (or middle, or last, based on preference)
        if (targetSize == 1) {
            indices.add(0) // Or currentSize / 2, or currentSize - 1
            return indices
        }

        // The step calculation needs to map 'targetSize' points over 'currentSize' available points.
        // The indices should range from 0 to currentSize - 1.
        for (i in 0 until targetSize) {
            val fraction = i.toDouble() / (targetSize - 1) // Normalized position in the target sequence (0.0 to 1.0)
            val mappedIndex = fraction * (currentSize - 1) // Mapped position in the original sequence

            // Convert to Int. Using floor to mimic np.linspace's typical behavior with dtype=int (truncation towards zero)
            // or round for potentially more even distribution if that's preferred.
            // For np.linspace with dtype=int, it's often effectively a floor or truncation.
            indices.add(floor(mappedIndex).toInt().coerceIn(0, currentSize - 1))
        }
        return indices
    }


}