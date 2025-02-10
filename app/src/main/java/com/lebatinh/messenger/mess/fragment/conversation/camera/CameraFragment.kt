package com.lebatinh.messenger.mess.fragment.conversation.camera

import android.Manifest
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.Key_Password.MAX_VIDEO_SIZE
import com.lebatinh.messenger.R
import com.lebatinh.messenger.databinding.FragmentCameraBinding
import java.io.File

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var currentUID: String? = null
    private var receiverUID: String? = null
    private var conversationId: String? = null
    private var isGroup: Boolean? = null

    private val args: CameraFragmentArgs by navArgs()

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var preview: Preview
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null

    private var isCaptureMode = true
    private var isRecording = false
    private var isFrontCamera = false

    private var videoFile: File? = null
    private var recordingAnimator: ScaleAnimation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUID = args.currentUID
        receiverUID = args.receiverUID
        conversationId = args.conversationID
        isGroup = args.isGroup
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val root: View = binding.root

        switchMode(true)
        binding.imgPhotoMode.setOnClickListener {
            switchMode(true)
            updateModeUI(true)
        }

        binding.imgVideoMode.setOnClickListener {
            switchMode(false)
            updateModeUI(false)
        }

        binding.btnSwitchCamera.setOnClickListener {
            isFrontCamera = !isFrontCamera
            startCamera()
        }

        loadLastImage()

        binding.btnImageCapture.setOnClickListener {
            captureImage()
        }

        binding.btnVideoRecord.setOnClickListener {
            if (!isRecording) {
                startRecording()
            }
        }

        binding.btnVideoStop.setOnClickListener {
            if (isRecording) {
                stopRecording()
            }
        }

        return root
    }

    private fun captureImage() {
        val photoFile = File(requireContext().cacheDir, "${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    if (!currentUID.isNullOrEmpty() && isGroup != null) {
                        val action =
                            CameraFragmentDirections.actionCameraFragmentToPreviewFragment(
                                currentUID!!,
                                receiverUID,
                                conversationId,
                                isGroup!!,
                                true,
                                photoFile.absolutePath
                            )
                        findNavController().navigate(action)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraFragment", "Chụp ảnh thất bại: ${exception.message}")
                }
            })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isSupportedVideoEncoder()) {
            Snackbar.make(
                binding.root,
                "Your device may not support video recording",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(
                Quality.SD,
                Quality.HD,
                Quality.FHD,
                Quality.UHD
            ),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                preview = Preview.Builder().build().also {
                    it.surfaceProvider = binding.preview.surfaceProvider
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .setExecutor(ContextCompat.getMainExecutor(requireContext()))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                startCamera()

            } catch (e: Exception) {
                Log.e("CameraFragment", "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startCamera() {
        try {
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
                .build()

            cameraProvider.unbindAll() // Hủy camera cũ
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
        } catch (e: Exception) {
            Log.e("CameraFragment", "Camera binding failed", e)
        }
    }

    private fun switchMode(isCapture: Boolean) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.ctlMode)

        TransitionManager.beginDelayedTransition(binding.ctlMode)

        if (isCapture) {
            // Đưa ctlPhotoMode vào giữa
            constraintSet.clear(R.id.ctlPhotoMode, ConstraintSet.END)
            constraintSet.connect(
                R.id.ctlPhotoMode,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                0
            )
            constraintSet.connect(
                R.id.ctlPhotoMode,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                0
            )

            // Đưa ctlVideoMode sang phải
            constraintSet.clear(
                R.id.ctlVideoMode,
                ConstraintSet.START
            ) // Gỡ ràng buộc với ctlPhotoMode
            constraintSet.connect(
                R.id.ctlVideoMode,
                ConstraintSet.START,
                R.id.ctlPhotoMode,
                ConstraintSet.END,
                16
            )
        } else {
            // Đưa imgVideoMode vào giữa
            constraintSet.clear(R.id.ctlVideoMode, ConstraintSet.START)
            constraintSet.connect(
                R.id.ctlVideoMode,
                ConstraintSet.START,
                ConstraintSet.PARENT_ID,
                ConstraintSet.START,
                0
            )
            constraintSet.connect(
                R.id.ctlVideoMode,
                ConstraintSet.END,
                ConstraintSet.PARENT_ID,
                ConstraintSet.END,
                0
            )

            // Đưa ctlPhotoMode sang trái
            constraintSet.clear(
                R.id.ctlPhotoMode,
                ConstraintSet.END
            ) // Gỡ ràng buộc với ctlVideoMode
            constraintSet.connect(
                R.id.ctlPhotoMode,
                ConstraintSet.END,
                R.id.ctlVideoMode,
                ConstraintSet.START,
                16
            )
        }

        constraintSet.applyTo(binding.ctlMode)
        isCaptureMode = isCapture
    }

    private fun loadLastImage() {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireContext().contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val imagePath = it.getString(0)
                Glide.with(this).load(imagePath).into(binding.imgPreview)
            }
        }
    }

    private fun startRecording() {
        if (isRecording || recording != null) {
            Log.w("CameraFragment", "Recording is already in progress, ignoring this call.")
            return
        }

        try {
            isRecording = true
            videoFile = File(requireContext().filesDir, "video_${System.currentTimeMillis()}.mp4")

            val outputOptions =
                FileOutputOptions.Builder(videoFile!!).setFileSizeLimit(MAX_VIDEO_SIZE.toLong())
                    .build()

            recording = videoCapture.output
                .prepareRecording(requireContext(), outputOptions)
                .apply {
                    if (PermissionChecker.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.RECORD_AUDIO
                        ) == PermissionChecker.PERMISSION_GRANTED
                    ) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            // Bắt đầu quay video thành công
                            Log.d("CameraFragment", "Recording started")
                            updateRecordingUI(true)
                            startRecordingAnimation()
                        }

                        is VideoRecordEvent.Finalize -> {
                            if (!event.hasError()) {
                                videoFile?.let { file ->
                                    if (file.exists() && file.length() > 0) {
                                        goToPreviewFragment(file.absolutePath)
                                    } else {
                                        Log.e(
                                            "CameraFragment",
                                            "Video file is empty or does not exist"
                                        )
                                        Snackbar.make(
                                            binding.root,
                                            "Failed to save video",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Log.e("CameraFragment", "Error recording video: ${event.cause}")
                                videoFile?.delete()
                            }
                            // Dừng recording, reset flag
                            isRecording = false
                            stopRecording()
                            updateRecordingUI(false)
                            stopRecordingAnimation()
                        }
                    }
                }

        } catch (e: Exception) {
            Log.e("CameraFragment", "Failed to start recording", e)
            isRecording = false
            recording = null
            updateRecordingUI(false)
            videoFile?.delete()
            Snackbar.make(
                requireContext(),
                binding.root,
                "Failed to start recording: ${e.message}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun stopRecording() {
        try {
            recording?.stop()
            recording = null
            isRecording = false
            stopRecordingAnimation()
            updateRecordingUI(false)
        } catch (e: Exception) {
            Log.e("CameraFragment", "Failed to stop recording", e)
            // Trong trường hợp lỗi, vẫn reset UI
            isRecording = false
            recording = null
            updateRecordingUI(false)
            stopRecordingAnimation()
            videoFile?.delete() // Xóa file nếu có lỗi khi dừng
        }
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        binding.apply {
            btnVideoRecord.isEnabled = !isRecording
            btnVideoRecord.isVisible = !isRecording
            btnVideoStop.isVisible = isRecording
            btnVideoStop.isEnabled = isRecording
        }
    }

    private fun isSupportedVideoEncoder(): Boolean {
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val encoderInfo = codecList.codecInfos.firstOrNull {
                it.isEncoder && it.supportedTypes.contains(MediaFormat.MIMETYPE_VIDEO_AVC)
            }
            encoderInfo != null
        } catch (e: Exception) {
            Log.e("CameraFragment", "Error checking video encoder support", e)
            false
        }
    }

    private fun goToPreviewFragment(videoPath: String) {
        if (!currentUID.isNullOrEmpty() && isGroup != null) {
            val action = CameraFragmentDirections.actionCameraFragmentToPreviewFragment(
                currentUID!!,
                receiverUID,
                conversationId,
                isGroup!!,
                false,
                videoPath
            )
            findNavController().navigate(action)
        }
    }

    private fun updateModeUI(isPhotoMode: Boolean) {
        TransitionManager.beginDelayedTransition(binding.ctlMode)

        if (isPhotoMode) {
            binding.btnImageCapture.visibility = View.VISIBLE
            binding.btnVideoRecord.visibility = View.GONE
            binding.btnVideoStop.visibility = View.GONE
        } else {
            binding.btnImageCapture.visibility = View.GONE
            binding.btnVideoRecord.visibility = View.VISIBLE
            binding.btnVideoStop.visibility = View.GONE
        }
    }

    private fun startRecordingAnimation() {
        recordingAnimator = ScaleAnimation(
            1.0f, 1.1f,
            1.0f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }

        binding.btnVideoStop.startAnimation(recordingAnimator)
    }

    private fun stopRecordingAnimation() {
        recordingAnimator?.cancel()
        binding.btnVideoStop.clearAnimation()
    }
}