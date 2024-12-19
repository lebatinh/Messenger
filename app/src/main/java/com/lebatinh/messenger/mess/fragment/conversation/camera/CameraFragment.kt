package com.lebatinh.messenger.mess.fragment.conversation.camera

import android.animation.ObjectAnimator
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
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

    private var recordingAnimator: ObjectAnimator? = null

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
            // Chụp ảnh
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
                        Log.e("CameraActivity", "Chụp ảnh thất bại: ${exception.message}")
                    }
                })
        }

        binding.btnVideoRecord.setOnClickListener {
            if (!isRecording) {
                startRecording()
                binding.btnVideoRecord.isEnabled = false
                binding.btnVideoRecord.isVisible = false
                binding.btnVideoStop.isVisible = true
                binding.btnVideoStop.isEnabled = true
            }
        }

        binding.btnVideoStop.setOnClickListener {
            if (isRecording) {
                stopRecording()
                binding.btnVideoRecord.isEnabled = true
                binding.btnVideoRecord.isVisible = true
                binding.btnVideoStop.isVisible = false
                binding.btnVideoStop.isEnabled = false
            }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.preview.surfaceProvider
            }

            imageCapture = ImageCapture.Builder().build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startCamera() {
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK)
            .build()

        cameraProvider.unbindAll() // Hủy camera cũ
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture, videoCapture)
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

        isRecording = true
        videoFile = File(requireContext().filesDir, "video_${System.currentTimeMillis()}.mp4")

        val outputOptions = FileOutputOptions.Builder(videoFile!!).build()
        recording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        // Bắt đầu quay video thành công
                        Log.d("CameraFragment", "Recording started")
                        startRecordingAnimation()
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (!event.hasError()) {
                            goToPreviewFragment(videoFile!!.absolutePath)
                        } else {
                            Log.e("CameraFragment", "Error recording video: ${event.error}")
                        }
                        // Dừng recording, reset flag
                        isRecording = false
                        stopRecording()
                    }
                }
            }
        startRecordingAnimation()
    }

    private fun stopRecording() {
        recording?.stop()  // Dừng recording
        recording = null
        resetRecordingUI() // Reset giao diện sau khi dừng quay
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
        recordingAnimator =
            ObjectAnimator.ofFloat(binding.btnVideoStop, View.SCALE_X, 1f, 1.2f, 1f).apply {
                duration = 1000
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
            }
        recordingAnimator?.start()
    }

    private fun resetRecordingUI() {
        TransitionManager.beginDelayedTransition(binding.root)
        binding.btnVideoRecord.visibility = View.VISIBLE
        binding.btnVideoStop.visibility = View.GONE
        recordingAnimator?.cancel()
        recordingAnimator = null
    }
}