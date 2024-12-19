package com.lebatinh.messenger.mess

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.Key_Password.API_KEY
import com.lebatinh.messenger.Key_Password.API_SECRET
import com.lebatinh.messenger.Key_Password.CLOUD_NAME
import com.lebatinh.messenger.R
import com.lebatinh.messenger.animation.CustomBorderAnimation
import com.lebatinh.messenger.databinding.ActivityMessageBinding
import com.lebatinh.messenger.databinding.DrawerHeaderBinding
import com.lebatinh.messenger.helper.FileHelper
import com.lebatinh.messenger.helper.PermissionHelper
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserRepository
import com.lebatinh.messenger.user.UserViewModel
import com.lebatinh.messenger.user.UserViewModelFactory
import kotlinx.coroutines.launch
import java.io.File

class MessageActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMessageBinding

    private lateinit var userViewModel: UserViewModel
    private lateinit var headerView: DrawerHeaderBinding

    private lateinit var permissionHelper: PermissionHelper
    private var email: String? = null
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cấu hình Cloudinary
        val config = mapOf(
            "cloud_name" to CLOUD_NAME,
            "api_key" to API_KEY,
            "api_secret" to API_SECRET
        )

        // Khởi tạo Cloudinary
        MediaManager.init(this, config)

        permissionHelper = PermissionHelper(this)
        // Kiểm tra quyền và yêu cầu quyền nếu chưa được cấp
        if (!permissionHelper.arePermissionsGranted(REQUIRED_PERMISSIONS)) {
            // Yêu cầu quyền nếu chưa được cấp
            permissionHelper.requestPermissions(this, REQUIRED_PERMISSIONS)
        }

        setSupportActionBar(binding.appBarMessage.toolbar)

        val drawerLayout = binding.dwMessage
        val navView = binding.navViewMessage

        val navController = findNavController(R.id.nav_host_fragment_container_messenger)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeMessenger, R.id.waitingMessage, R.id.friendFragment, R.id.settingsFragment
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val repository = UserRepository()
        userViewModel =
            ViewModelProvider(this, UserViewModelFactory(repository))[UserViewModel::class.java]

        headerView = DrawerHeaderBinding.bind(navView.getHeaderView(0))
        CustomBorderAnimation().runBorderAnimation(
            headerView.header,
            this,
            10f,
            20f,
            8f,
            20f,
            3000L
        )

        headerView.imgAvatar.setOnClickListener {
            showAvatarDialog()
        }

        email = intent.getStringExtra("email")
        if (email != null) {
            userViewModel.getUserInfo(email!!)

            userViewModel.returnResult.observe(this) { result ->
                when (result) {
                    ReturnResult.Loading -> {
                        binding.frLoading.visibility = View.VISIBLE
                    }

                    is ReturnResult.Success -> {
                        val user = result.data
                        binding.frLoading.visibility = View.GONE

                        headerView.tvName.text = user.fullName
                        headerView.tvAccount.text = user.email
                        Glide.with(this@MessageActivity)
                            .load(user.avatar)
                            .into(headerView.imgAvatar)

                        userViewModel.resetReturnResult()
                    }

                    is ReturnResult.Error -> {
                        binding.frLoading.visibility = View.GONE
                        Snackbar.make(binding.root, "Có lỗi xảy ra!", Snackbar.LENGTH_SHORT).show()
                        userViewModel.resetReturnResult()
                    }

                    null -> {
                        binding.frLoading.visibility = View.GONE
                    }
                }
            }
        } else {
            Snackbar.make(binding.root, "Có lỗi xảy ra!", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Glide.with(this).onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_container_messenger)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Xử lý kết quả yêu cầu quyền
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
            onPermissionsGranted = {},
            onPermissionsDenied = {
                Snackbar.make(
                    binding.root,
                    "Bạn cần cấp quyền để sử dụng tính năng này. Vui lòng cấp quyền trong cài đặt.",
                    Snackbar.LENGTH_LONG
                ).show()
            },
            onPermissionsRationaleNeeded = {
                Snackbar.make(
                    binding.root,
                    "Ứng dụng cần quyền camera và lưu trữ để chụp ảnh hoặc chọn ảnh.",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("Cấp quyền") {
                    permissionHelper.requestPermissions(this, REQUIRED_PERMISSIONS)
                }.show()
            }
        )
    }

    private fun showAvatarDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Chỉnh sửa ảnh đại diện")
            .setMessage("Chọn một tùy chọn")
            .setPositiveButton("Chọn từ thư viện ảnh") { _, _ ->
                // Mở thư viện ảnh
                openGallery()
            }
            .setNegativeButton("Chụp ảnh") { _, _ ->
                // Mở camera
                openCamera()
            }
            .setNeutralButton("Đóng") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    // Register activity result launchers
    private val getImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { uploadImageToCloudinary(it) }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
            if (isSuccess && uri != null) {
                Log.d("success", "")
                uploadImageToCloudinary(uri!!)
            } else {
                Log.d("fail", "")
            }
        }

    private fun openCamera() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "photo.jpg")

        // Tạo URI từ FileProvider
        val uri = FileProvider.getUriForFile(this, "com.lebatinh.messenger", file)
        Log.d("CameraURI", "Uri: $uri")

        // Cấp quyền tạm thời cho URI
        grantUriPermission(
            packageManager.toString(),
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        // Lưu URI vào biến toàn cục nếu cần dùng lại
        this.uri = uri

        // Chụp ảnh
        takePicture.launch(uri)
    }

    private fun openGallery() {
        getImageFromGallery.launch("image/*")
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        // Sử dụng FileHelper để upload ảnh
        lifecycleScope.launch {
            val imageUrl = FileHelper.uploadImage(this@MessageActivity, uri)
            if (!email.isNullOrEmpty()) {
                imageUrl?.let {
                    Glide.with(this@MessageActivity)
                        .load(imageUrl)
                        .into(headerView.imgAvatar)
                    userViewModel.updateUserInfo(email!!, null, null, null, imageUrl)
                } ?: run {
                    Snackbar.make(
                        binding.root,
                        "Ảnh không hợp lệ.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}