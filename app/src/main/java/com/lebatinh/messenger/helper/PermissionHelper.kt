package com.lebatinh.messenger.helper

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import javax.inject.Inject

class PermissionHelper @Inject constructor(private val context: Context) {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }

    // Kiểm tra tất cả quyền cần thiết đã được cấp hay chưa
    fun arePermissionsGranted(requiredPermissions: Array<String>): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Yêu cầu quyền từ người dùng
    fun requestPermissions(activity: Activity, permissions: Array<String>) {
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissionsToRequest,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    // Xử lý kết quả yêu cầu quyền
    fun handlePermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit,
        onPermissionsRationaleNeeded: () -> Unit
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Tất cả quyền đã được cấp
                onPermissionsGranted()
            } else {
                // Kiểm tra xem quyền đã bị từ chối vĩnh viễn (Don't ask again) chưa
                val deniedPermissions = permissions.filterIndexed { index, _ ->
                    grantResults[index] != PackageManager.PERMISSION_GRANTED
                }

                val shouldShowRationale = deniedPermissions.any { permission ->
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
                }

                if (shouldShowRationale) {
                    onPermissionsRationaleNeeded()
                } else {
                    onPermissionsDenied()
                }
            }
        }
    }
}