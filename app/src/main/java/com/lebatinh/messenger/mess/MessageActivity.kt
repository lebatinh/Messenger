package com.lebatinh.messenger.mess

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.lebatinh.messenger.R
import com.lebatinh.messenger.animation.CustomBorderAnimation
import com.lebatinh.messenger.databinding.ActivityMessageBinding
import com.lebatinh.messenger.databinding.DrawerHeaderBinding
import com.lebatinh.messenger.helper.FileHelper
import com.lebatinh.messenger.other.ReturnResult
import com.lebatinh.messenger.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class MessageActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMessageBinding

    private val userViewModel: UserViewModel by viewModels()
    private val cloudinaryViewModel: CloudinaryViewModel by viewModels()
    private lateinit var headerView: DrawerHeaderBinding

    private var id: String? = null
    private var email: String? = null

    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMessage.toolbar)

        val drawerLayout = binding.dwMessage
        val navView = binding.navViewMessage

        val navController = findNavController(R.id.nav_host_fragment_container_messenger)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeMessenger,
                R.id.waitingMessage,
                R.id.communityFragment,
                R.id.friendInvitationFragment,
                R.id.settingsFragment
            ), drawerLayout
        )

        navController.addOnDestinationChangedListener { _, des, _ ->
            when (des.id) {
                R.id.conversationFragment -> {
                    // Hiển thị ảnh và cho phép kéo giãn
                    binding.appBarMessage.appBarLayout.setExpanded(false, false)
                    binding.appBarMessage.collapsingToolbarLayout.apply {
                        isTitleEnabled = true
                        binding.appBarMessage.appBarLayout.setLiftable(true)
                    }
                    binding.appBarMessage.cvAvatar.apply {
                        visibility = View.VISIBLE
                        animate().alpha(1F).setDuration(300).start()
                    }
                }

                else -> {
                    // Ẩn ảnh và khóa kéo giãn
                    binding.appBarMessage.appBarLayout.setExpanded(false, false)
                    binding.appBarMessage.collapsingToolbarLayout.apply {
                        isTitleEnabled = false
                        title = ""
                        binding.appBarMessage.appBarLayout.setLiftable(false)
                    }
                    binding.appBarMessage.cvAvatar.apply {
                        animate().alpha(0F).setDuration(300).withEndAction {
                            visibility = View.GONE
                        }.start()
                    }
                }
            }
        }

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

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

        // Quan sát URL ảnh sau khi upload
        cloudinaryViewModel.imageUrl.observe(this) { url ->
            url?.let {
                Glide.with(this).load(it.first()).into(headerView.imgAvatar)
                userViewModel.updateUserInfo(email!!, null, null, null, it.first())
            }
        }

        email = intent.getStringExtra("email")
        id = intent.getStringExtra("userUID")
        id.let {
            userViewModel.getUserByUID(id!!)
        }
    }

    override fun onStart() {
        super.onStart()

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
    }

    override fun onDestroy() {
        super.onDestroy()
        Glide.with(this).onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_container_messenger)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
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

    private fun openGallery() {
        getImageFromGallery.launch("image/*")
    }

    private val getImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { FileHelper.startCrop(this, it, cropImageLauncher) }
        }

    private val cropImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val croppedUri = FileHelper.handleCropResult(result.data)
                croppedUri?.let { cloudinaryViewModel.uploadImage(listOf(it), this) }
            } else {
                Snackbar.make(binding.root, "Cắt ảnh thất bại.", Snackbar.LENGTH_LONG).show()
            }
        }

    private fun openCamera() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "photo.jpg")

        // Tạo URI từ FileProvider
        val uri = FileProvider.getUriForFile(this, "com.lebatinh.messenger", file)

        // Cấp quyền tạm thời cho URI
        grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        // Lưu URI vào biến toàn cục nếu cần dùng lại
        this.uri = uri

        // Chụp ảnh
        takePicture.launch(uri)
    }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
            if (isSuccess && uri != null) {
                FileHelper.startCrop(this, uri!!, cropImageLauncher)
            } else {
                Snackbar.make(binding.root, "Chụp ảnh thất bại.", Snackbar.LENGTH_LONG).show()
            }
        }
}