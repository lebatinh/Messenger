package com.lebatinh.messenger.account

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lebatinh.messenger.databinding.ActivityAccountBinding
import com.lebatinh.messenger.helper.PermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding

    @Inject
    lateinit var permissionHelper: PermissionHelper

    private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.account) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupPermissionView()
        checkAndRequestPermissions()
    }

    private fun setupPermissionView() {
        binding.layoutPermissions.apply {
            btnGrantPermission.setOnClickListener {
                checkAndRequestPermissions()
            }
            root.visibility = View.GONE
        }
    }

    private fun checkAndRequestPermissions() {
        if (!permissionHelper.arePermissionsGranted(REQUIRED_PERMISSIONS)) {
            showBlockingView()
            permissionHelper.requestPermissions(this, REQUIRED_PERMISSIONS)
        } else {
            hideBlockingView()
        }
    }

    private fun showBlockingView() {
        binding.layoutPermissions.root.visibility = View.VISIBLE
        binding.navHostFragmentContainerAccount.visibility = View.GONE
    }

    private fun hideBlockingView() {
        binding.layoutPermissions.root.visibility = View.GONE
        binding.navHostFragmentContainerAccount.visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionHelper.handlePermissionsResult(
            this,
            requestCode,
            permissions,
            grantResults,
            onPermissionsGranted = {
                hideBlockingView()
            },
            onPermissionsDenied = {
                showSettingsDialog()
            },
            onPermissionsRationaleNeeded = {
                showPermissionRationaleDialog(permissions)
            }
        )
    }

    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Cần cấp quyền")
            .setMessage("Một số quyền đã bị từ chối vĩnh viễn. Vui lòng cấp quyền trong cài đặt để sử dụng đầy đủ tính năng của ứng dụng.")
            .setPositiveButton("Đi tới Cài đặt") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Hủy") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionRationaleDialog(permissions: Array<String>) {
        val message = buildPermissionRationaleMessage(permissions)

        MaterialAlertDialogBuilder(this)
            .setTitle("Cần cấp quyền")
            .setMessage(message)
            .setPositiveButton("Cấp quyền") { _, _ ->
                permissionHelper.requestPermissions(this, permissions)
            }
            .setNegativeButton("Thoát") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun buildPermissionRationaleMessage(permissions: Array<String>): String {
        val reasons = mutableListOf<String>()

        permissions.forEach { permission ->
            when (permission) {
                Manifest.permission.CAMERA ->
                    reasons.add("- Camera: để chụp ảnh và quay video")

                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO ->
                    reasons.add("- Bộ nhớ: để truy cập ảnh và video")

                Manifest.permission.WRITE_EXTERNAL_STORAGE ->
                    reasons.add("- Bộ nhớ: để lưu ảnh và video")

                Manifest.permission.RECORD_AUDIO ->
                    reasons.add("- Microphone: để ghi âm")

                Manifest.permission.POST_NOTIFICATIONS ->
                    reasons.add("- Thông báo: để nhận các thông báo quan trọng")
            }
        }

        return "Ứng dụng cần các quyền sau để hoạt động:\n\n${
            reasons.distinct().joinToString("\n")
        }"
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }
}